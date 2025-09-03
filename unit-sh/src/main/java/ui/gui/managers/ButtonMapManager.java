package ui.gui.managers;

import devices.Device;
import devices.DeviceType;
import devices.actions.LiveDeviceState;
import storage.DeviceStorage;
import ui.gui.PageNavigator;
import ui.gui.guiDeviceControl.HeaterControlPage;
import ui.gui.guiDeviceControl.WashingMachineControlPage;
import utils.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ButtonMapManager {
    private static final int PAGE_SIZE = 10;

    // 🧭 Dynamic navigation
    private static void goToPage(List<DeviceType> types, int pageIndex, int basePageId) {
        int pageId = basePageId + pageIndex;
        JComponent fullPage = PageFactory.loadPage(pageIndex, basePageId, types.toArray(new DeviceType[0]));

        PageNavigator.registerPage(pageId, fullPage);
        PageNavigator.goToPage(pageId);

        Component pageComponent = PageNavigator.getPage(pageId);
        if (pageComponent != null) {
            pageComponent.revalidate();
            pageComponent.repaint();
//            System.out.println("🧪 Revalidated and repainted page " + pageId);
        } else {
            System.out.println("⚠️ Page " + pageId + " not found.");
        }
    }

    // 🧠 Filter by type
    private static List<Device> getDevicesByTypes(DeviceType... types) {
        Set<DeviceType> typeSet = new HashSet<>(Arrays.asList(types));
        return DeviceStorage.getDevices().values().stream()
                .filter(d -> typeSet.contains(d.getType()))
                .sorted(Comparator.comparing(Device::getId))
                .collect(Collectors.toList());
    }

    // 🔧 Page renderer
    public static JComponent renderPageForTypes(DeviceType[] types, int pageIndex, int basePageId) {
        List<Device> allFiltered = getDevicesByTypes(types);
        int maxPage = Math.max(0, (int) Math.ceil((double) allFiltered.size() / PAGE_SIZE) - 1);
        int clampedPageIndex = Math.min(pageIndex, maxPage);

        List<Device> devices = allFiltered.stream()
                .skip((long) clampedPageIndex * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();

        return renderGridFromDevices(devices, clampedPageIndex, basePageId, Arrays.asList(types));
    }

    // 🧱 Grid renderer
    private static JScrollPane renderGridFromDevices(List<Device> devices, int pageIndex, int basePageId, List<DeviceType> types) {
        JPanel gridPanel = new JPanel(new GridLayout(4, 3, 20, 20));
        gridPanel.setBackground(Theme.BACKGROUND_DARK);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));

        for (int i = 0; i < 9; i++) {
            gridPanel.add(i < devices.size()
                    ? createNameButton(devices.get(i), i)
                    : createPlaceholder());
        }

        gridPanel.add(createNavButton("←", pageIndex > 0, () -> goToPage(types, pageIndex - 1, basePageId)));

        gridPanel.add(devices.size() >= 10
                ? createNameButton(devices.get(9), 9)
                : createPlaceholder());

        boolean hasMore = devices.size() == PAGE_SIZE;
        gridPanel.add(createNavButton("→", hasMore, () -> goToPage(types, pageIndex + 1, basePageId)));

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setPreferredSize(new Dimension(800, 360));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    // 🧱 Button builders
    public static JButton createNameButton(Device device, int position) {
        JButton button = new JButton();
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(160, 60));

        if (device != null) {
            button.setText("<html><center>" + device.getName() + "</center></html>");
            button.setBackground(getDeviceColor(device));

            button.addActionListener(e -> {
                System.out.println("🔧 Selected for update: " + device.getName() + " [" + device.getId() + "]");

                switch (device.getType()) {
                    case SMART_LIGHT, LIGHT -> GuiStateManager.refreshDeviceControlPage(device);

                    case THERMOSTAT -> {
                        int pageId = 421 + position; // You can refine this logic if needed
                        JPanel heaterPage = new HeaterControlPage(device, pageId);
                        PageNavigator.registerPage(pageId, heaterPage);
                        PageNavigator.goToPage(pageId);
                    }
                    case WASHING_MACHINE -> {
                        int pageId = 441 + position;
                        JPanel washerPage = new WashingMachineControlPage(device, pageId);
                        PageNavigator.registerPage(pageId, washerPage);
                        PageNavigator.goToPage(pageId);
                    }
//
//                    case DRYER -> {
//                        int pageId = 441 + position;
//                        JPanel dryerPage = new DryerControlPage(device, pageId);
//                        PageNavigator.registerPage(pageId, dryerPage);
//                        PageNavigator.goToPage(pageId);
//                    }

                    default -> System.out.println("🛠️ No control page linked for: " + device.getType());
                }
            });
        } else {
            button.setText("N/A");
            button.setEnabled(false);
            button.setBackground(Theme.DARK_ORANGE);
        }

        return button;
    }


    public static Color getDeviceColor(Device device) {
        boolean isOn = LiveDeviceState.isOn(device);
        return switch (device.getType()) {
            case SMART_LIGHT -> isOn ? Theme.SMART_ON_GREEN : Theme.SMART_OFF_GREEN;
            case LIGHT -> isOn ? Theme.BASIC_ON_GREEN : Theme.BASIC_OFF_GREEN;
            case THERMOSTAT -> isOn ? Theme.PURPLE_ON_HEATER : Theme.PURPLE_OFF_HEATER;
            case WASHING_MACHINE -> isOn ? Theme.BLUE_ON_WASHER : Theme.BLUE_OFF_WASHER;
            case DRYER -> isOn ? Theme.BLUE_ON_DRYER : Theme.BLUE_OFF_DRYER;
            default -> Theme.DARK_ORANGE;
        };
    }

    private static JButton createNavButton(String label, boolean enabled, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Monospaced", Font.PLAIN, 14));
        btn.setEnabled(enabled);
        btn.setPreferredSize(new Dimension(160, 40));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private static JButton createPlaceholder() {
        JButton placeholder = new JButton("N/A");
        placeholder.setEnabled(false);
        placeholder.setBackground(Theme.DARK_ORANGE);
        placeholder.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_GRAY));
        placeholder.setPreferredSize(new Dimension(160, 40));
        return placeholder;
    }
}
