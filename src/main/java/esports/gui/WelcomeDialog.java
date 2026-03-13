package esports.gui;

import esports.db.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

public class WelcomeDialog extends JDialog {

    private static final String DEFAULT_OPTION = "↳ Создать esports_matches";

    private final DatabaseManager db;
    private boolean success = false;

    private JComboBox<String> loginDbCombo;
    private JComboBox<String> regDbCombo;

    public WelcomeDialog(DatabaseManager db) {
        super((Frame) null, "Esports Match Tracker", true);
        this.db = db;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG_PANEL);

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Theme.BG_CARD);
        brand.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        brand.setPreferredSize(new Dimension(420, 72));

        JLabel icon = new JLabel("◈");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 30));
        icon.setForeground(Theme.ACCENT_CYAN);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Esports Match Tracker");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Database Management System");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_SECONDARY);
        titleBox.add(title);
        titleBox.add(sub);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 16, 0, 12);
        brand.add(icon, gc);
        gc.insets = new Insets(0, 0, 0, 16);
        brand.add(titleBox, gc);

        root.add(brand, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.TEXT_SECONDARY);
        tabs.addTab("  Вход  ", buildLoginPanel());
        tabs.addTab("  Регистрация  ", buildRegisterPanel());
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildLoginPanel() {
        JPanel p = makeFormPanel();

        JTextField userField = Theme.input(18);
        JPasswordField passField = Theme.passwordInput(18);
        loginDbCombo = buildDbCombo();
        styleCombo(loginDbCombo);

        JButton refreshBtn = smallBtn("↻");
        refreshBtn.setToolTipText("Обновить список БД");

        JLabel status = statusLabel();

        JButton loginBtn = Theme.primaryBtn("  Войти  ");

        JLabel hint = new JLabel("<html><center><i>admin / adminpass · guest / guestpass</i></center></html>", SwingConstants.CENTER);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_SECONDARY);

        GridBagConstraints g = gbc();

        JPanel dbRow = new JPanel(new BorderLayout(4, 0));
        dbRow.setOpaque(false);
        dbRow.add(loginDbCombo, BorderLayout.CENTER);
        dbRow.add(refreshBtn, BorderLayout.EAST);

        row(p, g, 0, "База данных:", dbRow);
        row(p, g, 1, "Логин:", userField);
        row(p, g, 2, "Пароль:", passField);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        p.add(status, g);
        g.gridy = 4;
        p.add(loginBtn, g);
        g.gridy = 5;
        p.add(hint, g);

        getRootPane().setDefaultButton(loginBtn);

        refreshBtn.addActionListener(e -> { refreshCombo(loginDbCombo); refreshCombo(regDbCombo); });

        loginBtn.addActionListener(e -> {
            String user  = userField.getText().trim();
            String pass  = new String(passField.getPassword());
            String dbSel = (String) loginDbCombo.getSelectedItem();

            if (user.isEmpty() || pass.isEmpty()) { errStatus(status, "Введите логин и пароль."); return; }
            if (dbSel == null) { errStatus(status, "Выберите базу данных."); return; }

            if (DEFAULT_OPTION.equals(dbSel)) {
                infoStatus(status, "Создаём базу данных...");
                loginBtn.setEnabled(false);
                String fu = user, fp = pass;
                new Thread(() -> {
                    try {
                        db.createDatabase(DatabaseManager.DEFAULT_DB);
                        SwingUtilities.invokeLater(() -> {
                            refreshCombo(loginDbCombo); refreshCombo(regDbCombo);
                            loginDbCombo.setSelectedItem(DatabaseManager.DEFAULT_DB);
                            loginBtn.setEnabled(true);
                            okStatus(status, "БД создана! Подключаемся...");
                            doLogin(DatabaseManager.DEFAULT_DB, fu, fp, status, loginBtn);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            errStatus(status, cleanError(ex.getMessage()));
                            loginBtn.setEnabled(true);
                        });
                    }
                }).start();
                return;
            }
            doLogin(dbSel, user, pass, status, loginBtn);
        });

        return p;
    }

    private void doLogin(String dbName, String user, String pass, JLabel status, JButton btn) {
        try {
            db.login(dbName, user, pass);
            success = true;
            dispose();
        } catch (Exception ex) {
            errStatus(status, cleanError(ex.getMessage()));
            if (btn != null) btn.setEnabled(true);
        }
    }

    private JPanel buildRegisterPanel() {
        JPanel p = makeFormPanel();

        JTextField userField     = Theme.input(18);
        JPasswordField passField  = Theme.passwordInput(18);
        JPasswordField pass2Field = Theme.passwordInput(18);
        JComboBox<String> roleCombo = Theme.comboBox(new String[]{"guest", "admin"});
        styleCombo(roleCombo);
        JPasswordField codeField  = Theme.passwordInput(18);
        JLabel codeLabel = Theme.label("Код администратора:");
        regDbCombo = buildDbCombo();
        styleCombo(regDbCombo);

        codeField.setEnabled(false);
        codeLabel.setEnabled(false);
        roleCombo.addActionListener(e -> {
            boolean isAdmin = "admin".equals(roleCombo.getSelectedItem());
            codeField.setEnabled(isAdmin);
            codeLabel.setEnabled(isAdmin);
        });

        GridBagConstraints g = gbc();
        row(p, g, 0, "База данных:", regDbCombo);
        row(p, g, 1, "Логин:", userField);
        row(p, g, 2, "Пароль (8 симв.):", passField);
        row(p, g, 3, "Подтверждение:", pass2Field);
        row(p, g, 4, "Роль:", roleCombo);

        g.gridx = 0; g.gridy = 5; g.gridwidth = 1; g.weightx = 0;
        p.add(codeLabel, g);
        g.gridx = 1; g.weightx = 1;
        p.add(codeField, g);

        JLabel status = statusLabel();
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        p.add(status, g);

        JButton regBtn = Theme.primaryBtn("  Зарегистрироваться  ");
        regBtn.setBackground(Theme.ACCENT_GREEN);
        regBtn.setForeground(Theme.BG_DARK);
        g.gridy = 7;
        p.add(regBtn, g);

        regBtn.addActionListener(e -> {
            String dbSel = (String) regDbCombo.getSelectedItem();
            String user  = userField.getText().trim();
            String pass  = new String(passField.getPassword());
            String pass2 = new String(pass2Field.getPassword());
            String role  = (String) roleCombo.getSelectedItem();
            String code  = new String(codeField.getPassword());

            if (DEFAULT_OPTION.equals(dbSel)) { errStatus(status, "Сначала создайте БД через вкладку Вход."); return; }
            if (dbSel == null) { errStatus(status, "Выберите базу данных."); return; }
            if (user.length() < 3) { errStatus(status, "Логин минимум 3 символа."); return; }
            if (pass.length() != 8) { errStatus(status, "Пароль должен быть ровно 8 символов."); return; }
            if (!pass.equals(pass2)) { errStatus(status, "Пароли не совпадают."); return; }

            try {
                db.register(dbSel, user, pass, role, code);
                okStatus(status, "✓ Готово! Перейдите на вкладку Вход.");
            } catch (Exception ex) {
                errStatus(status, cleanError(ex.getMessage()));
            }
        });

        return p;
    }


    private JPanel makeFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        return p;
    }

    private JComboBox<String> buildDbCombo() {
        JComboBox<String> combo = new JComboBox<>();
        refreshCombo(combo);
        return combo;
    }

    private void refreshCombo(JComboBox<String> combo) {
        if (combo == null) return;
        String current = (String) combo.getSelectedItem();
        combo.removeAllItems();
        List<String> dbs = db.listDatabases();
        for (String d : dbs) combo.addItem(d);
        if (!dbs.contains(DatabaseManager.DEFAULT_DB)) combo.addItem(DEFAULT_OPTION);
        if (current != null) combo.setSelectedItem(current);
        if (dbs.contains(DatabaseManager.DEFAULT_DB)) combo.setSelectedItem(DatabaseManager.DEFAULT_DB);
        else if (combo.getModel().getSize() > 0) combo.setSelectedIndex(0);
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFont(Theme.FONT_LABEL);
    }

    private JButton smallBtn(String text) {
        JButton b = Theme.ghostBtn(text);
        b.setMargin(new Insets(2, 6, 2, 6));
        return b;
    }

    private JLabel statusLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.DANGER);
        return l;
    }

    private void errStatus(JLabel l, String msg) { l.setForeground(Theme.DANGER); l.setText(msg); }
    private void okStatus(JLabel l, String msg)   { l.setForeground(Theme.ACCENT_GREEN); l.setText(msg); }
    private void infoStatus(JLabel l, String msg) { l.setForeground(Theme.ACCENT_CYAN); l.setText(msg); }

    private void row(JPanel p, GridBagConstraints g, int row, String lbl, JComponent field) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        p.add(Theme.label(lbl), g);
        g.gridx = 1; g.weightx = 1;
        p.add(field, g);
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        return g;
    }

    private String cleanError(String msg) {
        if (msg == null) return "Неизвестная ошибка.";
        if (msg.contains("ERROR:")) {
            int idx = msg.indexOf("ERROR:") + 6;
            String tail = msg.substring(idx).trim();
            if (tail.contains("\n")) tail = tail.substring(0, tail.indexOf('\n')).trim();
            return tail;
        }
        return msg;
    }

    public boolean isSuccess() { return success; }
}
