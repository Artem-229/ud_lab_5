package esports;

import esports.gui.MainWindow;
import esports.gui.Theme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Apply dark theme defaults before any UI is created
        Theme.applyGlobalDefaults();

        SwingUtilities.invokeLater(() -> {
            MainWindow win = new MainWindow();
            win.setVisible(true);
        });
    }
}
