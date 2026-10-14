package br.com.papelaria;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Verifica a interface com dados temporários; não acessa os dados do usuário. */
public final class UiCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void invalidate(Container c){for(Component child:c.getComponents())if(child instanceof Container container)invalidate(container);c.invalidate();}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layout(container);}
    static void buttons(Component c){
        if(!c.isVisible())return;
        if(c instanceof Ui.Button b&&!b.getText().isEmpty()){check(b.getWidth()>30&&b.getHeight()>20,"Botão sem espaço: "+b.getText());
            check(b.getY()+b.getHeight()<=b.getParent().getHeight()+1,"Botão cortado: "+b.getText());
        }
        if(c instanceof Container container)for(Component child:container.getComponents())buttons(child);
    }
    static JTextField findField(Component c,String hint){
        if(c instanceof Ui.HintField f&&f.hint.equals(hint))return f;
        if(c instanceof Container container)for(Component child:container.getComponents()){JTextField f=findField(child,hint);if(f!=null)return f;}
        return null;
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("papelaria-interface-");
        try{
            App.Store store=new App.Store(temp);
            App.Item printing=new App.Item(App.id(),"Serviço","Impressão colorida A4","IMP002",new BigDecimal("2.50"));
            store.item(printing);store.item(new App.Item(App.id(),"Serviço","Cópia preto e branco A4","IMP001",new BigDecimal("0.50")));
            App.Item pen=new App.Item(App.id(),"Produto","Caneta azul","7891234567890",new BigDecimal("3.00"));store.item(pen);
            store.item(new App.Item(App.id(),"Produto","Caderno universitário","7891111111111",new BigDecimal("18.00")));
            store.customer(new App.Customer(App.id(),"Cliente demonstrativo","",""));
            List<App.Line> lines=List.of(new App.Line("Serviço",printing.name(),printing.price(),new BigDecimal("12")),new App.Line("Produto",pen.name(),pen.price(),new BigDecimal("2")));
            App.Sale s=new App.Sale(App.id(),"2026-10-11T21:05:00","Cliente demonstrativo","",lines,"Papel A4, frente e verso","","");store.sale(s);
            store.sale(new App.Sale(App.id(),"2026-10-11T20:00:00","Cliente não identificado","",List.of(lines.get(1)),"","",""));
            SwingUtilities.invokeAndWait(()->{
                try{
                    Ui.install();App app=new App(store,true);app.cart.addAll(lines);app.refreshCart();app.notes.setText("Impressão em papel A4, frente e verso.");
                    check(app.totals.getText().equals("R$ 36,00"),"Resumo do atendimento");
                    JTextField catalog=findField(app.shell,"Buscar nome, tipo ou código");catalog.setText("impressao");
                    check(app.itemTable.getRowCount()==1,"Busca sem acentos");app.itemTable.setRowSelectionInterval(0,0);
                    check(app.items.get(app.selected(app.itemTable)).id().equals(printing.id()),"Edição de cadastro filtrado mantém ID correto");catalog.setText("");
                    JTextField history=findField(app.shell,"Buscar cliente, data ou valor");history.setText("demonstrativo");app.selectSale(s.id());
                    check(app.selectedSale().id().equals(s.id()),"Seleção de atendimento filtrado");
                    app.refresh();check(app.selectedSale().id().equals(s.id()),"Seleção preservada após atualizar");
                    check(app.pdfActions.stream().noneMatch(AbstractButton::isEnabled),"PDF sem arquivo desabilitado");
                    check(app.serviceActions.stream().allMatch(AbstractButton::isEnabled),"Serviço pode anexar PDF");history.setText("");
                    app.selectSale(app.sales.stream().filter(sale->sale.lines().stream().allMatch(l->l.kind().equals("Produto"))).findFirst().orElseThrow().id());
                    check(app.serviceActions.stream().noneMatch(AbstractButton::isEnabled),"Venda de produto não anexa NFS-e");app.selectSale(s.id());
                    Path output=args.length==0?null:Path.of(args[0]);if(output!=null)Files.createDirectories(output);
                    for(int[] size:new int[][]{{1320,850},{1120,740},{1080,720}}){
                        app.shell.setSize(size[0],size[1]);
                        for(int n=0;n<5;n++){
                            app.showPage(n);invalidate(app.shell);layout(app.shell);buttons(app.shell);
                            BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();Ui.aa(g);app.shell.paint(g);g.dispose();
                            if(output!=null)ImageIO.write(image,"png",output.resolve("tela-"+n+"-"+size[0]+".png").toFile());
                        }
                    }
                    app.historySearch.setText("nenhum resultado");app.saveCart();
                    check(app.historySearch.getText().isEmpty()&&app.selectedSale().lines().size()==2,"Atendimento salvo fica visível mesmo após busca anterior");
                    check(app.cart.isEmpty()&&app.totals.getText().equals("R$ 0,00")&&app.activePage==3,"Salvar limpa o atendimento e abre o histórico");
                    System.out.println("OK: telas em três resoluções, busca, seleção filtrada, total, salvamento e ações de documentos.");
                }catch(Exception e){throw new RuntimeException(e);}
            });
        }finally{try(var stream=Files.walk(temp)){for(Path p:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
}
