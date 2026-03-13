package esports.gui;

import esports.model.MatchRecord;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class EditMatchDialog extends JDialog {

    public static final String[] FORMATS = {"Bo1", "Bo2", "Bo3", "Bo5"};
    public static final String[] GAMES = {
        "Counter-Strike 2", "Dota 2", "League of Legends", "Valorant",
        "Overwatch 2", "Rainbow Six Siege", "StarCraft II", "Rocket League",
        "PUBG", "Apex Legends", "Call of Duty", "Fortnite"
    };

    private boolean confirmed = false;
    private MatchRecord result;

    private final JTextField dateField     = Theme.input(12);
    private final JTextField durationField = Theme.input(8);
    private final JTextField tournField    = Theme.input(24);
    private final JComboBox<String> gameCombo   = Theme.comboBox(GAMES);
    private final JComboBox<String> formatCombo = Theme.comboBox(FORMATS);
    private final JTextField team1Field  = Theme.input(20);
    private final JTextField team2Field  = Theme.input(20);
    private final JTextField winnerField = Theme.input(20);

    public EditMatchDialog(Frame parent, String title, MatchRecord existing) {
        super(parent, title, true);
        buildUI(existing);
        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void buildUI(MatchRecord rec) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL);

        // ── Header bar ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        header.setBackground(Theme.BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));
        JLabel titleLbl = new JLabel(getTitle());
        titleLbl.setFont(Theme.FONT_HEADING);
        titleLbl.setForeground(Theme.ACCENT_CYAN);
        header.add(titleLbl);
        root.add(header, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        Object[][] rows = {
            {"Дата (YYYY-MM-DD):",  dateField},
            {"Длительность (HH:MM:SS):", durationField},
            {"Турнир:",             tournField},
            {"Игра:",               gameCombo},
            {"Формат:",             formatCombo},
            {"Команда 1:",          team1Field},
            {"Команда 2:",          team2Field},
            {"Победитель:",         winnerField},
        };

        for (int i = 0; i < rows.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0;
            JLabel lbl = Theme.label((String) rows[i][0]);
            form.add(lbl, g);
            g.gridx = 1; g.weightx = 1;
            form.add((Component) rows[i][1], g);
        }

        // hint
        JLabel hint = new JLabel("<html><i>Победитель должен совпадать с Командой 1 или Командой 2</i></html>");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_SECONDARY);
        g.gridx = 0; g.gridy = rows.length; g.gridwidth = 2;
        form.add(hint, g);

        root.add(form, BorderLayout.CENTER);

        // ── Footer buttons ────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Theme.BG_CARD);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));

        JButton cancelBtn = Theme.ghostBtn("Отмена");
        JButton saveBtn   = Theme.primaryBtn("  Сохранить  ");

        footer.add(cancelBtn);
        footer.add(saveBtn);
        root.add(footer, BorderLayout.SOUTH);

        // ── Pre-fill ──────────────────────────────────────────────────────────
        if (rec != null) {
            dateField.setText(rec.getMatchDate());
            durationField.setText(rec.getMatchDuration());
            tournField.setText(rec.getTournament());
            gameCombo.setSelectedItem(rec.getGame());
            formatCombo.setSelectedItem(rec.getFormat());
            team1Field.setText(rec.getTeam1());
            team2Field.setText(rec.getTeam2());
            winnerField.setText(rec.getWinner());
        }

        saveBtn.addActionListener(e -> {
            try {
                validateAndBuild(rec);
                confirmed = true;
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка валидации", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(saveBtn);

        setContentPane(root);
    }

    private void validateAndBuild(MatchRecord existing) {
        String date     = dateField.getText().trim();
        String duration = durationField.getText().trim();
        String tourn    = tournField.getText().trim();
        String game     = (String) gameCombo.getSelectedItem();
        String format   = (String) formatCombo.getSelectedItem();
        String team1    = team1Field.getText().trim();
        String team2    = team2Field.getText().trim();
        String winner   = winnerField.getText().trim();

        if (date.isEmpty()) throw new IllegalArgumentException("Дата обязательна.");
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Дата должна быть в формате YYYY-MM-DD.");
        if (duration.isEmpty()) throw new IllegalArgumentException("Длительность обязательна.");
        if (!duration.matches("\\d+:[0-5]\\d:[0-5]\\d")) throw new IllegalArgumentException("Длительность должна быть HH:MM:SS (например 32:21:05).");
        if (tourn.isEmpty()) throw new IllegalArgumentException("Название турнира обязательно.");
        if (team1.isEmpty()) throw new IllegalArgumentException("Команда 1 обязательна.");
        if (team2.isEmpty()) throw new IllegalArgumentException("Команда 2 обязательна.");
        if (team1.matches("\\d+")) throw new IllegalArgumentException("Название Команды 1 не должно быть числом.");
        if (team2.matches("\\d+")) throw new IllegalArgumentException("Название Команды 2 не должно быть числом.");
        if (!winner.isEmpty() && !winner.equals(team1) && !winner.equals(team2))
            throw new IllegalArgumentException("Победитель должен совпадать с Командой 1 или Командой 2.");

        result = new MatchRecord(
            existing != null ? existing.getId() : 0,
            date, duration, tourn, game, format, team1, team2, winner
        );
    }

    public boolean isConfirmed() { return confirmed; }
    public MatchRecord getResult() { return result; }
}
