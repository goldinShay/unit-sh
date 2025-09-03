package autoOp;

import devices.Device;
import devices.SmartLight;
import devices.actions.DeviceAction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sensors.Sensor;
import storage.DeviceStorage;
import storage.SensorStorage;
import storage.XlCreator;
import storage.xlc.XlSmartLightManager;
import utils.Log;
import storage.xlc.XlWorkbookUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static storage.xlc.XlWorkbookUtils.createSheetWithHeaders;
import static storage.xlc.XlWorkbookUtils.getFilePath;

public final class AutoOpLinker {
    private static final Scanner defaultScanner = new Scanner(System.in);
    private static final String SENS_CTRL = "Sens_Ctrl";


    private AutoOpLinker() {
        // Utility class – prevent instantiation
    }

    public static void promptAndLink(Device device, Scanner scanner) {
        Map<String, Sensor> sensors = SensorStorage.getSensors();
        if (sensors == null || sensors.isEmpty()) {
            System.out.println("⚠️ No sensors available to link.");
            return;
        }

        System.out.println("\n📡 Available Sensors:");
        sensors.values().forEach(sensor -> {
            try {
                System.out.printf("→ %s | %s | Current: %.1f%n",
                        sensor.getSensorId(), sensor.getSensorName(), sensor.getCurrentReading());
            } catch (Exception e) {
                System.out.printf("→ %s | %s | Current: ⚠️ Error (%s)%n",
                        sensor.getSensorId(), sensor.getSensorName(), e.getMessage());
            }
        });

        System.out.print("Enter sensor ID to link with device '" + device.getName() + "': ");
        String sensorId = scanner.nextLine().trim();

        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            System.out.println("❌ Invalid sensor ID. Aborting link.");
            return;
        }

        boolean success = linkDeviceToSensor(device, sensor);
        if (success) {
            System.out.printf("🔗 '%s' successfully linked to sensor '%s'.%n",
                    device.getName(), sensor.getSensorName());
        } else {
            System.out.println("⚠️ Failed to complete device-sensor link.");
        }
    }

    public static boolean linkDeviceToSensor(Device device, Sensor sensor) {
        if (device == null || sensor == null) return false;
        if (sensor.isAlreadyLinkedTo(device)) {
            System.out.println("🔄 Device already linked to sensor.");
            return true; // Technically already linked
        }

        // ✅ Step 1: Link device and enable automation
        sensor.linkLinkedDevice(device);
        device.setAutomationSensorId(sensor.getSensorId());
        device.setAutomationEnabled(true);
        device.enableAutoMode();

        device.supportedActions = List.of(DeviceAction.ON, DeviceAction.OFF, DeviceAction.STATUS);


        boolean updated = true;

        // ✅ Step 2: Update SmartLight sheet if applicable
        if (device instanceof SmartLight sl) {
            try {
                Workbook workbook = XlWorkbookUtils.getWorkbook(XlWorkbookUtils.getFilePath().toString());
                Sheet controlSheet = workbook.getSheet("Smart_Light_Control");
                if (controlSheet == null) {
                    workbook.createSheet("Smart_Light_Control");
                }
                updated &= XlSmartLightManager.updateSmartLight(workbook, sl);
            } catch (IOException e) {
                e.printStackTrace();
                updated = false;
            }
        }

        // ✅ Step 3: Update Devices sheet and persist link
        updated &= XlCreator.delegateDeviceUpdate(device);
        updated &= AutoOpManager.persistLink(device, sensor);
        updated &= SensorLinkManager(device, sensor);

        // ✅ Step 4: Now reevaluate sensors after link is complete
        AutoOpManager.reevaluateAllSensors();

        return updated;
    }
    public static boolean SensorLinkManager(Device slave, Sensor master) {
        if (slave == null || master == null) {
            Log.warn("⚠️ Skipping sheet append due to null reference: "
                    + (slave == null ? "slave=null " : "")
                    + (master == null ? "master=null" : ""));
            return false;
        }

        try (FileInputStream fis = new FileInputStream(getFilePath().toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(SENS_CTRL);
            if (sheet == null) {
                sheet = workbook.createSheet(SENS_CTRL);
                String[] headers = {
                        "SLAVE_ID", "SLAVE_NAME", "THRESHOLD",
                        "CRNT_VAL", "SENSOR_NAME", "SENSOR_ID", "UPDATED_TS"
                };
                createSheetWithHeaders(workbook, SENS_CTRL, headers);
            }

            removeRowIfExists(sheet, slave.getId());
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);

            row.createCell(0).setCellValue(slave.getId());
            row.createCell(1).setCellValue(slave.getName());
            row.createCell(2).setCellValue(slave.getAutoThreshold());
            row.createCell(3).setCellValue(master.getCurrentValue());
            row.createCell(4).setCellValue(master.getSensorName());
            row.createCell(5).setCellValue(master.getSensorId());
            row.createCell(6).setCellValue(master.getUpdatedTimestamp());

            try (FileOutputStream fos = new FileOutputStream(getFilePath().toFile())) {
                workbook.write(fos);
            }

            if (!master.getLinkedDevice().contains(slave)) {
                Log.warn("❌ SensorLinkManager aborted: device " + slave.getId() +
                        " is not listed as slave under sensor " + master.getSensorId());
                return false;
            }

// continue with the sheet logic...
            removeRowIfExists(sheet, slave.getId());
// [sheet updates]
            Log.debug("✅ Linked " + slave.getName() + " → " + master.getSensorName());
            return true;

        } catch (IOException e) {
            Log.error("❌ Failed to append to Sens_Ctrl: " + e.getMessage());
            return false;
        }
    }
    private static void removeRowIfExists(Sheet sheet, String deviceId) {
        for (Row row : sheet) {
            if (row.getCell(0) != null && deviceId.equals(row.getCell(0).getStringCellValue())) {
                sheet.removeRow(row);
                break;
            }
        }
    }
    public static void relinkLinkedDevicesToSensors() {
        Map<String, Sensor> sensors = SensorStorage.getSensors();
        Map<String, Device> devices = DeviceStorage.getDevices();

        if (sensors == null || devices == null) {
            Log.warn("⚠️ Cannot relink: sensors or devices map is null.");
            return;
        }

        for (Device device : devices.values()) {
            String sensorId = device.getAutomationSensorId();
            if (sensorId == null || sensorId.isEmpty()) continue;

            Sensor sensor = sensors.get(sensorId);
            if (sensor == null) continue;

            if (!sensor.isAlreadyLinkedTo(device)) {
                linkDeviceToSensor(device, sensor);
            }
        }

        Log.info("🔁 Relinking complete.");
    }


}
