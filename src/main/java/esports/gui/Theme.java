package esports.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class Theme {

    public static final Color BG_DARK      = new Color(0x0D, 0x0F, 0x17);
    public static final Color BG_PANEL     = new Color(0x13, 0x16, 0x22);
    public static final Color BG_CARD      = new Color(0x1A, 0x1E, 0x30);
    public static final Color BG_INPUT     = new Color(0x10, 0x13, 0x1F);
    public static final Color BG_TABLE_ROW = new Color(0x16, 0x1A, 0x28);
    public static final Color BG_TABLE_ALT = new Color(0x1C, 0x21, 0x35);
    public static final Color BG_SELECTED  = new Color(0x1F, 0x3A, 0x5F);

    public static final Color ACCENT_CYAN  = new Color(0x00, 0xE5, 0xFF);
    public static final Color ACCENT_BLUE  = new Color(0x22, 0x6B, 0xFF);
    public static final Color ACCENT_PINK  = new Color(0xFF, 0x2D, 0x78);
    public static final Color ACCENT_GREEN = new Color(0x00, 0xFF, 0x9F);

    public static final Color TEXT_PRIMARY   = new Color(0xE8, 0xEA, 0xFF);
    public static final Color TEXT_SECONDARY = new Color(0x7A, 0x83, 0xAA);

    public static final Color BORDER_SUBTLE = new Color(0x25, 0x2C, 0x45);
    public static final Color DANGER        = new Color(0xFF, 0x3B, 0x5C);

    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BTN     = new Font("Segoe UI", Font.BOLD, 12);

    private Theme() {}

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADING);
        l.setForeground(ACCENT_CYAN);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    public static JTextField input(int cols) {
        JTextField f = new JTextField(cols);
        styleField(f);
        f.setCaretColor(ACCENT_CYAN);
        return f;
    }

    public static JPasswordField passwordInput(int cols) {
        JPasswordField f = new JPasswordField(cols);
        styleField(f);
        f.setCaretColor(ACCENT_CYAN);
        return f;
    }

    public static void styleField(JComponent f) {
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setFont(FONT_LABEL);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    public static <E> JComboBox<E> comboBox(E[] items) {
        JComboBox<E> cb = new JComboBox<>(items);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_LABEL);
        return cb;
    }

    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setBackground(ACCENT_CYAN);
        b.setForeground(BG_DARK);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_LABEL);
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(BORDER_SUBTLE, 1));
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton dangerBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_LABEL);
        b.setBackground(new Color(0x3A, 0x10, 0x18));
        b.setForeground(DANGER);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 80), 1));
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", ACCENT_CYAN);
        UIManager.put("PasswordField.background", BG_INPUT);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", ACCENT_CYAN);
        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", BG_SELECTED);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("Viewport.background", BG_DARK);
        UIManager.put("Table.background", BG_TABLE_ROW);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", BG_SELECTED);
        UIManager.put("Table.selectionForeground", ACCENT_CYAN);
        UIManager.put("Table.gridColor", BORDER_SUBTLE);
        UIManager.put("TableHeader.background", BG_CARD);
        UIManager.put("TableHeader.foreground", ACCENT_CYAN);
        UIManager.put("TableHeader.font", FONT_HEADING);
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background", BG_CARD);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.background", BG_PANEL);
        UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.selected", BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", ACCENT_CYAN);
        UIManager.put("Separator.foreground", BORDER_SUBTLE);
    }
}
