package storage;

import devices.Device;
import devices.SmartLight;
import devices.actions.DeviceAction;
import sensors.Sensor;
import storage.xlc.XlDeviceManager;
import ui.gui.managers.GuiStateManager;

import java.util.*;

public class DeviceStorage {

    // 🔧 Static storage for all devices
    private static final Map<String, Device> devices = new HashMap<>();

    // 🔧 Track active threads (if any)
    private static final List<Thread> deviceThreads = new ArrayList<>();
    private static final DevicePersistence persistence = new ExcelDevicePersistence();

    // 🧪 Clear in-memory state for clean test execution
    public static void clear() {
        devices.clear();
        deviceThreads.clear();
    }

    // 🔄 Initialize by loading from Excel (usually called on startup)
    public static void initialize() {
        devices.clear(); // optional: only if you want a fresh start

        List<Device> loadedDevices = XlDeviceManager.loadDevicesFromExcel();

        if (loadedDevices == null) {
            System.err.println("🚨 Failed to load devices: Excel loader returned null.");
            return;
        }

        if (loadedDevices.isEmpty()) {
            System.err.println("⚠️ No devices found. Excel may lack entries or filters are too strict.");
        }

        // Devices are already added to memory and registered with GUI inside loadDevicesFromExcel()
        // So we just log and refresh here

        for (Device device : loadedDevices) {
            if (device == null || device.getId() == null) {
                System.err.println("⛔ Skipping invalid device: " + device);
                continue;
            }

//            System.out.println("🧠 Automation state for " + device.getId() + " → " + device.isAutomationEnabled());
        }

        System.out.println("📦 Successfully loaded " + devices.size() + " devices into memory.");
        ExcelDevicePersistence.setInitFlag(false);

        // 🔁 Refresh GUI again just to be safe
        GuiStateManager.refreshDeviceMatrix();
        // 🔁 Refresh GUI again just to be safe
        syncGuiWithMemory();
    }
    public static void syncGuiWithMemory() {
        System.out.println("🔄 Syncing GUI with in-memory devices...");
        for (Device device : devices.values()) {
            GuiStateManager.registerNewDevice(device);
        }
        GuiStateManager.refreshDeviceMatrix();
    }


    public static Device getDevice(String id) {
        return devices.get(id);
    }

    // 🚪 Expose all devices
    public static Map<String, Device> getDevices() {
        return devices;
    }

    public static SmartLight getSmartLight(String id) {
        Device device = devices.get(id);
        return (device instanceof SmartLight) ? (SmartLight) device : null;
    }

    public static List<Device> getDeviceList() {
        return new ArrayList<>(devices.values());
    }

    // ⚡ Turn a device on/off + update Excel
    public static void updateDeviceState(String deviceId, String action) {
        Device device = devices.get(deviceId);
        if (device == null) {
            System.err.println("❌ Device not found for ID: " + deviceId);
            return;
        }

        boolean shouldTurnOn = action.equalsIgnoreCase(DeviceAction.ON.name());
        toggleDevicePower(device, shouldTurnOn);
        updateDeviceExcelState(device);
    }

    private static void toggleDevicePower(Device device, boolean turnOn) {
        if (turnOn) {
            device.turnOn();
        } else {
            device.turnOff();
        }

        device.setState(turnOn ? DeviceAction.ON.name() : DeviceAction.OFF.name());
        devices.put(device.getId(), device);

        System.out.printf("🔄 Power toggled for '%s' → %s%n", device.getName(), turnOn ? "ON" : "OFF");
    }

    public static void updateDeviceExcelState(Device device) {
        boolean success = persistence.updateDevice(device);

        if (!success) {
            System.err.println("🚨 Excel update failed for device: " + device.getId());
        }
    }

    public static List<Thread> getDeviceThreads() {
        return deviceThreads;
    }

    public static void add(Device device) {
        if (device != null && device.getId() != null) {
            devices.put(device.getId(), device);
        } else {
            System.err.println("⛔ Invalid device passed to add(): " + device);
        }
    }

    public static boolean addDevice(Device device) {
        if (device == null || device.getId() == null || device.getId().isEmpty()) {
            System.err.println("⛔ Cannot add device: invalid or missing ID.");
            return false;
        }

        devices.put(device.getId(), device);
        System.out.printf("✅ Device added → ID: %s | Name: %s | Type: %s%n",
                device.getId(), device.getName(), device.getType());
        return true;
    }

    public static void reloadFromExcel() {
        List<Device> loadedDevices = XlCreator.loadDevicesFromExcel();

        if (loadedDevices == null) {
            System.err.println("⚠️ Excel reload failed: returned null.");
            return;
        }

        if (loadedDevices.isEmpty()) {
            System.err.println("⚠️ Excel reload returned an empty list. No devices to import.");
            return;
        }

        Map<String, Device> tempMap = new HashMap<>();
        for (Device device : loadedDevices) {
            if (device == null || device.getId() == null || device.getType() == null) {
                System.err.println("⛔ Invalid device entry → " + device);
                continue;
            }

            tempMap.put(device.getId(), device);
        }

        if (tempMap.isEmpty()) {
            System.err.println("⚠️ Reload aborted: No valid devices found in Excel.");
            return;
        }

        devices.clear();
        devices.putAll(tempMap);
    }

    public static void updateMemoryAfterExcelWrite(Device device) {
        boolean saved = XlCreator.delegateDeviceUpdate(device);

        if (saved) {
            reloadFromExcel();
            System.out.println("🔁 Memory updated with device: " + device.getId());
        } else {
            System.err.println("❌ Failed to update Excel with device: " + device.getId());
        }
    }

    public static Device getLinkedDevice(String sensorId) {
        for (Device device : devices.values()) {
            Sensor linkedSensor = device.getLinkedSensor();
            if (linkedSensor != null && linkedSensor.getSensorId().equalsIgnoreCase(sensorId)) {
                return device;
            }
        }
        return null;
    }

    // 🧘 Calm pass-through to DeviceAction
    public static List<DeviceAction> getActionsForDevice(String deviceId) {
        return DeviceAction.getActionsForDevice(deviceId);
    }

    public static Device getDeviceById(String id) {
        return devices.get(id);
    }

    // Optional: expose the map if needed
    public static Map<String, Device> getAllDevices() {
        return devices;
    }
}