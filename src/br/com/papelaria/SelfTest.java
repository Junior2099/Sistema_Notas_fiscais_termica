package br.com.papelaria;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

/** Verifica regras e persistência sem emitir notas ou enviar impressão. */
public final class SelfTest {
    static void check(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("papelaria-teste-");
        try {
            App.Store store = new App.Store(temp);
            check(App.decimal("1.234,56",false).equals(new BigDecimal("1234.56")),"decimal brasileiro");
            check(App.decimal("2.50",false).equals(new BigDecimal("2.50")),"decimal com ponto");
            for (String invalid : List.of("-1","NaN","1,123")) { boolean rejected = false; try { App.decimal(invalid,false); } catch (IllegalArgumentException e) { rejected = true; } check(rejected,"valor inválido " + invalid); }
            boolean rejected = false; try { App.decimal("0",true); } catch (IllegalArgumentException e) { rejected = true; } check(rejected,"quantidade zero");
            App.Item service = new App.Item(App.id(),"Serviço","Impressão colorida A4","S001",new BigDecimal("1.50"));
            App.Item product = new App.Item(App.id(),"Produto","Caderno","7890000000000",new BigDecimal("12.00"));
            store.item(service); store.item(product); check(store.items().size()==2,"cadastros");
            App.Customer customer = new App.Customer(App.id(),"Cliente de teste — São Paulo","","teste"); store.customer(customer);
            check(store.customers().get(0).name().equals(customer.name()),"acentos e cliente");
            List<App.Line> lines = List.of(new App.Line(service.kind(),service.name(),service.price(),new BigDecimal("10")),
                new App.Line(product.kind(),product.name(),product.price(),new BigDecimal("2")));
            App.Sale sale = new App.Sale(App.id(),"2026-10-11T20:46:00",customer.name(),"",lines,"Papel A4, impressão frente e verso","","");
            check(sale.total().equals(new BigDecimal("39.00")),"total completo");
            check(App.sum(lines,"Serviço").equals(new BigDecimal("15.00")),"subtotal serviços");
            check(!App.description(sale).contains("Caderno"),"produtos fora da NFS-e");
            store.sale(sale); store.item(new App.Item(service.id(),service.kind(),"Nome editado",service.code(),new BigDecimal("9.00")));
            App.Sale loaded = store.sales().get(0); check(loaded.total().equals(sale.total()),"histórico conserva preço");
            check(loaded.lines().get(0).name().equals("Impressão colorida A4"),"histórico conserva descrição");
            check(App.description(loaded).contains("frente e verso"),"observações persistem");
            Path pdf = temp.resolve("teste.pdf"); Files.writeString(pdf,"%PDF-1.4\nfixture para armazenamento");
            String stored = store.importPdf(sale.id(),pdf);
            App.Sale attached = new App.Sale(sale.id(),sale.date(),sale.customer(),sale.document(),sale.lines(),sale.notes(),stored,"123"); store.sale(attached);
            check(Arrays.equals(Files.readAllBytes(pdf),Files.readAllBytes(store.pdf(store.sales().get(0)))),"PDF preservado");
            App.Sale windowsReference=new App.Sale(attached.id(),attached.date(),attached.customer(),attached.document(),attached.lines(),attached.notes(),stored.replace('/','\\'),attached.reference());
            check(Files.isRegularFile(store.pdf(windowsReference)),"Referência PDF compatível entre Windows e Linux");
            check(Files.exists(temp.resolve("atendimentos").resolve(sale.id()+".bak")),"backup por atualização");
            Path fake = temp.resolve("falso.pdf"); Files.writeString(fake,"não é PDF");
            rejected = false; try { store.importPdf(sale.id(),fake); } catch(java.io.IOException e) { rejected = true; } check(rejected,"assinatura PDF");
            PageFormat f = new PageFormat(); Paper p = new Paper(); p.setSize(165,595); p.setImageableArea(9,9,147,577); f.setPaper(p);
            BufferedImage img = new BufferedImage(250,200,BufferedImage.TYPE_INT_RGB);
            var graphics = img.createGraphics();
            try {
                App.ThermalReceipt print = new App.ThermalReceipt(App.receipt(attached));
                check(print.print(graphics,f,0)==Printable.PAGE_EXISTS,"primeira página térmica");
                check(print.print(graphics,f,1)==Printable.NO_SUCH_PAGE,"comprovante curto em uma página");
                check(print.print(graphics,f,100)==Printable.NO_SUCH_PAGE,"fim da impressão");
                var fm = graphics.getFontMetrics();
                List<String> wrapped = App.wrap(List.of("Descrição muito longa: "+"impressão ".repeat(50)),fm,100);
                check(wrapped.stream().allMatch(s -> fm.stringWidth(s)<=100),"texto dentro da largura");
            } finally { graphics.dispose(); }
            check(App.receipt(attached).contains("DOCUMENTO NAO FISCAL"),"identificação não fiscal");
            Path receipt=temp.resolve("Comprovante não fiscal.pdf");ReceiptPdf.save(attached,receipt);
            check(Files.size(receipt)>500,"PDF térmico salvo sem impressora");
            check(store.sales().get(0).pdf().equals(stored),"Exportar comprovante não substitui a NFS-e anexada");
            System.out.println("OK: valores, cadastros, histórico, separação fiscal, PDF e paginação térmica.");
        } finally {
            try (var stream = Files.walk(temp)) { for (Path p : stream.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
}
