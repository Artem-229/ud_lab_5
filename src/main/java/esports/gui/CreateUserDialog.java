package esports.gui;

import javax.swing.*;
import java.awt.*;

public class CreateUserDialog extends JDialog {

    private boolean confirmed = false;
    private String username, password, role;

    public CreateUserDialog(Frame parent) {
        super(parent, "Создать пользователя БД", true);
        buildUI();
        pack();
        setMinimumSize(new Dimension(400, getHeight()));
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        header.setBackground(Theme.BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        JLabel lbl = new JLabel("Новый пользователь базы данных");
        lbl.setFont(Theme.FONT_HEADING);
        lbl.setForeground(Theme.ACCENT_GREEN);
        header.add(lbl);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        JTextField userField   = Theme.input(18);
        JPasswordField passField  = Theme.passwordInput(18);
        JPasswordField pass2Field = Theme.passwordInput(18);
        JComboBox<String> roleCombo = Theme.comboBox(new String[]{"guest", "admin"});

        Object[][] rows = {
            {"Логин:",            userField},
            {"Пароль:",           passField},
            {"Подтверждение:",    pass2Field},
            {"Роль:",             roleCombo}
        };

        for (int i = 0; i < rows.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0;
            form.add(Theme.label((String) rows[i][0]), g);
            g.gridx = 1; g.weightx = 1;
            form.add((Component) rows[i][1], g);
        }

        root.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Theme.BG_CARD);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));

        JButton cancelBtn = Theme.ghostBtn("Отмена");
        JButton createBtn = Theme.primaryBtn("  Создать  ");
        createBtn.setBackground(Theme.ACCENT_GREEN);
        createBtn.setForeground(Theme.BG_DARK);

        footer.add(cancelBtn);
        footer.add(createBtn);
        root.add(footer, BorderLayout.SOUTH);

        createBtn.addActionListener(e -> {
            String u  = userField.getText().trim();
            String p  = new String(passField.getPassword());
            String p2 = new String(pass2Field.getPassword());
            String r  = (String) roleCombo.getSelectedItem();

            if (u.length() < 3) {
                JOptionPane.showMessageDialog(this, "Логин должен быть не менее 3 символов.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (p.length() < 4) {
                JOptionPane.showMessageDialog(this, "Пароль должен быть не менее 4 символов.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!p.equals(p2)) {
                JOptionPane.showMessageDialog(this, "Пароли не совпадают.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            username = u; password = p; role = r;
            confirmed = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(createBtn);

        setContentPane(root);
    }

    public boolean isConfirmed() { return confirmed; }
    public String getUsername()  { return username; }
    public String getPassword()  { return password; }
    public String getRole()      { return role; }
}
