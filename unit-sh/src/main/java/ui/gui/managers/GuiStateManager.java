package ui.gui.managers;

import devices.Device;
import devices.DeviceType;
import devices.actions.LiveDeviceState;
import storage.DeviceStorage;
import ui.gui.PageNavigator;
import ui.gui.devicesListPages.ChooseLightsUpdatePage;
import ui.gui.guiDeviceControl.HeaterControlPage;
import ui.gui.guiDeviceControl.LightControlPage;
import utils.Log;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class GuiStateManager {

    // ✅ Internal map to track registered GUI buttons
    private static final Map<String, JButton> deviceButtonMap = new HashMap<>();

    public static void registerNewDevice(Device device) {
        if (device == null) {
            Log.warn("🚫 Tried to register null device.");
            return;
        }

        String id = device.getId();
        Log.debug("🧩 Attempting to register device: " + id);

        if (deviceButtonMap.containsKey(id)) {
//            Log.warn("⚠️ Device already registered in GUI: " + id);
            return;
        }

        // 🧠 Add to memory if not already present
        DeviceStorage.getDevices().putIfAbsent(id, device);

        // 🔋 Sync live state
        if (device.isOn()) {
            LiveDeviceState.turnOn(device);
        } else {
            LiveDeviceState.turnOff(device);
        }

        // 🖼️ Create and store GUI button
        JButton button = createDeviceButton(device); // You’ll need to implement this
        deviceButtonMap.put(id, button);

//        Log.info("✅ " + device.getName() + " (" + id + ") added to GUI button map successfully!(GUIststeM)");
    }

    public static void refreshDeviceMatrix() {
        Log.debug("🔁 Refreshing device matrix...");
//        System.out.println("🧩 Matrix refreshed with devices: " + DeviceStorage.getDevices().keySet());

        // ✅ Rebuild the matrix page with current memory
        ChooseLightsUpdatePage lightPage = ChooseLightsUpdatePage.loadFresh(
                0, 120, DeviceType.LIGHT, DeviceType.SMART_LIGHT
        );

        // ✅ Re-register page 120 with the new matrix
        PageNavigator.registerPage(120, lightPage);

        // ✅ Switch to page 120 if it's the current page
        if (PageNavigator.getCurrentPageId() == 120) {
            PageNavigator.goToPage(120);

            // ✅ Force GUI refresh
            JComponent panel = PageNavigator.getPage(120);
            if (panel != null) {
                panel.revalidate();
                panel.repaint();
//                System.out.println("🧪 Revalidated and repainted page 120");
            }
        }
    }

    public static void refreshDeviceControlPage(Device device) {
        // Generate a stable page ID based on device ID
        int numericId = Integer.parseInt(device.getId().replaceAll("[^0-9]", ""));
        int pageId = 200 + numericId;

        // Dynamically choose the correct control page
        JComponent controlPage = switch (device.getType()) {
            case SMART_LIGHT, LIGHT -> new LightControlPage(device, pageId);
            case THERMOSTAT -> new HeaterControlPage(device, pageId);
            // Add more types here as needed
            default -> {
                System.out.println("⚠️ No control page defined for: " + device.getType());
                yield new JPanel(); // fallback empty panel
            }
        };

        // Register and navigate
        if (!PageNavigator.isPageRegistered(pageId)) {
            PageNavigator.registerPage(pageId, controlPage);
        }

        PageNavigator.goToPage(pageId);
    }

    public static boolean isDeviceRegistered(String id) {
        return deviceButtonMap.containsKey(id);
    }

    public static void refreshGuiFromMemory() {
        Log.debug("🔄 Rebuilding GUI from memory...");

        for (Device device : DeviceStorage.getDevices().values()) {
            if (!isDeviceRegistered(device.getId())) {
                registerNewDevice(device);
            }
        }

        refreshDeviceMatrix();
    }

    // 🔧 Stub for button creation — replace with your actual logic
    private static JButton createDeviceButton(Device device) {
        JButton button = new JButton(device.getName());
        button.addActionListener(e -> refreshDeviceControlPage(device));
        return button;
    }
    public static Map<String, JButton> getDeviceButtonMap() {

        return deviceButtonMap;
    }
    public static JButton getButtonForDevice(String deviceId) {
        JButton button = deviceButtonMap.get(deviceId);
        System.out.println("🔍 getButtonForDevice: " + deviceId + " → " + (button != null ? "✅ Found" : "❌ Missing"));
        return button;
    }
}