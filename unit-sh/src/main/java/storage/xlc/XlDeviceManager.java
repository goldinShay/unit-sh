package storage.xlc;


import devices.*;
import devices.actions.ApprovedDeviceModel;
import devices.actions.LiveDeviceState;
import devices.actions.SmartLightAction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import storage.DeviceStorage;
import storage.xlc.sheetsCommand.DeviceSheetCommand;
import storage.xlc.sheetsCommand.SmartLightSheetCommand;
import storage.xlc.sheetsCommand.XlTabNames;
import ui.gui.managers.GuiStateManager;
import utils.Log;
import utils.TimestampUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;

import static storage.xlc.XlWorkbookUtils.updateWorkbook;


// ... other imports

public class XlDeviceManager {
    private static final Path FILE_PATH = XlWorkbookUtils.getFilePath();
    private static final Clock clock = utils.ClockUtil.getClock();


    public static List<Device> loadDevicesFromExcel() {
        List<Device> devices = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Log.debug("📁 Loading devices from Excel file: " + FILE_PATH);

        try (FileInputStream fis = new FileInputStream(FILE_PATH.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(XlTabNames.DEVICES.name());
            if (sheet == null) {
                Log.warn("⚠️ Sheet '" + XlTabNames.DEVICES.name() + "' not found.");
                return devices;
            }

            Map<DeviceSheetCommand, Integer> columnMap = DeviceSheetCommand.getColumnMap();

            for (Row row : sheet) {
                int rowIndex = row.getRowNum();
                if (rowIndex == 0 || row == null) continue;

                try {
                    String typeStr = getCellValue(row, columnMap.get(DeviceSheetCommand.TYPE)).trim();
                    if (typeStr.isBlank()) continue;

                    String id = getCellValue(row, columnMap.get(DeviceSheetCommand.DEVICE_ID)).trim();
                    String name = getCellValue(row, columnMap.get(DeviceSheetCommand.NAME));
                    String brand = getCellValue(row, columnMap.get(DeviceSheetCommand.BRAND));
                    String model = getCellValue(row, columnMap.get(DeviceSheetCommand.MODEL));
                    String autoEnabledStr = getCellValue(row, columnMap.get(DeviceSheetCommand.AUTO_ENABLED)).trim();

                    boolean autoEnabled = "1".equals(autoEnabledStr) || "true".equalsIgnoreCase(autoEnabledStr);

                    double autoOn = XlWorkbookUtils.getSafeNumeric(
                            row.getCell(columnMap.get(DeviceSheetCommand.AUTO_ON)),
                            DeviceDefaults.getDefaultAutoOn(DeviceType.fromString(typeStr))
                    );

                    double autoOff = XlWorkbookUtils.getSafeNumeric(
                            row.getCell(columnMap.get(DeviceSheetCommand.AUTO_OFF)),
                            DeviceDefaults.getDefaultAutoOff(DeviceType.fromString(typeStr))
                    );

                    DeviceType type = DeviceType.fromString(typeStr);
                    if (type == null || type == DeviceType.UNKNOWN) {
                        throw new IllegalArgumentException("❌ Invalid or unsupported device type: " + typeStr);
                    }

                    if (seenIds.contains(id)) {
                        throw new IllegalArgumentException("❌ Duplicate ID in this session: " + id);
                    }

                    ApprovedDeviceModel adm = ApprovedDeviceModel.lookup(brand, model);
                    if (adm == null) {
                        Log.warn("🚫 No matching ApprovedDeviceModel for brand/model: " + brand + "/" + model);
                    } else {
                        Log.debug("✅ Approved model found: " + adm.getBrand() + " / " + adm.getModel());
                    }

                    Device device = DeviceFactory.createDeviceByType(
                            type, id, name, clock, DeviceStorage.getDevices(), brand, model
                    );
                    if (device == null) {
                        throw new IllegalStateException("❌ Device creation returned null for ID: " + id);
                    }

                    String stateValue = getCellValue(row, columnMap.get(DeviceSheetCommand.STATE)).trim();
                    device.setState("ON".equalsIgnoreCase(stateValue));

                    device.setBrand(brand);
                    device.setModel(model);
                    device.setAutomationEnabled(autoEnabled);
                    device.setAutoThreshold(autoOn, autoEnabled);
                    device.setAutoThreshold(autoOff, autoEnabled);

                    devices.add(device);
                    seenIds.add(id);
                    DeviceStorage.getDevices().put(id, device);

                    // 🧠 GUI sync begins here
                    GuiStateManager.registerNewDevice(device);
                    if (device.isOn()) {
                        LiveDeviceState.turnOn(device);
                    } else {
                        LiveDeviceState.turnOff(device);
                    }

//                    Log.info("✅ Loaded & registered device: " + device.getId() + " (" + device.getType() + ")");

                } catch (Exception ex) {
                    Log.warn("🚫 Failed to parse row " + rowIndex + ": " + ex.getMessage());
                }
            }

        } catch (IOException e) {
            Log.error("🛑 Excel read error: " + e.getMessage());
        }

        // 🌟 Load SmartLights separately and add them in
        Map<String, SmartLight> smartLights = XlSmartLightManager.loadSmartLights();
        devices.addAll(smartLights.values());

        // 🖼️ Final GUI refresh
        GuiStateManager.refreshDeviceMatrix();

        Log.info("📦 Total devices loaded: " + devices.size());
        return devices;
    }

    public static void writeDeviceRow(Device device, Row row) {
        System.out.println("🧾 Writing Device [" + device.getName() + "] (ID: " + device.getId() + ", MosesType: " + device.getType().name() + ", Threshold: " + device.getAutoThreshold() + ") → Sheet: " + row.getSheet().getSheetName());
        Map<DeviceSheetCommand, Integer> columnMap = DeviceSheetCommand.getColumnMap();
        Clock clock = device instanceof Device ? device.getClock() : Clock.systemDefaultZone();

        // 🔧 Write standard fields using column map
        row.createCell(columnMap.get(DeviceSheetCommand.TYPE)).setCellValue(device.getType().name());
        row.createCell(columnMap.get(DeviceSheetCommand.DEVICE_ID)).setCellValue(device.getId());
        row.createCell(columnMap.get(DeviceSheetCommand.NAME)).setCellValue(device.getName());
        Log.info("🧠 Writing Device Row for device '" + device.getName() + "' (" + device.getId() + ") | Brand: " + device.getBrand() + ", Model: " + device.getModel());
        row.createCell(columnMap.get(DeviceSheetCommand.BRAND)).setCellValue(device.getBrand());
        row.createCell(columnMap.get(DeviceSheetCommand.MODEL)).setCellValue(device.getModel());
        row.createCell(columnMap.get(DeviceSheetCommand.AUTO_ENABLED)).setCellValue(device.isAutomationEnabled());
        row.createCell(columnMap.get(DeviceSheetCommand.AUTO_ON)).setCellValue(device.getAutoThreshold());
        row.createCell(columnMap.get(DeviceSheetCommand.AUTO_OFF)).setCellValue(device.getAutoThreshold());
        row.createCell(columnMap.get(DeviceSheetCommand.ACTIONS)).setCellValue(String.join(", ", device.getAvailableActions()));
        row.createCell(columnMap.get(DeviceSheetCommand.STATE)).setCellValue(device.isOn() ? "ON" : "OFF");

        // 🕒 REMOVED_TS
        String removed = device.getRemovedTimestamp();
        row.createCell(columnMap.get(DeviceSheetCommand.REMOVED_TS)).setCellValue(removed != null ? removed : "N/A");

        // 🕒 ADDED_TS (re-assign and write)
        ZonedDateTime added = TimestampUtils.safeParseTimestamp(
                device.getAddedTimestamp(), clock
        );
        device.setAddedTimestamp(added);
        row.createCell(columnMap.get(DeviceSheetCommand.ADDED_TS)).setCellValue(added.toString());

        // 🕒 UPDATED_TS (write current version)
        String updatedTs = device.getUpdatedTimestamp();
        row.createCell(columnMap.get(DeviceSheetCommand.UPDATED_TS))
                .setCellValue(updatedTs != null ? updatedTs : "N/A");
        // 🌈 SmartLight control data is handled separately.
    }

    public static boolean updateDevice(Device device) throws IOException {
        return updateWorkbook((workbook, tasks, sheet, sensors, sensCtrl, smartControl) -> {
            Map<DeviceSheetCommand, Integer> columnMap = DeviceSheetCommand.getColumnMap();
            boolean updated = false;

            String deviceId = device.getId().trim();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Cell idCell = row.getCell(columnMap.get(DeviceSheetCommand.DEVICE_ID));
                if (idCell == null || idCell.getCellType() == CellType.BLANK) continue;

                // 🔒 Add this check to skip malformed rows
                if (row.getLastCellNum() < columnMap.get(DeviceSheetCommand.DEVICE_ID) + 1) {
                    Log.warn("⚠️ Skipping malformed row at index " + row.getRowNum() + ": too few cells");
                    continue;
                }

                String rowId = getCellValue(row, columnMap.get(DeviceSheetCommand.DEVICE_ID)).trim();
                if (rowId.equalsIgnoreCase(deviceId)) {
                    // 🔄 Update existing device
                    setCell(row, columnMap.get(DeviceSheetCommand.TYPE), device.getType().name());
                    setCell(row, columnMap.get(DeviceSheetCommand.NAME), device.getName());
                    setCell(row, columnMap.get(DeviceSheetCommand.BRAND), device.getBrand());
                    setCell(row, columnMap.get(DeviceSheetCommand.MODEL), device.getModel());
                    setCell(row, columnMap.get(DeviceSheetCommand.AUTO_ENABLED), device.isAutomationEnabled());
                    setCell(row, columnMap.get(DeviceSheetCommand.AUTO_ON), device.getAutoThreshold());
                    setCell(row, columnMap.get(DeviceSheetCommand.AUTO_OFF), device.getAutoThreshold());
                    setCell(row, columnMap.get(DeviceSheetCommand.ACTIONS), device.getSupportedActionsAsText());
                    setCell(row, columnMap.get(DeviceSheetCommand.STATE), device.isOn() ? "ON" : "OFF");
                    setCell(row, columnMap.get(DeviceSheetCommand.ADDED_TS), ZonedDateTime.now(clock).toString());
                    setCell(row, columnMap.get(DeviceSheetCommand.UPDATED_TS), java.time.ZonedDateTime.now(clock).toString());
                    setCell(row, columnMap.get(DeviceSheetCommand.REMOVED_TS), "");

                    Log.info("✅ Device updated in Excel: " + deviceId);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                Log.warn("⚠️ No matching device found for update, appending new: " + deviceId);

                Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
                setCell(newRow, columnMap.get(DeviceSheetCommand.TYPE), device.getType().name());
                setCell(newRow, columnMap.get(DeviceSheetCommand.DEVICE_ID), deviceId);
                setCell(newRow, columnMap.get(DeviceSheetCommand.NAME), device.getName());
                setCell(newRow, columnMap.get(DeviceSheetCommand.BRAND), device.getBrand());
                setCell(newRow, columnMap.get(DeviceSheetCommand.MODEL), device.getModel());
                setCell(newRow, columnMap.get(DeviceSheetCommand.AUTO_ENABLED), device.isAutomationEnabled());
                setCell(newRow, columnMap.get(DeviceSheetCommand.AUTO_ON), device.getAutoThreshold());
                setCell(newRow, columnMap.get(DeviceSheetCommand.AUTO_OFF), device.getAutoThreshold());
                setCell(newRow, columnMap.get(DeviceSheetCommand.ACTIONS), device.getSupportedActionsAsText());
                setCell(newRow, columnMap.get(DeviceSheetCommand.STATE), device.isOn() ? "ON" : "OFF");
                setCell(newRow, columnMap.get(DeviceSheetCommand.UPDATED_TS), java.time.ZonedDateTime.now(clock).toString());

                Log.info("✨ New device appended to Excel: " + deviceId);
            }
        });
    }

    private static void setCell(Row row, int colIndex, Object value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);

        if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    public static boolean removeDevice(String deviceId) {
        try {
            updateWorkbook((workbook, tasks, sheet, sensors, sensCtrl, smartControl) -> {
                int lastRow = sheet.getLastRowNum();
                for (int i = 1; i <= lastRow; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String rowId = getCellValue(row, DeviceSheetCommand.DEVICE_ID.ordinal());
                    if (rowId.equalsIgnoreCase(deviceId)) {
                        sheet.removeRow(row);
                        if (i < lastRow) {
                            sheet.shiftRows(i + 1, lastRow, -1);
                        }

                        Log.info("🗑️ Removed device from Excel: " + deviceId);
                        return;
                    }
                }

                Log.warn("⚠️ Device not found during remove: " + deviceId);
                throw new IOException("Device not found in Excel sheet: " + deviceId);
            });

            return true;

        } catch (IOException e) {
            System.err.println("❌ Failed to remove device: " + e.getMessage());
            return false;
        }
    }

    public static String getNextAvailableId(String prefix, Set<String> existingIds) {
        int max = existingIds.stream()
                .filter(id -> id.startsWith(prefix))
                .map(id -> id.replace(prefix, ""))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        String nextId = prefix + String.format("%03d", max + 1);
        Log.debug("🔢 Generated next device ID: " + nextId);
        return nextId;
    }
    public static SmartLight getOriginalDeviceById(String deviceId) {
        // ✅ First, check if the device is already loaded in memory
        SmartLight existing = DeviceStorage.getSmartLight(deviceId);
        if (existing != null) {
            Log.info("🔄 Returning existing SmartLight from memory: " + deviceId);
            return existing;
        }

        // 🧭 Fallback: attempt to reconstruct from Excel only if not found in memory
        Workbook workbook = XlWorkbookUtils.loadWorkbook();
        if (workbook == null) {
            Log.error("❌ Cannot load workbook to retrieve original device.");
            return null;
        }

        Sheet deviceSheet = workbook.getSheet("Devices");
        if (deviceSheet == null) {
            Log.error("❌ Device sheet not found in workbook.");
            return null;
        }

        Map<DeviceSheetCommand, Integer> columnMap = DeviceSheetCommand.getColumnMap();
        int idCol = columnMap.getOrDefault(DeviceSheetCommand.DEVICE_ID, -1);
        int nameCol = columnMap.getOrDefault(DeviceSheetCommand.NAME, -1);
        int brandCol = columnMap.getOrDefault(DeviceSheetCommand.BRAND, -1);
        int modelCol = columnMap.getOrDefault(DeviceSheetCommand.MODEL, -1);
        int actionsCol = columnMap.getOrDefault(DeviceSheetCommand.ACTIONS, -1);

        if (idCol == -1 || nameCol == -1) {
            Log.error("❌ Required columns (DEVICE_ID or NAME) not found.");
            return null;
        }

        for (Row row : deviceSheet) {
            Cell idCell = row.getCell(idCol);
            if (idCell != null && deviceId.equalsIgnoreCase(idCell.getStringCellValue().trim())) {
                String name = getCellValue(row, nameCol);
                String brand = getCellValue(row, brandCol);
                String model = getCellValue(row, modelCol);

                ApprovedDeviceModel approved = ApprovedDeviceModel.lookup(brand, model);
                if (approved == null) {
                    Log.warn("⚠️ No approved model found for brand='" + brand + "', model='" + model + "'");
                }

                Clock clock = Clock.systemDefaultZone(); // Replace with your app's clock if needed
                boolean isOn = false; // Default assumption

                SmartLight light = new SmartLight(deviceId, name, approved, clock, isOn);

                if (actionsCol != -1) {
                    String actionsText = getCellValue(row, actionsCol);
                    if (!actionsText.isBlank()) {
                        light.setSupportedActionsFromText(actionsText);
                    }
                }

                Log.info("📦 Reconstructed SmartLight from sheet: " + deviceId);
                return light;
            }
        }

        Log.warn("⚠️ No matching device found for ID: " + deviceId);
        return null;
    }

    // 🔧 Helper method for safe cell value extraction
    private static String getCellValue(Row row, int colIndex) {
        if (row == null || colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue()).trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue()).trim();
            default -> "";
        };
    }
    public static Device getDeviceById(String id) {
        Device device = DeviceStorage.getDevices().get(id);
        if (device == null) {
            throw new IllegalArgumentException("No device found with ID: " + id);
        }
        return device;
    }

}

