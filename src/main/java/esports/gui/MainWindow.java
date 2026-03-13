package esports.gui;

import esports.db.DatabaseManager;
import esports.model.MatchRecord;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class MainWindow extends JFrame {

    private final DatabaseManager db = new DatabaseManager();
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusBar;
    private JLabel roleLabel;

    private JButton addBtn, editBtn, deleteBtn, deleteByTournBtn, clearBtn;
    private JButton createUserBtn, dropDbBtn, importJsonBtn;
    private JTextField searchField;

    public MainWindow() {
        super("Esports Match Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        buildUI();
        showWelcome();
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // ── Top bar ────────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.BG_CARD);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        topBar.setPreferredSize(new Dimension(1200, 46));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        brandBox.setOpaque(false);
        JLabel brandIcon = new JLabel("◈");
        brandIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brandIcon.setForeground(Theme.ACCENT_CYAN);
        JLabel brandTitle = new JLabel("Esports Match Tracker");
        brandTitle.setFont(Theme.FONT_HEADING);
        brandTitle.setForeground(Theme.TEXT_PRIMARY);
        brandBox.add(brandIcon);
        brandBox.add(brandTitle);
        topBar.add(brandBox, BorderLayout.WEST);

        roleLabel = new JLabel("Не подключено", SwingConstants.RIGHT);
        roleLabel.setFont(Theme.FONT_SMALL);
        roleLabel.setForeground(Theme.TEXT_SECONDARY);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        topBar.add(roleLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ── Table ──────────────────────────────────────────────────────────────
        String[] cols = {"ID", "Дата", "Длит.", "Турнир", "Игра", "Формат", "Команда 1", "Команда 2", "Победитель"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setBackground(Theme.BG_TABLE_ROW);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_LABEL);
        table.setSelectionBackground(Theme.BG_SELECTED);
        table.setSelectionForeground(Theme.ACCENT_CYAN);
        table.setGridColor(Theme.BORDER_SUBTLE);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // alternating rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? Theme.BG_TABLE_ROW : Theme.BG_TABLE_ALT);
                    setForeground(Theme.TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.BG_CARD);
        header.setForeground(Theme.ACCENT_CYAN);
        header.setFont(Theme.FONT_HEADING);
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setReorderingAllowed(false);

        int[] widths = {40, 90, 72, 150, 130, 64, 120, 120, 120};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(Theme.BG_DARK);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        add(scroll, BorderLayout.CENTER);

        // ── Status bar ──────────────────────────────────────────────────────────
        statusBar = new JLabel(" ");
        statusBar.setFont(Theme.FONT_SMALL);
        statusBar.setForeground(Theme.TEXT_SECONDARY);
        statusBar.setBackground(Theme.BG_CARD);
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        add(statusBar, BorderLayout.SOUTH);

        add(buildSidebar(), BorderLayout.WEST);
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 8, 12, 8)));
        panel.setPreferredSize(new Dimension(195, 600));

        // Search section
        addSectionHeader(panel, "🔍  Поиск по турниру");
        searchField = new JTextField();
        Theme.styleField(searchField);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(4));
        panel.add(sideBtn("Найти", e -> doSearch()));
        panel.add(Box.createVerticalStrut(2));
        panel.add(sideBtn("Показать все", e -> doRefresh()));

        addDivider(panel);
        addSectionHeader(panel, "⚡  Управление матчами");

        addBtn           = sideBtn("Добавить матч",     e -> doAdd());
        editBtn          = sideBtn("Редактировать",     e -> doEdit());
        deleteBtn        = sideBtn("Удалить выбранный", e -> doDeleteById());
        deleteByTournBtn = sideBtn("Удалить по турниру...", e -> doDeleteByTournament());
        clearBtn         = Theme.dangerBtn("Очистить все записи");
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        clearBtn.setHorizontalAlignment(SwingConstants.LEFT);
        clearBtn.addActionListener(e -> doClear());

        for (JButton b : new JButton[]{addBtn, editBtn, deleteBtn, deleteByTournBtn}) {
            panel.add(b); panel.add(Box.createVerticalStrut(3));
        }
        panel.add(Box.createVerticalStrut(3));
        panel.add(clearBtn);

        addDivider(panel);
        addSectionHeader(panel, "⚙  Администрирование");

        dropDbBtn     = Theme.dangerBtn("Удалить БД");
        dropDbBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        dropDbBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dropDbBtn.addActionListener(e -> doDropDatabase());

        createUserBtn = sideBtn("Создать пользователя", e -> doCreateUser());
        JButton newDbBtn      = sideBtn("Создать новую БД",  e -> doCreateNewDatabase());
        JButton switchDbBtn   = sideBtn("Сменить БД",        e -> switchDatabase());

        JButton logoutBtn = sideBtn("⏻  Выйти", e -> doLogout());
        logoutBtn.setForeground(Theme.DANGER);

        for (JButton b : new JButton[]{dropDbBtn, createUserBtn, newDbBtn, switchDbBtn, logoutBtn}) {
            panel.add(b); panel.add(Box.createVerticalStrut(3));
        }

        addDivider(panel);
        addSectionHeader(panel, "📁  Импорт / Экспорт");

        JButton exportBtn = sideBtn("Экспорт в Excel", e -> doExportExcel());
        importJsonBtn = sideBtn("Импорт из JSON", e -> doImportJson());
        panel.add(exportBtn); panel.add(Box.createVerticalStrut(3));
        panel.add(importJsonBtn);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void addSectionHeader(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_HEADING);
        lbl.setForeground(Theme.ACCENT_CYAN);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        panel.add(lbl);
    }

    private void addDivider(JPanel panel) {
        panel.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER_SUBTLE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(8));
    }

    private JButton sideBtn(String text, java.awt.event.ActionListener al) {
        JButton b = Theme.ghostBtn(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.addActionListener(al);
        return b;
    }

    private void applyRolePermissions() {
        setAllButtonsEnabled(true);
        boolean isAdmin = "admin".equals(db.getCurrentRole());
        String badge = isAdmin ? "● ADMIN" : "● GUEST";
        Color badgeColor = isAdmin ? Theme.ACCENT_GREEN : Theme.ACCENT_BLUE;
        roleLabel.setText("<html><font color='#" + colorHex(badgeColor) + "'>" + badge + "</font>  "
                + db.getCurrentUser() + "  <font color='#454e6b'>|</font>  " + db.getCurrentDb() + "</html>");
        setTitle("Esports Match Tracker — " + db.getCurrentDb());
    }

    private String colorHex(Color c) {
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void showWelcome() {
        if (db.isConnected()) db.disconnect();
        WelcomeDialog dlg = new WelcomeDialog(db);
        dlg.setVisible(true);
        if (!dlg.isSuccess()) {
            if (!db.isConnected()) {
                setStatus("Нет подключения.");
                setAllButtonsEnabled(false);
                roleLabel.setText("Не подключено");
            }
            return;
        }
        applyRolePermissions();
        doRefresh();
        setStatus("Подключен как " + db.getCurrentUser() + " (" + db.getCurrentRole() + ") → [" + db.getCurrentDb() + "]");
    }

    private void setAllButtonsEnabled(boolean en) {
        for (JButton b : new JButton[]{addBtn, editBtn, deleteBtn, deleteByTournBtn, clearBtn,
                dropDbBtn, createUserBtn, importJsonBtn})
            b.setEnabled(en);
    }

    private void doRefresh() {
        if (!db.isConnected()) return;
        try {
            List<MatchRecord> records = db.getAll();
            populateTable(records);
            setStatus("Загружено записей: " + records.size());
        } catch (Exception e) { showError("Ошибка загрузки", e); }
    }

    private void populateTable(List<MatchRecord> records) {
        tableModel.setRowCount(0);
        for (MatchRecord r : records) {
            tableModel.addRow(new Object[]{
                r.getId(), r.getMatchDate(), r.getMatchDuration(), r.getTournament(),
                r.getGame(), r.getFormat(), r.getTeam1(), r.getTeam2(), r.getWinner()
            });
        }
    }

    private MatchRecord getSelectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        try { return db.getById(id); } catch (Exception e) { return null; }
    }

    private void doAdd() {
        EditMatchDialog dlg = new EditMatchDialog(this, "Добавить матч", null);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        try { db.addMatchRecord(dlg.getResult()); doRefresh(); setStatus("Матч добавлен."); }
        catch (Exception e) { showError("Ошибка добавления", e); }
    }

    private void doEdit() {
        MatchRecord rec = getSelectedRecord();
        if (rec == null) { JOptionPane.showMessageDialog(this, "Выберите запись для редактирования."); return; }
        EditMatchDialog dlg = new EditMatchDialog(this, "Редактировать матч #" + rec.getId(), rec);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        try { db.updateMatch(dlg.getResult()); doRefresh(); setStatus("Матч #" + rec.getId() + " обновлён."); }
        catch (Exception e) { showError("Ошибка обновления", e); }
    }

    private void doDeleteById() {
        MatchRecord rec = getSelectedRecord();
        if (rec == null) { JOptionPane.showMessageDialog(this, "Выберите запись для удаления."); return; }
        if (JOptionPane.showConfirmDialog(this,
                "Удалить матч #" + rec.getId() + " (" + rec.getTeam1() + " vs " + rec.getTeam2() + ")?",
                "Подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try { db.deleteById(rec.getId()); doRefresh(); setStatus("Матч #" + rec.getId() + " удалён."); }
        catch (Exception e) { showError("Ошибка удаления", e); }
    }

    private void doDeleteByTournament() {
        String tourn = JOptionPane.showInputDialog(this, "Введите точное название турнира:");
        if (tourn == null || tourn.trim().isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this,
                "Удалить ВСЕ матчи турнира \"" + tourn.trim() + "\"?",
                "Подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try { db.deleteByTournament(tourn.trim()); doRefresh(); setStatus("Удалены матчи турнира: " + tourn.trim()); }
        catch (Exception e) { showError("Ошибка удаления", e); }
    }

    private void doClear() {
        if (JOptionPane.showConfirmDialog(this, "Удалить ВСЕ записи? Это действие необратимо.",
                "Подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try { db.clearTable(); doRefresh(); setStatus("Таблица очищена."); }
        catch (Exception e) { showError("Ошибка очистки", e); }
    }

    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { doRefresh(); return; }
        try {
            List<MatchRecord> results = db.searchByTournament(query);
            populateTable(results);
            setStatus("Найдено записей: " + results.size() + "  (турнир содержит: \"" + query + "\")");
        } catch (Exception e) { showError("Ошибка поиска", e); }
    }

    private void doCreateUser() {
        CreateUserDialog dlg = new CreateUserDialog(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        try {
            db.createUser(dlg.getUsername(), dlg.getPassword(), dlg.getRole());
            JOptionPane.showMessageDialog(this,
                    "Пользователь «" + dlg.getUsername() + "» создан с ролью «" + dlg.getRole() + "».",
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Пользователь создан: " + dlg.getUsername());
        } catch (Exception e) { showError("Ошибка создания пользователя", e); }
    }

    private void doLogout() {
        db.disconnect();
        tableModel.setRowCount(0);
        setAllButtonsEnabled(false);
        roleLabel.setText("Не подключено");
        setStatus("Выход выполнен.");
        setVisible(false);
        showWelcome();
        setVisible(true);
    }

    private void doDropDatabase() {
        if (JOptionPane.showConfirmDialog(this,
                "Удалить базу данных «" + db.getCurrentDb() + "»? ВСЕ ДАННЫЕ БУДУТ ПОТЕРЯНЫ.",
                "Подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) return;
        String dbName = db.getCurrentDb();
        try {
            db.dropDatabase(dbName);
            JOptionPane.showMessageDialog(this, "БД «" + dbName + "» удалена.", "Готово", JOptionPane.INFORMATION_MESSAGE);
            doLogout();
        } catch (Exception e) { showError("Ошибка удаления БД", e); }
    }

    private void doCreateNewDatabase() {
        String name = JOptionPane.showInputDialog(this, "Имя новой базы данных:", "Создать БД", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        try {
            db.createDatabase(name.trim());
            JOptionPane.showMessageDialog(this,
                    "БД «" + name.trim() + "» создана.\nАккаунты по умолчанию: admin/adminpass, guest/guestpass",
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
            setStatus("БД «" + name.trim() + "» создана.");
        } catch (Exception e) { showError("Ошибка создания БД", e); }
    }

    private void doExportExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Экспорт в Excel");
        fc.setSelectedFile(new java.io.File("esports_matches.xlsx"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.endsWith(".xlsx")) path += ".xlsx";
        try {
            List<MatchRecord> records = db.getAll();
            if (records.isEmpty()) { JOptionPane.showMessageDialog(this, "Нет записей для экспорта."); return; }
            db.exportExcel(records, path);
            JOptionPane.showMessageDialog(this, "Экспортировано " + records.size() + " записей:\n" + path,
                    "Экспорт завершён", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Экспортировано: " + records.size() + " записей.");
        } catch (Exception e) { showError("Ошибка экспорта", e); }
    }

    private void doImportJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Импорт из JSON");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files (*.json)", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = fc.getSelectedFile().getAbsolutePath();
        try {
            int added = db.importJson(path);
            doRefresh();
            JOptionPane.showMessageDialog(this, "Импортировано " + added + " записей.",
                    "Импорт завершён", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Импортировано: " + added + " записей.");
        } catch (DatabaseManager.PartialImportException e) {
            doRefresh();
            JOptionPane.showMessageDialog(this,
                    "Частичный импорт: добавлено " + e.added + ", пропущено " + e.skipped + ".\n\nОшибки:" + e.details,
                    "Частичный импорт", JOptionPane.WARNING_MESSAGE);
            setStatus("Частичный импорт: добавлено " + e.added + ", пропущено " + e.skipped + ".");
        } catch (Exception e) { showError("Ошибка импорта", e); }
    }

    private void switchDatabase() {
        if (!db.isConnected()) { showWelcome(); return; }
        List<String> dbs = db.listDatabases();
        if (dbs.isEmpty()) { JOptionPane.showMessageDialog(this, "Базы данных не найдены."); return; }
        JComboBox<String> combo = new JComboBox<>(dbs.toArray(new String[0]));
        combo.setSelectedItem(db.getCurrentDb());
        int res = JOptionPane.showConfirmDialog(this, combo, "Выбрать базу данных", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        String dbName = (String) combo.getSelectedItem();
        if (dbName == null || dbName.equals(db.getCurrentDb())) return;
        showWelcome();
    }

    private void setStatus(String msg) { statusBar.setText(msg); }

    private void showError(String title, Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("ERROR:")) {
            int idx = msg.indexOf("ERROR:") + 6;
            msg = msg.substring(idx).trim();
            if (msg.contains("\n")) msg = msg.substring(0, msg.indexOf('\n')).trim();
        }
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
        setStatus("Ошибка: " + msg);
    }
}
