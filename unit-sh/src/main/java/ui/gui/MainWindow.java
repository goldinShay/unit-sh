package ui.gui;

import devices.Device;
import devices.DeviceType;
import storage.xlc.XlWorkbookUtils;
import ui.gui.managers.ButtonMapManager;
import ui.gui.managers.GuiStateManager;
import ui.gui.pages.*;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private static MainWindow instance;
    private static JPanel pageContainer; // ✅ Promoted to class-level field

    public MainWindow() {
        instance = this;

        setTitle("PhoenixSH");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // === Main Content Area with CardLayout ===
        pageContainer = new JPanel(new CardLayout());
        getContentPane().add(pageContainer);

        // === Navigator Setup ===
        PageNavigator.initialize(pageContainer);

        // === Static Page Registration ===
        PageNavigator.registerPage(50, new WelcomeMenuPage());
        PageNavigator.registerPage(100, new DeviceSettingsPage());
        PageNavigator.registerPage(110, new AddDeviceMenuPage());
        PageNavigator.registerPage(112, new ChooseDevice4UpdatePage());
        PageNavigator.registerPage(200, new ChooseDeviceCtrlPage());
        PageNavigator.registerPage(300, new AutoOpControlPage());

        // === Initial Page ===
        SwingUtilities.invokeLater(() -> {
            GuiStateManager.refreshDeviceMatrix(); // ✅ Populate button map

            // ✅ Rebuild page 120 with updated matrix
            JComponent lightPage = ButtonMapManager.renderPageForTypes(
                    new DeviceType[]{DeviceType.LIGHT}, 0, 120
            );
            PageNavigator.registerPage(120, lightPage);

            // ✅ Rebuild page 441 with washing machine matrix
            JComponent washerPage = ButtonMapManager.renderPageForTypes(
                    new DeviceType[]{DeviceType.WASHING_MACHINE}, 0, 441
            );
            PageNavigator.registerPage(441, washerPage);

            PageNavigator.goToPage(120); // ✅ Show the correct page
        });

    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            if (!XlWorkbookUtils.ensureFileExists()) {
                return;
            }

            System.out.println("✅ MainWindow: Launch triggered.");
            if (instance == null) {
                instance = new MainWindow();
            }

            instance.setVisible(true); // ✅ Show window

            PageNavigator.goToPage(50); // ✅ Start on welcome page

            GuiStateManager.refreshDeviceMatrix(); // ✅ Populate button map

            JComponent lightPage = ButtonMapManager.renderPageForTypes(
                    new DeviceType[]{DeviceType.LIGHT}, 0, 120
            );
            PageNavigator.registerPage(120, lightPage); // ✅ Matrix ready when needed
        });
    }

    // ✅ Expose the root panel for PageNavigator
    public static JPanel getRootPanel() {
        if (pageContainer == null) {
            throw new IllegalStateException("MainWindow not initialized yet.");
        }
        return pageContainer;
    }

    public static void initialize() {
        if (instance == null) {
            instance = new MainWindow(); // ✅ Construct the window
        }
    }

}