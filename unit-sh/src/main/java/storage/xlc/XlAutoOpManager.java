package storage.xlc;

import autoOp.AutoOpLinker;
import devices.Device;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sensors.Sensor;
import utils.Log;
import storage.DeviceStorage;
import storage.SensorStorage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static storage.xlc.XlWorkbookUtils.*;

public class XlAutoOpManager {

    private static final String SHEET_SENSE = "Sens_Ctrl";

    public static void removeSensorLink(String slaveId) {
        try {
            updateWorkbook((workbook, tasks, devices, sensors, sensCtrl, smartLightControl) -> {
                for (Row row : sensCtrl) {
                    if (row.getRowNum() == 0) continue;
                    if (getCellValue(row, 0).equalsIgnoreCase(slaveId)) {
                        sensCtrl.removeRow(row);
                        Log.debug("🗑️ Removed link for " + slaveId);
                        break;
                    }
                }
            });
        } catch (IOException e) {
            Log.error("❌ Failed to remove link for " + slaveId + ": " + e.getMessage());
        }
    }
    public boolean updateAutoOpThresholds(String deviceId, double newOn, double ignoredOff) {
        // Implement the logic to update thresholds
        // For now, let’s return true as a placeholder
        return true;
    }
    public static void restoreSensorLinks() {
        try (FileInputStream fis = new FileInputStream(getFilePath().toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(SHEET_SENSE);
            if (sheet == null) {
                Log.warn("⚠️ No Sens_Ctrl sheet found during restoration.");
                return;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String linkedDeviceId = getCellValue(row, 0); // SLAVE_ID
                String sensorId       = getCellValue(row, 5); // SENSOR_ID

                Device device = DeviceStorage.getDeviceById(linkedDeviceId);
                Sensor sensor = SensorStorage.getSensorById(sensorId);

                if (device != null && sensor != null) {
                    device.setAutomationSensorId(sensor.getSensorId());
                    device.setAutomationEnabled(true);
                    device.enableAutoMode();

//                    Log.debug("🧬 Sensor '" + sensor.getSensorId() + "' instance hash: " + System.identityHashCode(sensor));
//                    Log.info("💡 Before link, sensor '" + sensor.getSensorId() + "' had " + sensor.getLinkedDevice().size() + " linked devices");
//
                    sensor.linkLinkedDevice(device);
//
//                    Log.info("🔗 After link, sensor '" + sensor.getSensorId() + "' has " + sensor.getLinkedDevice().size() + " linked devices");
//                    Log.info("✅ Restored AutoOp link → " + device.getName() + " ← " + sensor.getSensorName());
                } else {
                    Log.warn("🔍 Could not restore link for Device ID '" + linkedDeviceId + "' and Sensor ID '" + sensorId + "'");
                }
            }

        } catch (IOException e) {
            Log.error("❌ Failed to restore links from Sens_Ctrl: " + e.getMessage());
        }
    }

    public static boolean updateSensorValueInSheet(Sensor sensor) {
        try (FileInputStream fis = new FileInputStream(getFilePath().toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Sensors");
            if (sheet == null) return false;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String id = getCellValue(row, 1); // ID column
                if (id.equalsIgnoreCase(sensor.getSensorId())) {
                    row.getCell(4).setCellValue(sensor.getCurrentValue()); // CURRENT_VALUE
                    row.getCell(6).setCellValue(sensor.getUpdatedTimestamp().toString()); // UPDATED_TS
                    break;
                }
            }

            try (FileOutputStream fos = new FileOutputStream(getFilePath().toFile())) {
                workbook.write(fos);
            }

            Log.debug("📥 Sensor " + sensor.getSensorName() + " updated in Excel");
            return true;

        } catch (IOException e) {
            Log.error("❌ Failed to update sensor sheet: " + e.getMessage());
            return false;
        }
    }
    public static boolean appendToSensCtrlSheet(Device linkedDevice, Sensor master) {
        return AutoOpLinker.SensorLinkManager(linkedDevice, master);
    }


}
