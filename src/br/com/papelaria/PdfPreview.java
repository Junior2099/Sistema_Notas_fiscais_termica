package br.com.papelaria;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

/** Cria exemplos temporários para conferir o PDF sem acessar dados reais. */
public final class PdfPreview {
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Informe uma pasta para os exemplos.");
        Path folder=Path.of(args[0]);Files.createDirectories(folder);
        App.Sale sale=new App.Sale("00000000-0000-0000-0000-000000000001","2026-10-11T21:30:00","Cliente demonstrativo - São Paulo","",
            List.of(new App.Line("Serviço","Impressão colorida A4",new BigDecimal("2.50"),new BigDecimal("12")),
            new App.Line("Produto","Caneta azul",new BigDecimal("3.00"),new BigDecimal("2"))),"","","123 (exemplo)");
        ReceiptPdf.save(sale,folder.resolve("comprovante-curto.pdf"));
        App.Sale one=new App.Sale(sale.id(),sale.date(),"Cliente demonstrativo","",List.of(sale.lines().get(0)),"","","");
        ReceiptPdf.save(one,folder.resolve("comprovante-um-servico.pdf"));
        List<App.Line> lines=new ArrayList<>();
        for(int n=1;n<=90;n++)lines.add(new App.Line("Serviço","Impressão número "+n+" - descrição extensa (frente e verso) com acentos: ação, cópia, São Paulo e referência \\ teste",new BigDecimal("1.25"),new BigDecimal("2")));
        App.Sale longSale=new App.Sale(sale.id(),sale.date(),sale.customer(),"",List.copyOf(lines),"","","EXEMPLO");
        ReceiptPdf.save(longSale,folder.resolve("comprovante-longo.pdf"));
        System.out.println("Exemplos de comprovantes gerados para revisão.");
    }
}
