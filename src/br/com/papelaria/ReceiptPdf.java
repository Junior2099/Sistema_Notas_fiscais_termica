package br.com.papelaria;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** PDF de comprovante não fiscal, sem bibliotecas adicionais ou impressora virtual. */
final class ReceiptPdf {
    static final double MM = 72.0 / 25.4;
    static final String COMPANY = "Imprimir", CNPJ = "40.203.293/0001-29";
    // KP-1025: 58 mm paper, 48 mm effective print width (5 mm on each side).
    static final double WIDTH = 58 * MM, HEIGHT = 210 * MM, MARGIN = 5 * MM, FONT = 8, LEADING = 11, HEADER = 62;
    static final int COLUMNS = (int)((WIDTH - 2 * MARGIN) / (FONT * 0.6));
    static final int LINES_PER_PAGE = (int)((HEIGHT - 2 * MARGIN - HEADER - FONT - 20) / LEADING);
    static final Charset WIN_ANSI = Charset.forName("windows-1252");

    static String number(double n) { return String.format(Locale.ROOT,"%.3f",n); }
    static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    static String hex(String s) { return HexFormat.of().withUpperCase().formatHex(s.getBytes(WIN_ANSI)); }
    static List<String> wrap(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String original : raw) {
            // Latin-text encoding preserves Portuguese accents. Unsupported glyphs become '?'.
            String text = new String(original.replace('\t',' ').getBytes(WIN_ANSI),WIN_ANSI);
            if(text.matches("-+"))text="-".repeat(COLUMNS);
            for (String line : text.split("\\R",-1)) {
                if (line.isEmpty()) { out.add(""); continue; }
                while (line.length()>COLUMNS) {
                    int end=line.lastIndexOf(' ',COLUMNS);
                    if(end<COLUMNS/2)end=COLUMNS;
                    out.add(line.substring(0,end));line=line.substring(end).stripLeading();
                }
                out.add(line);
            }
        }
        return out;
    }
    static byte[] content(List<String> lines,double height,int page,int pages) {
        StringBuilder b = new StringBuilder("0 g\n");
        double cx=WIDTH/2, top=height-MARGIN;
        // Printer symbol as vector paths, so it stays sharp on thermal printers.
        b.append("q 0 G 0.8 w ").append(number(cx-9)).append(' ').append(number(top-17)).append(" 18 10 re S ")
         .append(number(cx-6)).append(' ').append(number(top-7)).append(" 12 6 re S ")
         .append(number(cx-6)).append(' ').append(number(top-22)).append(" 12 10 re S ")
         .append(number(cx-3)).append(' ').append(number(top-16)).append(" m ").append(number(cx+3)).append(' ').append(number(top-16)).append(" l S Q\n");
        centered(b,COMPANY,14,top-39,true);
        centered(b,"CNPJ: "+CNPJ,7.5,top-52,true);
        double y = height - MARGIN - HEADER - FONT;
        for(int n=0;n<lines.size();n++) {
            String line=lines.get(n);boolean title=line.equals("COMPROVANTE DE ATENDIMENTO")||line.equals("DOCUMENTO NAO FISCAL");
            double x=title?(WIDTH-line.length()*FONT*0.6)/2:MARGIN;
            b.append("BT /F2 ").append(number(FONT)).append(" Tf 1 0 0 1 ")
             .append(number(x)).append(' ').append(number(y)).append(" Tm <").append(hex(line)).append("> Tj ET\n");
            y-=LEADING;
        }
        b.append("BT /F2 7 Tf 1 0 0 1 ").append(number(MARGIN)).append(' ').append(number(MARGIN))
         .append(" Tm <").append(hex("Página "+page+" de "+pages+" - não fiscal")).append("> Tj ET\n");
        return ascii(b.toString());
    }
    static void centered(StringBuilder b,String text,double font,double y,boolean bold){double x=(WIDTH-text.length()*font*0.6)/2;b.append("BT /").append(bold?"F2":"F1").append(' ').append(number(font)).append(" Tf 1 0 0 1 ").append(number(x)).append(' ').append(number(y)).append(" Tm <").append(hex(text)).append("> Tj ET\n");}
    static byte[] generate(App.Sale sale) throws IOException {
        List<String> raw=App.receipt(sale);
        List<String> lines=wrap(raw.subList(2,raw.size()));int count=Math.max(1,(lines.size()+LINES_PER_PAGE-1)/LINES_PER_PAGE);
        List<byte[]> objects=new ArrayList<>();
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R /PageLayout /SinglePage /ViewerPreferences << /PrintScaling /None >> >>"));
        StringBuilder kids=new StringBuilder();for(int p=0;p<count;p++)kids.append(5+p*2).append(" 0 R ");
        objects.add(ascii("<< /Type /Pages /Count "+count+" /Kids [ "+kids+"] >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>"));
        for(int p=0;p<count;p++) {
            List<String> page=lines.subList(p*LINES_PER_PAGE,Math.min(lines.size(),(p+1)*LINES_PER_PAGE));
            double height=HEIGHT;
            int streamId=6+p*2;
            objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "+number(WIDTH)+" "+number(height)+"] /Rotate 0 /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "+streamId+" 0 R >>"));
            byte[] content=content(page,height,p+1,count);ByteArrayOutputStream stream=new ByteArrayOutputStream();
            stream.write(ascii("<< /Length "+content.length+" >>\nstream\n"));stream.write(content);stream.write(ascii("endstream"));objects.add(stream.toByteArray());
        }
        ByteArrayOutputStream output=new ByteArrayOutputStream();output.write(ascii("%PDF-1.4\n"));output.write(new byte[]{'%',(byte)0xE2,(byte)0xE3,(byte)0xCF,(byte)0xD3,'\n'});
        List<Integer> offsets=new ArrayList<>();offsets.add(0);
        for(int n=0;n<objects.size();n++) {offsets.add(output.size());output.write(ascii((n+1)+" 0 obj\n"));output.write(objects.get(n));output.write(ascii("\nendobj\n"));}
        int xref=output.size();output.write(ascii("xref\n0 "+offsets.size()+"\n0000000000 65535 f \n"));
        for(int n=1;n<offsets.size();n++)output.write(ascii(String.format(Locale.ROOT,"%010d 00000 n \n",offsets.get(n))));
        output.write(ascii("trailer\n<< /Size "+offsets.size()+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return output.toByteArray();
    }
    static void save(App.Sale sale,Path destination) throws IOException {
        Path target=destination.toAbsolutePath();Path parent=target.getParent();
        Path temp=Files.createTempFile(parent,"comprovante-",".tmp");
        try {
            Files.write(temp,generate(sale));
            try {Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
}
