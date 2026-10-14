package br.com.papelaria;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.*;
import java.io.*;
import java.math.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Protótipo local: a emissão fiscal é feita pelo usuário no portal oficial. */
public final class App {
    static final String PORTAL = "https://www.nfse.gov.br/EmissorNacional";
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final String[] KINDS = {"Serviço", "Produto"};
    record Item(String id, String kind, String name, String code, BigDecimal price) {
        public String toString() { return name + " — " + money(price); }
    }
    record Customer(String id, String name, String document, String contact) {
        public String toString() { return name; }
    }
    record Line(String kind, String name, BigDecimal price, BigDecimal qty) {
        BigDecimal total() { return price.multiply(qty).setScale(2, RoundingMode.HALF_UP); }
    }
    record Sale(String id, String date, String customer, String document, List<Line> lines,
                String notes, String pdf, String reference) {
        BigDecimal total() { return sum(lines, null); }
    }
    static BigDecimal sum(List<Line> lines, String kind) {
        return lines.stream().filter(l -> kind == null || kind.equals(l.kind))
            .map(Line::total).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
    static BigDecimal decimal(String s, boolean quantity) {
        s = s.strip().replace("R$", "").replace(" ", "");
        if (s.contains(",")) s = s.replace(".", "").replace(',', '.');
        if (!s.matches("[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException("Informe um número, como 2,50.");
        BigDecimal n = new BigDecimal(s);
        if (n.signum() < 0 || (quantity && n.signum() == 0))
            throw new IllegalArgumentException("A quantidade precisa ser maior que zero.");
        if (n.scale() > (quantity ? 3 : 2))
            throw new IllegalArgumentException(quantity ? "Use até 3 casas decimais na quantidade." : "Use até 2 casas decimais no valor.");
        if (n.compareTo(new BigDecimal("99999999")) > 0) throw new IllegalArgumentException("Valor muito alto.");
        return quantity ? n : n.setScale(2);
    }
    static String money(BigDecimal n) { return "R$ " + n.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','); }
    static String id() { return UUID.randomUUID().toString(); }
    static String description(Sale s) {
        StringBuilder b = new StringBuilder();
        s.lines.stream().filter(l -> l.kind.equals("Serviço")).forEach(l ->
            b.append(l.name).append("; quantidade: ").append(l.qty.toPlainString().replace('.', ','))
             .append("; valor unitário: ").append(money(l.price)).append("; total: ").append(money(l.total())).append('\n'));
        if (!s.notes.isBlank()) b.append("Observações do serviço: ").append(s.notes).append('\n');
        if (sum(s.lines, "Serviço").signum() > 0) b.append("Total de serviços: ").append(money(sum(s.lines, "Serviço")));
        return b.toString().strip();
    }

    static final class Store {
        final Path root;
        Store(Path root) throws IOException { this.root = root; Files.createDirectories(root); }
        Properties read(Path file) throws IOException {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(r); }
            return p;
        }
        List<Properties> readAll(String dir) throws IOException {
            Path d = root.resolve(dir); Files.createDirectories(d);
            List<Properties> out = new ArrayList<>();
            try (var stream = Files.list(d)) {
                for (Path f : stream.filter(p -> p.toString().endsWith(".properties")).sorted().toList()) out.add(read(f));
            }
            return out;
        }
        void write(String dir, String id, Properties p) throws IOException {
            if (!id.matches("[0-9a-f-]{36}")) throw new IOException("Identificador inválido.");
            Path d = root.resolve(dir); Files.createDirectories(d);
            Path target = d.resolve(id + ".properties"), temp = Files.createTempFile(d, "registro-", ".tmp");
            try {
                try (Writer w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { p.store(w, "Papelaria Java v0.1"); }
                if (Files.exists(target)) Files.copy(target, d.resolve(id + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
        }
        List<Item> items() throws IOException {
            List<Item> out = new ArrayList<>();
            for (Properties p : readAll("itens")) out.add(new Item(p.getProperty("id"), p.getProperty("tipo"),
                p.getProperty("nome"), p.getProperty("codigo", ""), new BigDecimal(p.getProperty("preco"))));
            return out.stream().sorted(Comparator.comparing(Item::name)).toList();
        }
        void item(Item i) throws IOException {
            Properties p = new Properties(); p.setProperty("id", i.id); p.setProperty("tipo", i.kind);
            p.setProperty("nome", i.name); p.setProperty("codigo", i.code); p.setProperty("preco", i.price.toPlainString());
            write("itens", i.id, p);
        }
        List<Customer> customers() throws IOException {
            List<Customer> out = new ArrayList<>();
            for (Properties p : readAll("clientes")) out.add(new Customer(p.getProperty("id"), p.getProperty("nome"),
                p.getProperty("documento", ""), p.getProperty("contato", "")));
            return out.stream().sorted(Comparator.comparing(Customer::name)).toList();
        }
        void customer(Customer c) throws IOException {
            Properties p = new Properties(); p.setProperty("id", c.id); p.setProperty("nome", c.name);
            p.setProperty("documento", c.document); p.setProperty("contato", c.contact); write("clientes", c.id, p);
        }
        void sale(Sale s) throws IOException {
            Properties p = new Properties(); p.setProperty("id", s.id); p.setProperty("data", s.date);
            p.setProperty("cliente", s.customer); p.setProperty("documento", s.document);
            p.setProperty("observacoes", s.notes); p.setProperty("pdf", s.pdf); p.setProperty("referencia", s.reference);
            p.setProperty("quantidade_linhas", "" + s.lines.size());
            for (int n = 0; n < s.lines.size(); n++) {
                Line l = s.lines.get(n); String prefix = "linha." + n + ".";
                p.setProperty(prefix + "tipo", l.kind); p.setProperty(prefix + "nome", l.name);
                p.setProperty(prefix + "preco", l.price.toPlainString()); p.setProperty(prefix + "quantidade", l.qty.toPlainString());
            }
            write("atendimentos", s.id, p);
        }
        List<Sale> sales() throws IOException {
            List<Sale> out = new ArrayList<>();
            for (Properties p : readAll("atendimentos")) {
                List<Line> lines = new ArrayList<>();
                int count = Integer.parseInt(p.getProperty("quantidade_linhas"));
                for (int n = 0; n < count; n++) {
                    String prefix = "linha." + n + ".";
                    lines.add(new Line(p.getProperty(prefix + "tipo"), p.getProperty(prefix + "nome"),
                        new BigDecimal(p.getProperty(prefix + "preco")), new BigDecimal(p.getProperty(prefix + "quantidade"))));
                }
                out.add(new Sale(p.getProperty("id"), p.getProperty("data"), p.getProperty("cliente"),
                    p.getProperty("documento", ""), List.copyOf(lines), p.getProperty("observacoes", ""),
                    p.getProperty("pdf", ""), p.getProperty("referencia", "")));
            }
            return out.stream().sorted(Comparator.comparing(Sale::date).reversed()).toList();
        }
        Path pdf(Sale s) throws IOException {
            if (s.pdf.isBlank()) throw new IOException("Este atendimento ainda não tem PDF anexado.");
            Path p = root.resolve(s.pdf.replace('\\','/')).normalize();
            if (!p.startsWith(root.normalize()) || !Files.isRegularFile(p)) throw new IOException("PDF não encontrado.");
            return p;
        }
        String importPdf(String saleId, Path input) throws IOException {
            byte[] header;
            try (InputStream in = Files.newInputStream(input)) { header = in.readNBytes(5); }
            if (!Arrays.equals(header, "%PDF-".getBytes(StandardCharsets.US_ASCII))) throw new IOException("O arquivo não é um PDF reconhecido.");
            Path d = root.resolve("documentos").resolve(saleId); Files.createDirectories(d);
            Path target = d.resolve(id() + ".pdf"); Files.copy(input, target);
            return root.relativize(target).toString();
        }
    }

    final Store store;
    JFrame frame;
    final JPanel shell = new JPanel(new BorderLayout());
    final JPanel pages = Ui.plain(new CardLayout());
    final Ui.Button[] navigation = new Ui.Button[5];
    int activePage;
    final JLabel pageTitle = Ui.label("Novo atendimento",27,true,Ui.INK);
    final JLabel pageSubtitle = Ui.label("",13,false,Ui.MUTED);
    final JLabel status = Ui.label("Pronto para atender",12,false,Ui.MUTED);
    final JLabel shortcuts = Ui.label("",11,false,Ui.MUTED);
    List<Item> items = List.of(); List<Customer> customers = List.of(); List<Sale> sales = List.of();
    final List<Line> cart = new ArrayList<>();
    final DefaultTableModel itemModel = model("Tipo", "Nome", "Código", "Preço");
    final DefaultTableModel customerModel = model("Nome", "CPF/CNPJ (informado)", "Contato");
    final DefaultTableModel cartModel = model("Tipo", "Descrição", "Qtd.", "Unitário", "Total");
    final DefaultTableModel saleModel = model("Data", "Cliente", "Serviços", "Produtos", "Total", "Documento");
    final JTable itemTable = Ui.table(itemModel,"Seu catálogo começa aqui","Cadastre um serviço de impressão ou um produto."),
        customerTable = Ui.table(customerModel,"Clientes, sempre à mão","Cadastre um cliente para agilizar o próximo atendimento."),
        cartTable = Ui.table(cartModel,"Adicione o primeiro item","Leia um código ou escolha no catálogo acima."),
        saleTable = Ui.table(saleModel,"Tudo organizado em um só lugar","Os atendimentos salvos aparecerão aqui.");
    final JComboBox<Item> itemChoice = new JComboBox<>();
    final JComboBox<Customer> customerChoice = new JComboBox<>();
    final JTextField barcode = Ui.field("","Leia ou digite o código",16), quantity = Ui.field("1","",4);
    final JTextArea notes = new JTextArea(4, 20);
    final JLabel totals = Ui.label("R$ 0,00",32,true,Ui.INK);
    final JLabel serviceTotal = Ui.label("R$ 0,00",14,true,Ui.INK), productTotal = Ui.label("R$ 0,00",14,true,Ui.INK);
    final JLabel cartCount = Ui.label("0 itens",12,false,Ui.MUTED), itemCount = Ui.label("",12,false,Ui.MUTED), customerCount = Ui.label("",12,false,Ui.MUTED), historyCount = Ui.label("",12,false,Ui.MUTED);
    final JLabel selectedCustomer = Ui.label("Selecione um atendimento",15,true,Ui.INK), selectedTotal = Ui.label("—",26,true,Ui.INK);
    final JLabel selectedDate = Ui.label("",12,false,Ui.MUTED), selectedPdf = Ui.label("",12,false,Ui.MUTED);
    final List<JButton> selectionActions = new ArrayList<>(), pdfActions = new ArrayList<>(), serviceActions = new ArrayList<>();
    JTextField historySearch;
    interface Action { void run() throws Exception; }
    static DefaultTableModel model(String... cols) { return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    JButton button(String label, Action action) {
        return button(label,null,false,action);
    }
    Ui.Button button(String label,String icon,boolean primary,Action action) {
        Ui.Button b = new Ui.Button(label,icon,primary); b.addActionListener(e -> perform(action)); return b;
    }
    void perform(Action a) { try { a.run(); } catch (Exception e) { JOptionPane.showMessageDialog(frame,
        e.getMessage() == null ? e.toString() : e.getMessage(), "Não foi possível concluir", JOptionPane.ERROR_MESSAGE); } }
    JPanel row(Component... cs) { JPanel p = Ui.plain(new FlowLayout(FlowLayout.LEFT,8,0)); for (Component c : cs) p.add(c); return p; }
    JPanel page(Component center, Component bottom) { JPanel p = Ui.plain(new BorderLayout(0,16)); p.add(center); p.add(bottom,BorderLayout.SOUTH); return p; }
    int selected(JTable t) { int n = t.getSelectedRow(); if (n < 0) throw new IllegalArgumentException("Selecione uma linha primeiro."); return t.convertRowIndexToModel(n); }
    Sale selectedSale() { return sales.get(selected(saleTable)); }
    JTextField field(String text) { return Ui.field(text,"",24); }
    boolean form(String title, Object[][] rows) {
        JPanel p = Ui.vertical(); p.setBorder(BorderFactory.createEmptyBorder(8,4,8,4));
        for (Object[] r : rows) { JComponent input=(JComponent)r[1]; if(input instanceof JComboBox<?> combo)Ui.combo(combo); Ui.add(p,Ui.labeled(r[0].toString(),input));Ui.gap(p,14); }
        return JOptionPane.showConfirmDialog(frame, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    static String required(String s) { if (s.isBlank()) throw new IllegalArgumentException("Informe o nome/descrição."); return s.strip(); }
    App(Store store) throws Exception {
        this(store,false);
    }
    App(Store store,boolean preview) throws Exception {
        this.store = store;
        Ui.combo(itemChoice);Ui.combo(customerChoice);itemChoice.setPrototypeDisplayValue(new Item("","","Selecione um serviço ou produto","",BigDecimal.ZERO));
        customerChoice.setPrototypeDisplayValue(new Customer("","Cliente não identificado","",""));
        buildShell(); refresh(); showPage(0);
        if(preview)return;
        frame = new JFrame("Papelaria · atendimento e NFS-e");
        frame.setIconImage(Ui.appIcon());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosing(java.awt.event.WindowEvent e) {
            if (cart.isEmpty() || JOptionPane.showConfirmDialog(frame, "Há um atendimento não salvo. Fechar e descartá-lo?", "Fechar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) frame.dispose();
        }});
        Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(1080,screen.width),Math.min(720,screen.height)));frame.setSize(Math.min(1320,screen.width),Math.min(850,screen.height));frame.setContentPane(shell);frame.setLocationRelativeTo(null);frame.setVisible(true);
        SwingUtilities.invokeLater(() -> barcode.requestFocusInWindow());
    }
    void buildShell() {
        shell.setBackground(Ui.BG);
        pageTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));pageSubtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE,24));
        JPanel sidebar=new JPanel(new BorderLayout());sidebar.setBackground(Ui.NAV);sidebar.setPreferredSize(new Dimension(202,0));sidebar.setBorder(BorderFactory.createEmptyBorder(26,18,20,18));
        JPanel menu=Ui.vertical();
        Ui.Pill mark=new Ui.Pill("P",Color.WHITE,Ui.ACCENT);mark.setFont(Ui.font(20,true));mark.setBorder(BorderFactory.createEmptyBorder(7,13,7,13));
        JPanel brand=Ui.plain(new BorderLayout(10,0));brand.add(mark,BorderLayout.WEST);JPanel names=Ui.vertical();Ui.add(names,Ui.label("Papelaria",20,true,Color.WHITE));Ui.gap(names,3);Ui.add(names,Ui.label("Seu balcão organizado",10,false,Ui.NAV_MUTED));brand.add(names);
        Ui.add(menu,brand);Ui.gap(menu,38);Ui.add(menu,Ui.label("OPERAÇÃO",10,true,Ui.NAV_MUTED));Ui.gap(menu,12);
        String[] labels={"Atendimento","Catálogo","Clientes","Histórico","Ajuda"};String[] icons={"cart","catalog","users","history","help"};
        for(int index:new int[]{0,3,1,2,4}){
            if(index==1||index==4){Ui.gap(menu,24);Ui.add(menu,Ui.label(index==1?"CADASTROS":"SUPORTE",10,true,Ui.NAV_MUTED));Ui.gap(menu,12);}
            final int n=index;Ui.Button b=button(labels[index],icons[index],false,()->showPage(n));b.nav();b.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));b.setToolTipText("Ctrl + "+(n+1));navigation[index]=b;Ui.add(menu,b);Ui.gap(menu,5);
        }
        sidebar.add(menu,BorderLayout.NORTH);JPanel sidebarFooter=Ui.vertical();Ui.add(sidebarFooter,Ui.label("●  Dados neste computador",10,false,Ui.NAV_MUTED));Ui.gap(sidebarFooter,8);Ui.add(sidebarFooter,Ui.label("Versão 0.5 · Java",10,false,Ui.NAV_MUTED));sidebar.add(sidebarFooter,BorderLayout.SOUTH);shell.add(sidebar,BorderLayout.WEST);
        JPanel workspace=Ui.plain(new BorderLayout(0,22));workspace.setBorder(BorderFactory.createEmptyBorder(26,28,16,28));
        JPanel heading=Ui.plain(new BorderLayout(16,0));JPanel titles=Ui.vertical();Ui.add(titles,pageTitle);Ui.gap(titles,6);Ui.add(titles,pageSubtitle);heading.add(titles);
        JPanel date=Ui.vertical();Ui.add(date,new Ui.Pill("MEI · NFS-e nacional",Ui.ACCENT,Ui.SOFT_BLUE));Ui.gap(date,7);
        Ui.add(date,Ui.label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.forLanguageTag("pt-BR"))),12,false,Ui.MUTED));heading.add(date,BorderLayout.EAST);workspace.add(heading,BorderLayout.NORTH);
        pages.add(buildCart(),"0");pages.add(buildCatalog(),"1");pages.add(buildCustomers(),"2");pages.add(buildHistory(),"3");pages.add(buildHelp(),"4");workspace.add(pages);
        JPanel footer=Ui.plain(new BorderLayout());footer.add(status);footer.add(shortcuts,BorderLayout.EAST);workspace.add(footer,BorderLayout.SOUTH);shell.add(workspace);
        for(int n=0;n<5;n++){final int index=n;shell.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control "+(n+1)),"page"+n);shell.getActionMap().put("page"+n,new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){showPage(index);}});}
        shell.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F2"),"barcode");shell.getActionMap().put("barcode",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){showPage(0);barcode.requestFocusInWindow();}});
        shell.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control S"),"save");shell.getActionMap().put("save",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(activePage==0)perform(App.this::saveCart);}});
    }
    void showPage(int index) {
        activePage=index;((CardLayout)pages.getLayout()).show(pages,""+index);
        String[] titles={"Novo atendimento","Serviços e produtos","Seus clientes","Histórico e notas","Ajuda e backup"};
        String[] hints={"Do primeiro item ao comprovante, tudo no mesmo lugar.","Organize o que você imprime e o que você vende.","Tenha os dados à mão no próximo atendimento.","Consulte atendimentos, organize PDFs e reimprima.","Um fluxo simples para atender, emitir e guardar."};
        pageTitle.setText(titles[index]);pageSubtitle.setText(hints[index]);for(int n=0;n<5;n++){navigation[n].active=n==index;navigation[n].repaint();}
        shortcuts.setText(index==0?"F2  Código de barras     Ctrl+S  Salvar":"Ctrl+1 a Ctrl+5  Navegar");
    }
    JTextField search(JTable table,String hint) {
        JTextField f=Ui.field("",hint,22);TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>((DefaultTableModel)table.getModel());table.setRowSorter(sorter);
        for(int n=0;n<table.getColumnCount();n++)sorter.setSortable(n,false);
        f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter();}public void removeUpdate(javax.swing.event.DocumentEvent e){filter();}public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}
            void filter(){String query=fold(f.getText().strip());sorter.setRowFilter(query.isBlank()?null:new RowFilter<DefaultTableModel,Integer>(){public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){for(int n=0;n<entry.getValueCount();n++)if(fold(entry.getStringValue(n)).contains(query))return true;return false;}});}
        });return f;
    }
    static String fold(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
    JPanel tablePage(JTable table,JTextField search,JLabel count,JButton create,JButton edit) {
        Ui.Card card=new Ui.Card(new BorderLayout(0,18));JPanel toolbar=Ui.plain(new BorderLayout(14,0));toolbar.add(search);toolbar.add(create,BorderLayout.EAST);card.add(toolbar,BorderLayout.NORTH);card.add(Ui.scroll(table));
        JPanel footer=Ui.plain(new BorderLayout());footer.add(count);footer.add(edit,BorderLayout.EAST);card.add(footer,BorderLayout.SOUTH);edit.setEnabled(false);table.getSelectionModel().addListSelectionListener(e->edit.setEnabled(table.getSelectedRow()>=0));
        table.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2 && table.getSelectedRow()>=0)edit.doClick();}});
        return card;
    }
    JPanel buildCatalog(){int[] widths={110,420,230,130};for(int n=0;n<4;n++)itemTable.getColumnModel().getColumn(n).setPreferredWidth(widths[n]);itemTable.getColumnModel().getColumn(0).setMaxWidth(120);itemTable.getColumnModel().getColumn(3).setMinWidth(105);return tablePage(itemTable,search(itemTable,"Buscar nome, tipo ou código"),itemCount,button("Novo item","plus",true,()->editItem(null)),button("Editar item","edit",false,()->editItem(items.get(selected(itemTable)))));}
    JPanel buildCustomers(){return tablePage(customerTable,search(customerTable,"Buscar nome, documento ou contato"),customerCount,button("Novo cliente","plus",true,()->editCustomer(null)),button("Editar cliente","edit",false,()->editCustomer(customers.get(selected(customerTable)))));}
    void refresh() throws IOException {
        Item prev = (Item)itemChoice.getSelectedItem(); Customer cp = (Customer)customerChoice.getSelectedItem();
        String previousSale = saleTable.getSelectedRow()<0?null:selectedSale().id;
        items = store.items(); customers = store.customers(); sales = store.sales();
        itemModel.setRowCount(0); itemChoice.removeAllItems();
        for (Item i : items) { itemModel.addRow(new Object[]{i.kind, i.name, i.code, money(i.price)}); itemChoice.addItem(i); if (prev != null && prev.id.equals(i.id)) itemChoice.setSelectedItem(i); }
        customerModel.setRowCount(0); customerChoice.removeAllItems(); customerChoice.addItem(new Customer("", "Cliente não identificado", "", ""));
        for (Customer c : customers) { customerModel.addRow(new Object[]{c.name, c.document, c.contact}); customerChoice.addItem(c); if (cp != null && cp.id.equals(c.id)) customerChoice.setSelectedItem(c); }
        saleModel.setRowCount(0);
        for (Sale s : sales) saleModel.addRow(new Object[]{LocalDateTime.parse(s.date).format(DATE), s.customer,
            money(sum(s.lines, "Serviço")), money(sum(s.lines, "Produto")), money(s.total()), s.pdf.isBlank() ? "Sem PDF" : "PDF anexado"});
        itemCount.setText(items.size()+" itens cadastrados");customerCount.setText(customers.size()+" clientes cadastrados");historyCount.setText(sales.size()+" atendimentos salvos");
        if(previousSale!=null)selectSale(previousSale);updateHistoryActions();
    }
    void editItem(Item existing) throws Exception {
        JComboBox<String> kind = new JComboBox<>(KINDS); kind.setSelectedItem(existing == null ? "Serviço" : existing.kind);
        JTextField name = field(existing == null ? "" : existing.name), code = field(existing == null ? "" : existing.code);
        JTextField price = field(existing == null ? "" : existing.price.toPlainString().replace('.', ','));
        if (!form("Cadastro de item", new Object[][]{{"Tipo",kind},{"Nome / descrição",name},{"Código de barras / interno",code},{"Preço unitário (R$)",price}})) return;
        String itemId = existing == null ? id() : existing.id, c = code.getText().strip();
        if (!c.isEmpty() && items.stream().anyMatch(i -> i.code.equals(c) && !i.id.equals(itemId))) throw new IllegalArgumentException("Este código já pertence a outro item.");
        store.item(new Item(itemId, kind.getSelectedItem().toString(), required(name.getText()), c, decimal(price.getText(), false))); refresh();
    }
    void editCustomer(Customer c) throws Exception {
        JTextField name = field(c == null ? "" : c.name), doc = field(c == null ? "" : c.document), contact = field(c == null ? "" : c.contact);
        if (!form("Cadastro de cliente", new Object[][]{{"Nome",name},{"CPF/CNPJ (opcional)",doc},{"Contato (opcional)",contact}})) return;
        store.customer(new Customer(c == null ? id() : c.id, required(name.getText()), doc.getText().strip(), contact.getText().strip())); refresh();
    }
    JPanel buildCart() {
        JPanel p=Ui.plain(new BorderLayout(20,0));JPanel left=Ui.plain(new BorderLayout(0,18));
        Ui.Card entry=new Ui.Card(new BorderLayout(0,16));JPanel fields=Ui.vertical();
        Ui.add(fields,Ui.label("Adicionar ao atendimento",15,true,Ui.INK));Ui.gap(fields,16);
        Ui.add(fields,Ui.labeled("Cliente · opcional",customerChoice));Ui.gap(fields,14);
        JPanel codeRow=Ui.plain(new BorderLayout(12,0));codeRow.add(Ui.labeled("Código de barras",barcode));JPanel qty=Ui.labeled("Qtd.",quantity);qty.setPreferredSize(new Dimension(70,0));codeRow.add(qty,BorderLayout.EAST);Ui.add(fields,codeRow);Ui.gap(fields,14);
        JPanel choose=Ui.plain(new BorderLayout(10,0));choose.add(itemChoice);Ui.Button add=button("Adicionar","plus",true,()->{if(!barcode.getText().isBlank())addByCode();else add((Item)itemChoice.getSelectedItem());});choose.add(add,BorderLayout.EAST);
        Ui.add(fields,Ui.labeled("Ou selecione no catálogo",choose));Ui.gap(fields,9);Ui.add(fields,Ui.label("Leitor conectado? Leia o código e pressione Enter.",11,false,Ui.MUTED));entry.add(fields);left.add(entry,BorderLayout.NORTH);
        barcode.addActionListener(e -> perform(this::addByCode));
        quantity.addActionListener(e->perform(()->{if(!barcode.getText().isBlank())addByCode();else add((Item)itemChoice.getSelectedItem());}));
        Ui.Card cartCard=new Ui.Card(new BorderLayout(0,14));JPanel cartTitle=Ui.plain(new BorderLayout());cartTitle.add(Ui.label("Itens do atendimento",15,true,Ui.INK));cartTitle.add(cartCount,BorderLayout.EAST);cartCard.add(cartTitle,BorderLayout.NORTH);cartCard.add(Ui.scroll(cartTable));
        Ui.Button remove=button("Remover","trash",false,()->{cart.remove(selected(cartTable));refreshCart();});remove.setBorder(BorderFactory.createEmptyBorder(7,10,7,10));remove.setEnabled(false);cartTable.getSelectionModel().addListSelectionListener(e->remove.setEnabled(cartTable.getSelectedRow()>=0));
        JPanel cartTools=Ui.plain(new FlowLayout(FlowLayout.RIGHT,10,0));cartTools.add(cartCount);cartTools.add(remove);cartTitle.add(cartTools,BorderLayout.EAST);left.add(cartCard);p.add(left);
        int[] widths={80,186,56,92,96};for(int n=0;n<5;n++){cartTable.getColumnModel().getColumn(n).setPreferredWidth(widths[n]);if(n!=1)cartTable.getColumnModel().getColumn(n).setMinWidth(widths[n]);}
        JPanel right=Ui.plain(new BorderLayout(0,16));right.setPreferredSize(new Dimension(264,0));
        Ui.Card summary=new Ui.Card(new BorderLayout());JPanel summaryContent=Ui.vertical();Ui.add(summaryContent,Ui.label("Resumo",16,true,Ui.INK));Ui.gap(summaryContent,24);
        Ui.add(summaryContent,amountRow("Serviços",serviceTotal));Ui.gap(summaryContent,14);Ui.add(summaryContent,amountRow("Produtos",productTotal));Ui.gap(summaryContent,20);
        JSeparator line=new JSeparator();line.setForeground(Ui.LINE);line.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));Ui.add(summaryContent,line);Ui.gap(summaryContent,20);
        Ui.add(summaryContent,Ui.label("TOTAL DO ATENDIMENTO",10,true,Ui.MUTED));Ui.gap(summaryContent,7);Ui.add(summaryContent,totals);Ui.gap(summaryContent,22);
        Ui.Button save=button("Salvar atendimento","check",true,this::saveCart);save.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));Ui.add(summaryContent,save);Ui.gap(summaryContent,12);Ui.add(summaryContent,Ui.label("A NFS-e é emitida no portal nacional.",10,false,Ui.MUTED));summary.add(summaryContent);right.add(summary,BorderLayout.NORTH);
        Ui.Card observation=new Ui.Card(new BorderLayout(0,12));JPanel noteHeading=Ui.vertical();Ui.add(noteHeading,Ui.label("Descrição adicional",14,true,Ui.INK));Ui.gap(noteHeading,6);Ui.add(noteHeading,Ui.label("Somente observações dos serviços.",11,false,Ui.MUTED));observation.add(noteHeading,BorderLayout.NORTH);
        notes.setLineWrap(true);notes.setWrapStyleWord(true);notes.setFont(Ui.font(13,false));notes.setMargin(new Insets(8,8,8,8));JScrollPane noteScroll=Ui.scroll(notes);noteScroll.setBorder(new Ui.FieldBorder(false));observation.add(noteScroll);right.add(observation);p.add(right,BorderLayout.EAST);return p;
    }
    JPanel amountRow(String title,JLabel amount){JPanel p=Ui.plain(new BorderLayout());p.add(Ui.label(title,13,false,Ui.MUTED));p.add(amount,BorderLayout.EAST);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));return p;}
    void addByCode() {
        String code = barcode.getText().strip(); if (code.isEmpty()) throw new IllegalArgumentException("Leia ou digite um código.");
        Item i = items.stream().filter(it -> it.code.equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("Código não cadastrado. Cadastre o item primeiro."));
        add(i); barcode.setText(""); barcode.requestFocusInWindow();
    }
    void add(Item i) {
        if (i == null) throw new IllegalArgumentException("Cadastre e selecione um item.");
        cart.add(new Line(i.kind, i.name, i.price, decimal(quantity.getText(), true))); refreshCart();status.setText("Item adicionado: "+i.name);
    }
    void refreshCart() {
        cartModel.setRowCount(0); for (Line l : cart) cartModel.addRow(new Object[]{l.kind, l.name, l.qty, money(l.price), money(l.total())});
        totals.setText(money(sum(cart,null)));serviceTotal.setText(money(sum(cart,"Serviço")));productTotal.setText(money(sum(cart,"Produto")));cartCount.setText(cart.size()+" itens");
    }
    void saveCart() throws Exception {
        if (cart.isEmpty()) throw new IllegalArgumentException("Adicione ao menos um item.");
        Customer c = (Customer)customerChoice.getSelectedItem();
        Sale s = new Sale(id(), LocalDateTime.now().toString(), c.name, c.document, List.copyOf(cart), notes.getText().strip(), "", "");
        store.sale(s); cart.clear(); notes.setText("");historySearch.setText(""); refreshCart(); refresh();showPage(3);selectSale(s.id);
        status.setText("Atendimento salvo. Continue com a NFS-e no portal, se necessário.");
    }
    void selectSale(String id){for(int n=0;n<sales.size();n++)if(sales.get(n).id.equals(id)){int view=saleTable.convertRowIndexToView(n);if(view>=0){saleTable.setRowSelectionInterval(view,view);saleTable.scrollRectToVisible(saleTable.getCellRect(view,0,true));}return;}}
    JPanel buildHistory() {
        JPanel p=Ui.plain(new BorderLayout(20,0));Ui.Card list=new Ui.Card(new BorderLayout(0,16));historySearch=search(saleTable,"Buscar cliente, data ou valor");list.add(historySearch,BorderLayout.NORTH);list.add(Ui.scroll(saleTable));list.add(historyCount,BorderLayout.SOUTH);p.add(list);
        Ui.Card actions=new Ui.Card(new BorderLayout());actions.setPreferredSize(new Dimension(264,0));JPanel stack=Ui.vertical();
        Ui.add(stack,Ui.label("ATENDIMENTO SELECIONADO",10,true,Ui.MUTED));Ui.gap(stack,13);Ui.add(stack,selectedCustomer);Ui.gap(stack,7);Ui.add(stack,selectedDate);Ui.gap(stack,10);Ui.add(stack,selectedTotal);Ui.gap(stack,7);Ui.add(stack,selectedPdf);Ui.gap(stack,22);
        historyButton(stack,"Detalhes e descrição","document",false,()->showSale(selectedSale()),selectionActions);
        Ui.gap(stack,15);Ui.add(stack,Ui.label("EMITIR E ANEXAR",10,true,Ui.MUTED));Ui.gap(stack,10);
        historyButton(stack,"Abrir portal nacional","external",false,()->Platform.browse(URI.create(PORTAL)),null);
        historyButton(stack,"Anexar PDF oficial","plus",true,this::attachPdf,serviceActions);
        Ui.gap(stack,15);Ui.add(stack,Ui.label("DOCUMENTOS E IMPRESSÃO",10,true,Ui.MUTED));Ui.gap(stack,10);
        historyButton(stack,"Abrir PDF","document",false,()->Platform.open(store.pdf(selectedSale())),pdfActions);
        historyButton(stack,"Salvar cópia do PDF","folder",false,this::copyPdf,pdfActions);
        historyButton(stack,"Imprimir PDF A4","print",false,this::printPdf,pdfActions);
        historyButton(stack,"Comprovante térmico","print",false,()->printReceipt(selectedSale()),selectionActions);
        historyButton(stack,"Salvar PDF 58 × 210","folder",false,this::saveReceiptPdf,selectionActions);
        Ui.gap(stack,5);Ui.add(stack,Ui.label("Comprovante não fiscal · 58 × 210 mm",10,false,Ui.MUTED));actions.add(Ui.scroll(stack));p.add(actions,BorderLayout.EAST);
        saleTable.removeColumn(saleTable.getColumnModel().getColumn(3));saleTable.removeColumn(saleTable.getColumnModel().getColumn(2));
        saleTable.getColumnModel().getColumn(0).setPreferredWidth(140);saleTable.getColumnModel().getColumn(0).setMinWidth(138);saleTable.getColumnModel().getColumn(1).setPreferredWidth(180);saleTable.getColumnModel().getColumn(2).setPreferredWidth(92);saleTable.getColumnModel().getColumn(2).setMinWidth(90);saleTable.getColumnModel().getColumn(3).setPreferredWidth(110);saleTable.getColumnModel().getColumn(3).setMinWidth(108);
        saleTable.getSelectionModel().addListSelectionListener(e->updateHistoryActions());saleTable.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2&&saleTable.getSelectedRow()>=0)perform(()->showSale(selectedSale()));}});return p;
    }
    void historyButton(JPanel p,String title,String icon,boolean primary,Action action,List<JButton> group){Ui.Button b=button(title,icon,primary,action);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));Ui.add(p,b);Ui.gap(p,6);if(group!=null)group.add(b);}
    void updateHistoryActions(){
        Sale s=saleTable.getSelectedRow()<0?null:selectedSale();boolean chosen=s!=null;
        selectionActions.forEach(b->b.setEnabled(chosen));pdfActions.forEach(b->b.setEnabled(chosen&&!s.pdf.isBlank()));serviceActions.forEach(b->b.setEnabled(chosen&&s.lines.stream().anyMatch(l->l.kind.equals("Serviço"))));
        selectedCustomer.setText(chosen?s.customer:"Selecione um atendimento");selectedCustomer.setToolTipText(chosen?s.customer:null);selectedTotal.setText(chosen?money(s.total()):"—");selectedDate.setText(chosen?LocalDateTime.parse(s.date).format(DATE):"Veja os detalhes e as ações aqui.");selectedPdf.setText(chosen?(s.pdf.isBlank()?"Nenhum PDF anexado":"PDF oficial anexado pelo usuário"):"");
    }
    void showSale(Sale s) {
        JTextArea text = new JTextArea(22,75); text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setFont(Ui.font(14,false));text.setMargin(new Insets(16,18,16,18));
        StringBuilder b = new StringBuilder("Cliente: " + s.customer + "\nCPF/CNPJ informado: " + s.document + "\n\nItens do atendimento:\n");
        for (Line l : s.lines) b.append(l.kind).append(" — ").append(l.name).append(" — ").append(l.qty).append(" — ").append(money(l.total())).append('\n');
        b.append("\nTOTAL: ").append(money(s.total())).append("\n\nTEXTO DOS SERVIÇOS PARA REVISAR E COPIAR AO PORTAL:\n").append(description(s));
        b.append("\n\nPDF anexado: ").append(s.pdf.isBlank() ? "não" : "sim").append("\nReferência informada: ").append(s.reference);
        text.setText(b.toString()); text.setCaretPosition(0);
        Object[] options = {"Copiar texto dos serviços", "Fechar"};
        if (JOptionPane.showOptionDialog(frame,Ui.scroll(text),"Detalhes do atendimento",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[1]) == 0) {
            String desc = description(s); if (s.lines.stream().noneMatch(l -> l.kind.equals("Serviço"))) throw new IllegalArgumentException("Este atendimento só tem produtos. Não há serviços para NFS-e.");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(desc), null);
        }
    }
    JFileChooser chooser(String title) { JFileChooser c = new JFileChooser(); c.setDialogTitle(title); return c; }
    void attachPdf() throws Exception {
        Sale s = selectedSale();
        if (s.lines.stream().noneMatch(l -> l.kind.equals("Serviço"))) throw new IllegalArgumentException("Este atendimento não contém serviços. A NFS-e não deve documentar a venda de produtos.");
        JFileChooser c = chooser("Selecione o PDF oficial baixado do portal"); c.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        if (c.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        String ref = JOptionPane.showInputDialog(frame, "Número / referência da NFS-e (informado por você; não validado pelo app):", s.reference);
        if (ref == null) return;
        if (JOptionPane.showConfirmDialog(frame, "Confira o emitente, cliente, serviços e valores do PDF.\nO aplicativo armazena o arquivo; não verifica sua autenticidade fiscal.\nAnexar a este atendimento?", "Vincular documento", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String file = store.importPdf(s.id, c.getSelectedFile().toPath());
        store.sale(new Sale(s.id,s.date,s.customer,s.document,s.lines,s.notes,file,ref.strip())); refresh();
    }
    void copyPdf() throws Exception {
        Sale s = selectedSale(); Path source = store.pdf(s); JFileChooser c = chooser("Salvar cópia do PDF"); c.setSelectedFile(new File("NFS-e-" + s.id.substring(0,8) + ".pdf"));
        if (c.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path dest = c.getSelectedFile().toPath(); if (!dest.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) dest = dest.resolveSibling(dest.getFileName()+".pdf");
        if (Files.exists(dest) && JOptionPane.showConfirmDialog(frame,"Substituir o arquivo existente?","Salvar",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (source.toAbsolutePath().equals(dest.toAbsolutePath())) return;
        Files.copy(source,dest,StandardCopyOption.REPLACE_EXISTING);
    }
    void printPdf() throws Exception {
        Path p = store.pdf(selectedSale());
        Platform.open(p);
        JOptionPane.showMessageDialog(frame, "O PDF foi aberto no leitor padrão do sistema.\nUse Ctrl+P, selecione uma impressora A4 e preserve o tamanho do documento.\nA Elgin de 58 × 210 mm deve ser usada para o comprovante não fiscal.");
    }
    void saveReceiptPdf() throws Exception {
        Sale sale=selectedSale();JFileChooser chooser=chooser("Salvar comprovante não fiscal em PDF de 58 × 210 mm");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF","pdf"));chooser.setSelectedFile(new File("Comprovante-"+sale.id.substring(0,8)+".pdf"));
        if(chooser.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        Path destination=chooser.getSelectedFile().toPath();
        if(!destination.toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))destination=destination.resolveSibling(destination.getFileName()+".pdf");
        if(Files.exists(destination)&&JOptionPane.showConfirmDialog(frame,"Substituir o arquivo existente?","Salvar comprovante",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        ReceiptPdf.save(sale,destination);status.setText("Comprovante não fiscal salvo em PDF de 58 × 210 mm.");
    }
    static List<String> receipt(Sale s) {
        List<String> out = new ArrayList<>();
        out.add(ReceiptPdf.COMPANY);out.add("CNPJ: "+ReceiptPdf.CNPJ);
        out.add("COMPROVANTE DE ATENDIMENTO"); out.add("DOCUMENTO NAO FISCAL"); out.add("Atendimento: " + s.id.substring(0,8));
        out.add("Data: " + LocalDateTime.parse(s.date).format(DATE)); out.add("Cliente: " + s.customer);
        out.add("------------------------------");
        for (Line l : s.lines) { out.add(l.kind + ": " + l.name); out.add(l.qty.toPlainString() + " x " + money(l.price) + " = " + money(l.total())); }
        out.add("------------------------------"); out.add("Serviços: " + money(sum(s.lines,"Serviço"))); out.add("Produtos: " + money(sum(s.lines,"Produto"))); out.add("TOTAL: " + money(s.total()));
        if (!s.reference.isBlank()) out.add("NFS-e ref. informada: " + s.reference);
        out.add("Este comprovante não substitui"); out.add("a NFS-e nem a nota de produtos.");
        return out;
    }
    static List<String> wrap(List<String> lines, FontMetrics fm, double width) {
        List<String> out = new ArrayList<>();
        for (String original : lines) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < original.length(); i++) {
                char c = original.charAt(i);
                if (c == '\n' || c == '\r') { if (!line.isEmpty()) { out.add(line.toString()); line.setLength(0); } continue; }
                if (!line.isEmpty() && fm.stringWidth(line.toString() + c) > width) { out.add(line.toString()); line.setLength(0); }
                line.append(c);
            }
            out.add(line.toString());
        }
        return out;
    }
    static final class ThermalReceipt implements Printable {
        final List<String> raw;
        ThermalReceipt(List<String> raw) { this.raw = List.copyOf(raw); }
        public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
            Graphics2D g = (Graphics2D)graphics.create();
            try {
                g.translate(format.getImageableX(),format.getImageableY());
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8)); g.setColor(Color.BLACK);
                FontMetrics fm = g.getFontMetrics(); double width = format.getImageableWidth();
                if (width < 35 || format.getImageableHeight() < fm.getHeight()) throw new PrinterException("Área imprimível inválida. Confira o driver e o papel.");
                List<String> lines = wrap(raw.subList(2,raw.size()),fm,width);
                int perPage = (int)((format.getImageableHeight()-ReceiptPdf.HEADER-20)/ReceiptPdf.LEADING);
                if(perPage<1)throw new PrinterException("Configure o papel 58 × 210 mm em orientação retrato.");
                long start = (long)page * perPage; if (page < 0 || start >= lines.size()) return NO_SUCH_PAGE;
                new Ui.Glyph("print",Color.BLACK,24).paintIcon(null,g,(int)(width-24)/2,0);
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));g.drawString(ReceiptPdf.COMPANY,(float)(width-g.getFontMetrics().stringWidth(ReceiptPdf.COMPANY))/2,39);
                g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,8));String companyId="CNPJ: "+ReceiptPdf.CNPJ;g.drawString(companyId,(float)(width-g.getFontMetrics().stringWidth(companyId))/2,52);
                for (int n = (int)start, row = 0; n < Math.min(lines.size(),start+perPage); n++,row++)
                    g.drawString(lines.get(n),0,(float)(ReceiptPdf.HEADER+fm.getAscent()+row*ReceiptPdf.LEADING));
                return PAGE_EXISTS;
            } finally { g.dispose(); }
        }
    }
    void printReceipt(Sale s) throws Exception {
        PrinterJob job = PrinterJob.getPrinterJob(); job.setJobName("Comprovante não fiscal " + s.id.substring(0,8));
        PageFormat f = job.defaultPage(); Paper paper = new Paper();
        double mm = 72.0/25.4; paper.setSize(58*mm,210*mm); paper.setImageableArea(3*mm,3*mm,52*mm,204*mm); f.setPaper(paper);f.setOrientation(PageFormat.PORTRAIT);
        job.setPrintable(new ThermalReceipt(receipt(s)),f);
        if (!job.printDialog()) return;
        PageFormat validated = job.validatePage(f);validated.setOrientation(PageFormat.PORTRAIT); job.setPrintable(new ThermalReceipt(receipt(s)),validated); job.print();
    }
    JPanel buildHelp() {
        JPanel p=Ui.plain(new BorderLayout(20,0));Ui.Card guide=new Ui.Card(new BorderLayout());JPanel steps=Ui.vertical();
        Ui.add(steps,Ui.label("Seu fluxo de trabalho",17,true,Ui.INK));Ui.gap(steps,22);
        helpStep(steps,"1","Organize o catálogo","Cadastre os serviços de impressão e os produtos separadamente. Clientes são opcionais.");
        helpStep(steps,"2","Registre o atendimento","Leia um código ou escolha um item. Confira as quantidades, a descrição adicional e o total.");
        helpStep(steps,"3","Emita no portal nacional","No histórico, abra os detalhes e copie o texto dos serviços. Acesse o portal e revise os dados antes de emitir.");
        helpStep(steps,"4","Guarde e reimprima","Anexe o PDF oficial. Use A4 para o DANFSe. Salve o comprovante não fiscal em PDF de 58 × 210 mm para imprimir depois.");
        Ui.gap(steps,8);Ui.add(steps,Ui.label("ATALHOS",10,true,Ui.MUTED));Ui.gap(steps,10);Ui.add(steps,paragraph("F2 · foco no código de barras\nCtrl+S · salvar o atendimento\nCtrl+1 a Ctrl+5 · trocar de tela\nDuplo clique · abrir detalhes ou editar um cadastro",12));guide.add(steps,BorderLayout.NORTH);p.add(guide);
        JPanel right=Ui.plain(new GridLayout(2,1,0,18));right.setPreferredSize(new Dimension(320,0));Ui.Card backup=new Ui.Card(new BorderLayout(0,14));JPanel backupText=Ui.vertical();
        Ui.add(backupText,Ui.label("Seus dados e backup",16,true,Ui.INK));Ui.gap(backupText,12);Ui.add(backupText,paragraph("Seus cadastros, atendimentos e PDFs ficam neste computador. Feche o app e copie toda a pasta abaixo para guardar um backup.",12));Ui.gap(backupText,12);
        JTextArea path=paragraph(store.root.toString(),11);path.setForeground(Ui.ACCENT);Ui.add(backupText,path);backup.add(backupText,BorderLayout.NORTH);backup.add(button("Abrir pasta dos dados","folder",false,()->Platform.open(store.root)),BorderLayout.SOUTH);right.add(backup);
        Ui.Card fiscal=new Ui.Card(new BorderLayout());JPanel info=Ui.vertical();Ui.add(info,Ui.label("Notas e documentos",16,true,Ui.INK));Ui.gap(info,12);
        Ui.add(info,paragraph("A NFS-e é emitida por você no portal. O app organiza o PDF importado; não verifica sua autenticidade fiscal.\n\nAs vendas de produtos ficam separadas dos serviços. O comprovante térmico não substitui uma nota fiscal.",12));Ui.gap(info,12);
        Ui.Button more=button("Mais informações","help",false,()->JOptionPane.showMessageDialog(frame,paragraph("Versão 0.5 · Java 17+\n\nO app não emite, consulta ou cancela notas por API e não guarda a senha gov.br. Uma integração futura por API exige certificado digital.\n\nCPF/CNPJ são informados pelo usuário, sem validação fiscal. Os arquivos locais não são criptografados; proteja sua conta e seus backups.\n\nAlterações de cadastros não modificam atendimentos anteriores. Reimprimir não emite uma nova nota.\n\nUse o leitor USB em modo teclado com Enter. As informações de linear imager e varreduras são características do leitor, um equipamento separado da impressora.\n\nO app é destinado a um computador e uma instância por vez. O formato dos dados é compatível com a versão 0.1.",13),"Informações do aplicativo",JOptionPane.PLAIN_MESSAGE));more.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));Ui.add(info,more);fiscal.add(info,BorderLayout.NORTH);right.add(fiscal);p.add(right,BorderLayout.EAST);return p;
    }
    JTextArea paragraph(String text,int fontSize){JTextArea a=new JTextArea(text);a.setFont(Ui.font(fontSize,false));a.setForeground(Ui.MUTED);a.setOpaque(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setEditable(false);a.setFocusable(true);a.setColumns(42);int rows=0;for(String line:text.split("\n",-1))rows+=Math.max(1,(line.length()+47)/48);a.setRows(Math.max(2,rows));return a;}
    void helpStep(JPanel p,String number,String title,String body){JPanel step=Ui.plain(new BorderLayout(14,0));Ui.Pill pill=new Ui.Pill(number,Ui.ACCENT,Ui.SOFT_BLUE);pill.setFont(Ui.font(14,true));JPanel left=Ui.plain(new BorderLayout());left.add(pill,BorderLayout.NORTH);step.add(left,BorderLayout.WEST);JPanel texts=Ui.vertical();Ui.add(texts,Ui.label(title,14,true,Ui.INK));Ui.gap(texts,7);Ui.add(texts,paragraph(body,13));step.add(texts);Ui.add(p,step);Ui.gap(p,24);}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Ui.install();
                String appData = System.getenv("LOCALAPPDATA");
                Path root = Path.of(appData == null || appData.isBlank() ? System.getProperty("user.home") : appData).resolve("PapelariaNfse").toAbsolutePath();
                new App(new Store(root));
            } catch (Exception e) { JOptionPane.showMessageDialog(null, "Falha ao iniciar: " + e.getMessage() + "\nOs arquivos existentes foram preservados.", "Papelaria", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
