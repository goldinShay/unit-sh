package ui.gui.guiDeviceControl;

import devices.Device;
import devices.actions.LiveDeviceState;
import devices.actions.WashingMachineAction;
import devices.actions.actionSimulator.WasherCycleSimulator;
import devices.actions.advancedActions.WashActionsLGTwin;
import devices.state.WasherState;
import ui.gui.managers.GuiStateManager;
import utils.Theme;
import ui.gui.PageNavigator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class WashingMachineControlPage extends JPanel {
    private final Device device;
    private JLabel statusLabel;
    private JLabel autoOpLabel;
    private JLabel programLabel;
    private JLabel timerLabel;
    private JLabel spinLabel;
    private JLabel tempLabel;
    private JPanel headerPanel;

    private WashingMachineAction currentAction;

    public WashingMachineControlPage(Device device, int pageNumber) {
        this.device = device;
        this.currentAction = WashingMachineAction.ECO_WASH;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND_DARK);

        headerPanel = createHeader(WasherState.getCurrentProgram());
        add(headerPanel, BorderLayout.NORTH);
        add(createControlPanel(), BorderLayout.CENTER);
        add(createFooter(pageNumber), BorderLayout.SOUTH);
    }


    private JPanel createHeader(WashingMachineAction selectedProgram) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.setBorder(BorderFactory.createTitledBorder(
                null,
                "Washing Machine Control",
                0, 0,
                new Font("Monospaced", Font.PLAIN, 14),
                Color.LIGHT_GRAY
        ));

        // === Top Row: Device Name (left), Status (right) ===
        JLabel nameLabel = new JLabel("🧺 " + device.getName(), JLabel.LEFT);
        nameLabel.setForeground(Color.LIGHT_GRAY);
        nameLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        statusLabel = new JLabel(getStatusText(), JLabel.RIGHT);
        statusLabel.setForeground(getStatusColor());
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(Color.BLACK);
        topRow.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 8));
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(statusLabel, BorderLayout.EAST);

        // === Second Row: AutoOp status (right-aligned) ===
        autoOpLabel = new JLabel(getAutoOpText(), JLabel.RIGHT);
        autoOpLabel.setForeground(getAutoOpColor());
        autoOpLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        JPanel autoOpRow = new JPanel(new BorderLayout());
        autoOpRow.setBackground(Color.BLACK);
        autoOpRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        autoOpRow.add(autoOpLabel, BorderLayout.EAST);

        // === Center Display: Program, Temp, Spin, Time ===
        String programName = WasherState.getCurrentProgram().name().replace("_", " ");
        int duration = WasherState.getRemainingTime();
        int temp = WasherState.getWaterTemperature();
        int spin = WasherState.getSpinSpeed();

        programLabel = new JLabel("Program: " + programName);
        tempLabel = new JLabel("Temp: " + temp + "°C");
        spinLabel = new JLabel("Spin: " + spin + " RPM");
        timerLabel = new JLabel("Time Left: " + duration + " min");

        JLabel[] labels = { programLabel, tempLabel, spinLabel, timerLabel };
        for (JLabel label : labels) {
            label.setForeground(Color.CYAN);
            label.setFont(new Font("Monospaced", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        }
        programLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 36, 2));
        row1.setBackground(Color.BLACK);
        row1.add(programLabel);
        row1.add(tempLabel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 2));
        row2.setBackground(Color.BLACK);
        row2.add(spinLabel);
        row2.add(timerLabel);

        JPanel centerDisplay = new JPanel();
        centerDisplay.setLayout(new BoxLayout(centerDisplay, BoxLayout.Y_AXIS));
        centerDisplay.setBackground(Color.BLACK);
        centerDisplay.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        centerDisplay.add(row1);
        centerDisplay.add(Box.createVerticalStrut(4)); // spacing between rows
        centerDisplay.add(row2);

        // === Stack all rows vertically ===
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.BLACK);
        content.add(topRow);
        content.add(autoOpRow);
        content.add(centerDisplay);

        header.add(content, BorderLayout.CENTER);
        return header;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND_DARK);

        // Display area
        JPanel displayPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        displayPanel.setPreferredSize(new Dimension(0, 100));
        displayPanel.setBackground(Theme.BACKGROUND_DARK);

        JPanel bottomRow = new JPanel(new GridLayout(1, 2));
        bottomRow.setBackground(Theme.BACKGROUND_DARK);

        // Create two horizontal button rows
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.setBackground(Theme.BACKGROUND_DARK);

        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.setBackground(Theme.BACKGROUND_DARK);

        // Add buttons to rows
        row1.add(createButton("ON"));
        row1.add(createButton("OFF"));
        row1.add(createButton("START", timerLabel, statusLabel));
        row1.add(createButton("CHANGE PROGRAM"));

        row2.add(createButton("AutoOp"));
        row2.add(createButton("SCHEDULE"));
        row2.add(createButton("TEST"));
        row2.add(createButton("UPDATE DEVICE"));

        // Stack rows vertically
        JPanel buttonGrid = new JPanel();
        buttonGrid.setLayout(new BoxLayout(buttonGrid, BoxLayout.Y_AXIS));
        buttonGrid.setBackground(Theme.BACKGROUND_DARK);
        buttonGrid.add(row1);
        buttonGrid.add(row2);

        // Wrapper to push button grid to lower third
        JPanel buttonGridWrapper = new JPanel();
        buttonGridWrapper.setLayout(new BoxLayout(buttonGridWrapper, BoxLayout.Y_AXIS));
        buttonGridWrapper.setBackground(Theme.BACKGROUND_DARK);
        buttonGridWrapper.add(Box.createVerticalGlue());     // Push down
        buttonGridWrapper.add(buttonGrid);                   // Add grid
        buttonGridWrapper.add(Box.createVerticalStrut(180));  // Optional spacing below

        panel.add(buttonGridWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JButton createButton(String label) {
        return createButton(label, null, null);
    }

    private JButton createButton(String label, JLabel countdownLabel, JLabel statusLabel) {
        JButton button = new JButton(label);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(Theme.BUTTON_GRAY);
        button.setForeground(Color.DARK_GRAY);
        button.setPreferredSize(new Dimension(175, 40));
        button.setMaximumSize(new Dimension(175, 40));
        button.setMinimumSize(new Dimension(175, 40));
        button.setToolTipText("Trigger " + label + " action");

        button.addActionListener(e -> {
            switch (label) {
                case "ON":
                    device.turnOn();
                    LiveDeviceState.turnOn(device);
                    refreshHeader();
                    break;
                case "OFF":
                    device.turnOff();
                    LiveDeviceState.turnOff(device);
                    refreshHeader();
                    break;
                case "TEST":
                    if (!LiveDeviceState.isOn(device)) {
                        device.testDevice();
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Device is ON. Turn it OFF before testing.",
                                "Test Blocked",
                                JOptionPane.WARNING_MESSAGE);
                    }
                    break;
                case "START":
                    if (!LiveDeviceState.isOn(device)) {
                        JOptionPane.showMessageDialog(null,
                                "Device must be ON to start a cycle.",
                                "Start Blocked",
                                JOptionPane.WARNING_MESSAGE);
                        break;
                    }

                    WashingMachineAction selectedAction = WasherState.getCurrentProgram(); // ✅ dynamic
                    WasherCycleSimulator.simulateCycle(selectedAction, remainingSeconds -> {
                        WasherState.setRemainingTime(remainingSeconds);
                        SwingUtilities.invokeLater(() -> {
                            int minutes = remainingSeconds / 60;
                            int seconds = remainingSeconds % 60;
                            timerLabel.setText(String.format("Time Left: %02d:%02d", minutes, seconds));
                        });
                    });
                    break;

                case "CHANGE PROGRAM":
                    // ✅ Get available programs for the current washer model
                    List<WashingMachineAction> availablePrograms = WashActionsLGTwin.getAvailablePrograms();

                    // ✅ Convert to array for dropdown
                    WashingMachineAction[] options = availablePrograms.toArray(new WashingMachineAction[0]);

                    // ✅ Show selection dialog
                    WashingMachineAction selectedProgram = (WashingMachineAction) JOptionPane.showInputDialog(
                            null,
                            "Select a washing program:",
                            "Change Program",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            options,
                            WasherState.getCurrentProgram()
                    );

                    // ✅ If user made a selection, apply it and refresh display
                    if (selectedProgram != null) {
                        WasherState.applyProgram(selectedProgram); // Update all program attributes
                        updateHeaderDisplay();                     // Refresh GUI labels

                        // ✅ Reset timer label to show initial duration
                        if (timerLabel != null) {
                            int initialTime = WasherState.getRemainingTime();
                            timerLabel.setText("Time Left: " + initialTime + " min");
                        }
                    }
                    break;

            }

            if (statusLabel != null) {
                statusLabel.setText(getStatusText());
                statusLabel.setForeground(getStatusColor());
            }
        });

        return button;
    }
    private void updateHeaderDisplay() {
        String programName = WasherState.getCurrentProgram().name().replace("_", " ");
        int temp = WasherState.getWaterTemperature();
        int spin = WasherState.getSpinSpeed();
        int duration = WasherState.getRemainingTime();

        if (programLabel != null) programLabel.setText("Program: " + programName);
        if (tempLabel != null) tempLabel.setText("Temp: " + temp + "°C");
        if (timerLabel != null) timerLabel.setText("Time Left: " + duration + " min");
        if (spinLabel != null) spinLabel.setText("Spin: " + spin + " RPM");
        // If you added spinLabel, update it here too
    }


    private JPanel createFooter(int pageId) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.BLACK);

        JLabel pageLabel = new JLabel("Page " + pageId);
        pageLabel.setForeground(Color.GREEN);
        pageLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        JButton backBtn = new JButton("←");
        backBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        backBtn.setToolTipText("Go back");

        backBtn.addActionListener(e -> {
            GuiStateManager.refreshDeviceMatrix(); // ✅ Refresh button states
            PageNavigator.goToPage(400);           // ✅ Go back to utility device selection
        });

        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        homeBtn.addActionListener(e -> PageNavigator.goToPage(50));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        navPanel.setBackground(Color.BLACK);
        navPanel.add(backBtn);
        navPanel.add(homeBtn);

        footer.add(pageLabel, BorderLayout.WEST);
        footer.add(navPanel, BorderLayout.EAST);

        return footer;
    }

    private String getStatusText() {
        return LiveDeviceState.isOn(device) ? "🟢 ON" : "🔴 OFF";
    }

    private Color getStatusColor() {
        return LiveDeviceState.isOn(device) ? Color.GREEN : Color.RED;
    }

    private String getAutoOpText() {
        return device.isAutomationEnabled() ? "⚙️ AutoOp ON" : "⚙️ AutoOp OFF";
    }

    private Color getAutoOpColor() {
        return device.isAutomationEnabled() ? Color.GREEN : Color.RED;
    }
    private void refreshHeader() {
        if (statusLabel != null) {
            statusLabel.setText(getStatusText());
            statusLabel.setForeground(getStatusColor());
        }
        if (autoOpLabel != null) {
            autoOpLabel.setText(getAutoOpText());
            autoOpLabel.setForeground(getAutoOpColor());
        }
    }


}