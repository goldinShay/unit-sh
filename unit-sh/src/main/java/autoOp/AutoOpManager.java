package autoOp;

import devices.Device;
import devices.SmartLight;
import sensors.Sensor;
import autoOp.AutoOpExcelReader.AutoOpRecord;
import storage.DeviceStorage;
import storage.SensorStorage;
import storage.XlCreator;
import controllers.SmartLightController;
import utils.Log;


import java.util.List;
import java.util.Map;

public class AutoOpManager {

    // 📥 Load AutoOp links from Excel using external reader
    public static void loadMappingsFromExcel() {
        List<AutoOpRecord> links = AutoOpExcelReader.readLinks();
        if (links.isEmpty()) {
            System.out.println("⚠️ No mappings found in Excel.");
            return;
        }

        Map<String, Device> devices = DeviceStorage.getDevices();
        Map<String, Sensor> sensors = SensorStorage.getSensors();

        for (AutoOpRecord record : links) {
            String deviceId = record.linkedDeviceId();
            String sensorId = record.sensorId();
            double autoOn = record.autoOn();
            double autoOff = record.autoOff();

            Device device = devices.get(deviceId);
            Sensor sensor = sensors.get(sensorId);

            if (device == null || sensor == null) {
                System.out.printf("⚠️ Missing %s in memory → Device: %s | Sensor: %s%n",
                        (device == null ? "device" : "sensor"), deviceId, sensorId);
                continue;
            }

            // Delegate restoration logic based on device type
            if (device instanceof SmartLight light) {
                SmartLightController.restoreLink(light, sensor, autoOn, autoOff);
            } else {
                restoreGenericLink(device, sensor, autoOn, autoOff);
            }

            System.out.printf("🔗 Restored link → %s → %s | AutoOp: %b | Thresholds: ON=%.1f OFF=%.1f%n",
                    device.getId(), sensor.getSensorId(), device.isAutomationEnabled(),
                    device.getAutoThreshold(), device.getAutoThreshold());
        }
    }

    private static void restoreGenericLink(Device device, Sensor sensor, double autoOn, double autoOff) {
        device.setAutoThreshold(autoOn, true);
        device.setAutoThreshold(autoOff, true); // Optional: differentiate ON/OFF if supported
        device.setAutomationEnabled(true);
        device.setAutomationSensorId(sensor.getSensorId());
        sensor.linkLinkedDevice(device);
    }


    // 🔗 Save a new AutoOp link to Excel
    public static boolean persistLink(Device linkedDevice, Sensor master) {
        boolean written;

        if (linkedDevice instanceof SmartLight) {
            written = SmartLightController.linkSensor((SmartLight) linkedDevice, master);
        } else {
            written = XlCreator.appendToAutoOpManager(linkedDevice, master);
        }
        if (!written) {
            System.out.println("⚠️ Failed to persist AutoOp link between " + linkedDevice.getName() + " and " + master.getSensorName());
            return false;
        }
        System.out.println("💾 AutoOp link saved to Sens_Ctrl sheet.");
        return true;
    }

    // ❌ Remove a broken AutoOp link from Excel
    public static void unlink(Device linkedDevice) {
        XlCreator.removeFromSensCtrl(linkedDevice.getId());
        System.out.println("🧹 AutoOp link removed from Excel.");
    }

    // 🧠 Refresh runtime links after Excel rehydrate
    public static void restoreMemoryLinks() {
        System.out.println("🔁 Restoring 69 AutoOp memory mappings...");
        List<AutoOpRecord> records = AutoOpExcelReader.readLinks();

        if (records.isEmpty()) {
            System.out.println("⚠️ No mappings found in Excel.");
            return;
        }

        Map<String, Device> devices = DeviceStorage.getDevices();
        Map<String, Sensor> sensors = SensorStorage.getSensors();

        for (AutoOpRecord record : records) {
            String deviceId = record.linkedDeviceId();
            String sensorId = record.sensorId();
            double autoOn = record.autoOn();

            Device device = devices.get(deviceId);
            Sensor sensor = sensors.get(sensorId);

            if (device == null || sensor == null) {
                System.out.printf("⚠️ Missing device or sensor → Device: %s | Sensor: %s%n", deviceId, sensorId);
                continue;
            }

            device.setAutomationEnabled(true);
            device.setAutomationSensorId(sensorId);
            device.setAutoThreshold(autoOn, true);

            sensor.linkLinkedDevice(device);       // Sensor knows device
            device.setLinkedSensor(sensor);        // Device knows sensor ✅

            System.out.printf("✅ Restored link → %s → %s | Threshold: %.2f%n",
                    deviceId, sensorId, autoOn);
        }
    }


    // 🪄 Manual trigger if you want to rescan sensor thresholds
    public static void reevaluateAllSensors() {
//        System.out.println("🔁 Reevaluating all sensors...");

        for (Sensor sensor : SensorStorage.getSensors().values()) {
            Log.debug("🔍 Reevaluating Sensor ID: " + sensor.getSensorId() +
                    " | Ref: " + System.identityHashCode(sensor) +
                    " | Linked Devices: " + sensor.getLinkedDevice().size());

//            System.out.printf("🔍 Reevaluating Sensor ID: %s | Ref: %s%n",
//                    sensor.getSensorId(), System.identityHashCode(sensor));

            double value = sensor.getCurrentReading();
//            int slaveCount = sensor.getLinkedDevicesCount();

//            System.out.printf("📡 Sensor %s | Reading: %.1f | Linked Devices: %d%n",
//                    sensor.getSensorId(), value, slaveCount);

            for (Device linkedDevice : sensor.getLinkedDevice()) {
                // ✅ Use general device registry instead of SmartLightController
                Device liveDevice = DeviceStorage.getDevice(linkedDevice.getId());
                if (liveDevice == null) {
                    System.out.printf("⚠️ Device %s not found in DeviceStorage. Skipping.%n", linkedDevice.getId());
                    continue;
                }

//                System.out.printf("   🔍 %s → AutoOp: %b | ON: %.1f | OFF: %.1f | Ref: %s%n",
//                        liveDevice.getId(), liveDevice.isAutomationEnabled(),
//                        liveDevice.getAutoThreshold(), liveDevice.getAutoThreshold(),
//                        System.identityHashCode(liveDevice));

                sensor.notifyLinkedDevices(value); // ✅ Use live reference
            }
        }
    }


}