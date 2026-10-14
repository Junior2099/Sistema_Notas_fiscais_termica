package br.com.papelaria;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Interface nativa, sem dependências externas. */
final class Ui {
    static final Color BG = new Color(245,247,251), WHITE = Color.WHITE;
    static final Color INK = new Color(27,39,59), MUTED = new Color(106,119,140);
    static final Color LINE = new Color(225,231,239), ACCENT = new Color(40,91,224);
    static final Color NAV = new Color(20,33,54), NAV_MUTED = new Color(152,169,192);
    static final Color SOFT_BLUE = new Color(237,243,255), GREEN = new Color(24,119,93);
    static final Color SOFT_GREEN = new Color(230,246,239);
    static String family = "SansSerif";
    static Font font(int size, boolean bold) { return new Font(family,bold?Font.BOLD:Font.PLAIN,size); }
    static Image appIcon(){java.awt.image.BufferedImage img=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);Graphics2D g=img.createGraphics();aa(g);g.setColor(ACCENT);g.fillRoundRect(2,2,60,60,16,16);g.setFont(font(42,true));g.setColor(WHITE);FontMetrics fm=g.getFontMetrics();g.drawString("P",(64-fm.stringWidth("P"))/2,(64-fm.getHeight())/2+fm.getAscent());g.dispose();return img;}
    static void install() throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains("Segoe UI")) family="Segoe UI";
        for (Object key : new ArrayList<>(UIManager.getDefaults().keySet()))
            if (key.toString().endsWith(".font")) UIManager.put(key,new javax.swing.plaf.FontUIResource(font(13,false)));
        UIManager.put("Panel.background",BG); UIManager.put("Label.foreground",INK);
        UIManager.put("TextField.background",WHITE); UIManager.put("TextField.foreground",INK);
        UIManager.put("TextArea.background",WHITE); UIManager.put("TextArea.foreground",INK);
        UIManager.put("TextField.selectionBackground",SOFT_BLUE); UIManager.put("TextArea.selectionBackground",SOFT_BLUE);
        UIManager.put("TextField.caretForeground",INK); UIManager.put("TextArea.caretForeground",INK);
        UIManager.put("OptionPane.background",BG); UIManager.put("OptionPane.messageForeground",INK);
        UIManager.put("Button.background",WHITE); UIManager.put("Button.foreground",INK);
        UIManager.put("Button.border",new CompoundBorder(new LineBorder(LINE),new EmptyBorder(8,14,8,14)));
        UIManager.put("ScrollBar.width",10); UIManager.put("ToolTip.background",INK); UIManager.put("ToolTip.foreground",WHITE);
        UIManager.put("TabbedPane.background",BG); UIManager.put("TabbedPane.selected",WHITE);
    }
    static JLabel label(String s,int size,boolean bold,Color c) { JLabel l=new JLabel(s); l.setFont(font(size,bold)); l.setForeground(c); return l; }
    static JPanel plain(LayoutManager l) { JPanel p=new JPanel(l); p.setOpaque(false); return p; }
    static JPanel vertical() { JPanel p=new ScrollColumn();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p; }
    static final class ScrollColumn extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return orientation==SwingConstants.VERTICAL?Math.max(24,r.height-24):Math.max(24,r.width-24);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    static void add(JPanel p,JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); }
    static void gap(JPanel p,int size) { p.add(Box.createVerticalStrut(size)); }
    static JPanel labeled(String title,JComponent input) {
        JPanel p=plain(new BorderLayout(0,7)); p.add(label(title,12,true,MUTED),BorderLayout.NORTH); p.add(input); return p;
    }
    static JTextField field(String initial,String hint,int columns) {
        HintField f=new HintField(initial,hint,columns); styleField(f); return f;
    }
    static void styleField(JTextField f) {
        f.setFont(font(14,false)); f.setBackground(WHITE); f.setForeground(INK); f.setCaretColor(INK);
        f.setBorder(new FieldBorder(false)); f.setPreferredSize(new Dimension(f.getPreferredSize().width,42));
        f.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e){f.setBorder(new FieldBorder(true));} public void focusLost(FocusEvent e){f.setBorder(new FieldBorder(false));} });
    }
    static final class HintField extends JTextField {
        final String hint;
        HintField(String text,String hint,int columns){super(text,columns);this.hint=hint;}
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g=(Graphics2D)graphics.create(); aa(g); g.setFont(getFont()); g.setColor(MUTED);
                g.drawString(hint,getInsets().left,(getHeight()-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());g.dispose();
            }
        }
    }
    static final class FieldBorder extends AbstractBorder {
        final boolean focused;
        FieldBorder(boolean focused){this.focused=focused;}
        public Insets getBorderInsets(Component c){return new Insets(10,12,10,12);}
        public Insets getBorderInsets(Component c,Insets i){i.set(10,12,10,12);return i;}
        public void paintBorder(Component c,Graphics graphics,int x,int y,int w,int h){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.setColor(focused?ACCENT:LINE);g.setStroke(new BasicStroke(focused?1.6f:1f));g.drawRoundRect(x+1,y+1,w-3,h-3,10,10);g.dispose();}
    }
    static void combo(JComboBox<?> c) {
        c.setFont(font(14,false)); c.setBackground(WHITE); c.setForeground(INK);
        c.setUI(new BasicComboBoxUI(){
            protected JButton createArrowButton(){JButton b=new JButton(){protected void paintComponent(Graphics g){new Glyph("chevron",MUTED,16).paintIcon(this,g,(getWidth()-16)/2,(getHeight()-16)/2);}};b.setBorder(BorderFactory.createEmptyBorder());b.setContentAreaFilled(false);b.setPreferredSize(new Dimension(24,24));return b;}
            public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus){g.setColor(WHITE);g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);}
            public void paintCurrentValue(Graphics graphics,Rectangle bounds,boolean focus){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.clip(bounds);g.setFont(c.getFont());g.setColor(INK);String s=c.getSelectedItem()==null?"Selecione um item":c.getSelectedItem().toString();FontMetrics fm=g.getFontMetrics();int width=bounds.width-4;if(fm.stringWidth(s)>width){while(!s.isEmpty()&&fm.stringWidth(s+"…")>width)s=s.substring(0,s.length()-1);s+=width>=fm.stringWidth("…")?"…":"";}g.drawString(s,bounds.x+2,bounds.y+(bounds.height-fm.getHeight())/2+fm.getAscent());g.dispose();}
        });
        c.setBorder(new FieldBorder(false)); c.setPreferredSize(new Dimension(220,42));
        c.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int i,boolean selected,boolean focused){
            JLabel l=(JLabel)super.getListCellRendererComponent(list,value,i,selected,focused);l.setFont(font(14,false));l.setBorder(new EmptyBorder(i<0?0:8,2,i<0?0:8,6));
            l.setBackground(selected?SOFT_BLUE:WHITE);l.setForeground(INK);return l;
        }});
        c.setMaximumRowCount(12);
    }
    static JScrollPane scroll(Component component) {
        JScrollPane s=new JScrollPane(component);if(component instanceof JTable table)s.setColumnHeaderView(table.getTableHeader());s.setBorder(BorderFactory.createEmptyBorder());s.getViewport().setBackground(WHITE);s.setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(24);s.getVerticalScrollBar().setUI(new BasicScrollBarUI(){
            protected void configureScrollBarColors(){thumbColor=new Color(202,212,225);trackColor=WHITE;}
            protected JButton createDecreaseButton(int o){return zero();}protected JButton createIncreaseButton(int o){return zero();}
            private JButton zero(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
            protected void paintThumb(Graphics graphics,JComponent c,Rectangle r){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.setColor(thumbColor);g.fillRoundRect(r.x+2,r.y+2,Math.max(4,r.width-4),r.height-4,6,6);g.dispose();}
        });return s;
    }
    static void aa(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);}
    static final class Card extends JPanel {
        final Color fill; final boolean border;
        Card(LayoutManager layout){this(layout,WHITE,true);}
        Card(LayoutManager layout,Color fill,boolean border){super(layout);this.fill=fill;this.border=border;setOpaque(false);setBorder(new EmptyBorder(18,20,18,20));}
        protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.setColor(fill);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);if(border){g.setColor(LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);}g.dispose();super.paintComponent(graphics);}
    }
    static final class Button extends JButton {
        boolean primary,nav,active; String icon;
        Button(String text,String icon,boolean primary){super(text);this.icon=icon;this.primary=primary;
            setFont(font(13,true));setBorder(new EmptyBorder(11,15,11,15));setOpaque(false);setContentAreaFilled(false);setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setIconTextGap(9);setHorizontalAlignment(SwingConstants.CENTER);
            addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){repaint();}public void mouseExited(MouseEvent e){repaint();}});
        }
        void nav(){nav=true;setHorizontalAlignment(SwingConstants.LEFT);setBorder(new EmptyBorder(13,14,13,14));}
        public Dimension getPreferredSize(){Dimension d=super.getPreferredSize();return new Dimension(d.width+(icon==null?0:27),d.height);}
        protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();aa(g);
            Color bg=nav?(active?new Color(43,66,101):getModel().isRollover()?new Color(30,47,73):NAV):primary?(isEnabled()?(getModel().isPressed()?new Color(25,62,170):getModel().isRollover()?new Color(33,77,203):ACCENT):new Color(224,229,240)):(getModel().isRollover()&&isEnabled()?new Color(246,248,252):WHITE);
            Color fg=nav?(active?WHITE:NAV_MUTED):!isEnabled()?new Color(150,161,179):primary?WHITE:INK;
            g.setColor(bg);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
            if(!nav&&!primary){g.setColor(LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);}
            if(isFocusOwner()){g.setColor(primary?WHITE:ACCENT);g.drawRoundRect(3,3,getWidth()-7,getHeight()-7,8,8);}
            g.setFont(getFont());g.setColor(fg);FontMetrics fm=g.getFontMetrics();String text=getText();
            int iconWidth=icon==null?0:27,available=Math.max(0,getWidth()-getInsets().left-getInsets().right-iconWidth);
            if(fm.stringWidth(text)>available){while(!text.isEmpty()&&fm.stringWidth(text+"…")>available)text=text.substring(0,text.length()-1);text+="…";}
            int width=fm.stringWidth(text)+iconWidth,left=nav?getInsets().left:Math.max(getInsets().left,(getWidth()-width)/2);
            if(icon!=null)new Glyph(icon,fg,18).paintIcon(this,g,left,(getHeight()-18)/2);
            g.drawString(text,left+iconWidth,(getHeight()-fm.getHeight())/2+fm.getAscent());g.dispose();
        }
    }
    static final class Pill extends JLabel {
        final Color bg;
        Pill(String text,Color fg,Color bg){super(text);this.bg=bg;setForeground(fg);setFont(font(11,true));setBorder(new EmptyBorder(5,9,5,9));setOpaque(false);}
        protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.setColor(bg);g.fillRoundRect(0,0,getWidth(),getHeight(),10,10);g.dispose();super.paintComponent(graphics);}
    }
    static JTable table(DefaultTableModel model,String emptyTitle,String emptyHint) {
        JTable t=new JTable(model){protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getRowCount()==0){Graphics2D g=(Graphics2D)graphics.create();aa(g);int cy=Math.max(65,getHeight()/2-15);g.setColor(INK);g.setFont(font(16,true));g.drawString(emptyTitle,Math.max(12,(getWidth()-g.getFontMetrics().stringWidth(emptyTitle))/2),cy);g.setColor(MUTED);g.setFont(font(12,false));g.drawString(emptyHint,Math.max(12,(getWidth()-g.getFontMetrics().stringWidth(emptyHint))/2),cy+25);g.dispose();}}};
        t.setFont(font(13,false));t.setForeground(INK);t.setBackground(WHITE);t.setRowHeight(46);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(0,0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setSelectionBackground(SOFT_BLUE);t.setSelectionForeground(INK);t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focused,int r,int c){
            String s=String.valueOf(value);int col=table.convertColumnIndexToModel(c);
            if(s.equals("Serviço")||s.equals("Produto")||col==5 && table.getModel().getColumnCount()==6){
                boolean good=s.equals("Serviço")||s.equals("PDF anexado");return new JComponent(){protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.setColor(selected?SOFT_BLUE:r%2==0?WHITE:new Color(250,251,253));g.fillRect(0,0,getWidth(),getHeight());g.setFont(font(11,true));FontMetrics fm=g.getFontMetrics();int width=Math.min(getWidth()-16,fm.stringWidth(s)+16),y=(getHeight()-25)/2;g.setColor(good?SOFT_GREEN:new Color(238,242,247));g.fillRoundRect(8,y,width,25,10,10);g.setColor(good?GREEN:MUTED);g.clipRect(8,y,width,25);g.drawString(s,16,y+(25-fm.getHeight())/2+fm.getAscent());g.dispose();}};
            }
            JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,selected,false,r,c);l.setBorder(new EmptyBorder(0,12,0,12));
            l.setFont(font(12,s.startsWith("R$")));l.setForeground(s.startsWith("R$")?INK:col==0?MUTED:INK);l.setBackground(selected?SOFT_BLUE:r%2==0?WHITE:new Color(250,251,253));
            l.setHorizontalAlignment(s.startsWith("R$")||value instanceof java.math.BigDecimal?SwingConstants.RIGHT:SwingConstants.LEFT);l.setToolTipText(s);return l;
        }});
        t.getTableHeader().setReorderingAllowed(false);t.getTableHeader().setPreferredSize(new Dimension(100,42));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focused,int r,int c){
            JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,selected,focused,r,c);l.setBackground(new Color(246,248,251));l.setForeground(MUTED);l.setFont(font(11,true));l.setBorder(new EmptyBorder(8,12,8,12));return l;
        }});return t;
    }
    static final class Glyph implements Icon {
        final String name;final Color color;final int size;
        Glyph(String name,Color color,int size){this.name=name;this.color=color;this.size=size;}
        public int getIconWidth(){return size;}public int getIconHeight(){return size;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();aa(g);g.translate(x,y);g.scale(size/20.0,size/20.0);g.setColor(color);g.setStroke(new BasicStroke(1.65f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(name){
                case "plus" -> {g.drawLine(10,4,10,16);g.drawLine(4,10,16,10);}
                case "chevron" -> {g.drawLine(6,8,10,12);g.drawLine(10,12,14,8);}
                case "search" -> {g.drawOval(3,3,10,10);g.drawLine(12,12,17,17);}
                case "cart" -> {g.drawRoundRect(3,6,14,11,2,2);g.drawArc(7,2,6,8,0,180);}
                case "catalog" -> {g.drawRoundRect(3,3,14,14,2,2);g.drawLine(3,9,17,9);g.drawLine(9,3,9,17);}
                case "users" -> {g.drawOval(6,2,7,7);g.drawArc(3,11,14,12,0,180);}
                case "history" -> {g.drawOval(3,3,14,14);g.drawLine(10,6,10,10);g.drawLine(10,10,14,12);}
                case "help" -> {g.drawOval(3,3,14,14);g.drawArc(7,5,6,6,-20,200);g.drawLine(10,10,10,12);g.fillOval(9,14,2,2);}
                case "document" -> {g.drawRoundRect(5,2,11,16,2,2);g.drawLine(8,7,13,7);g.drawLine(8,11,13,11);g.drawLine(8,14,12,14);}
                case "print" -> {g.drawRoundRect(2,7,16,8,2,2);g.drawRect(5,2,10,5);g.drawRect(5,11,10,7);g.drawLine(14,10,15,10);}
                case "folder" -> {g.drawRoundRect(2,5,16,12,2,2);g.drawLine(3,5,3,3);g.drawLine(3,3,8,3);g.drawLine(8,3,10,5);}
                case "edit" -> {g.rotate(-Math.PI/4,10,10);g.drawRect(8,3,4,12);g.drawLine(8,15,10,18);g.drawLine(10,18,12,15);}
                case "copy" -> {g.drawRoundRect(6,6,11,12,2,2);g.drawLine(3,14,3,3);g.drawLine(3,3,13,3);}
                case "external" -> {g.drawLine(10,3,17,3);g.drawLine(17,3,17,10);g.drawLine(17,3,9,11);g.drawLine(7,5,3,5);g.drawLine(3,5,3,17);g.drawLine(3,17,15,17);g.drawLine(15,17,15,13);}
                case "check" -> {g.drawLine(4,10,8,14);g.drawLine(8,14,16,6);}
                case "trash" -> {g.drawLine(3,5,17,5);g.drawLine(7,2,13,2);g.drawRoundRect(5,5,10,13,2,2);g.drawLine(8,8,8,15);g.drawLine(12,8,12,15);}
                case "barcode" -> {for(int i=3;i<18;i+=3){g.drawLine(i,3,i,16);}g.drawLine(4,3,4,16);g.drawLine(13,3,13,16);}
                default -> g.drawOval(4,4,12,12);
            }g.dispose();
        }
    }
}
