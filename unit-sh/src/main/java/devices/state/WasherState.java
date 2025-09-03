package devices.state;

import devices.actions.WashingMachineAction;
import devices.actions.advancedActions.WashActionsLGTwin;

public class WasherState {
    private static WashingMachineAction currentProgram = WashingMachineAction.ECO_WASH;
    private static int remainingTime = 0; // in seconds
    private static int waterTemperature = 40; // default stub
    private static int spinSpeed = 800; // RPM, default stub

    // Future indicators (placeholders)
    private static boolean detergentPresent = false;
    private static boolean softenerPresent = false;

    // === Program ===
    public static WashingMachineAction getCurrentProgram() {
        return currentProgram;
    }

    public static void setCurrentProgram(WashingMachineAction program) {
        currentProgram = program;
    }

    // === Time ===
    public static int getRemainingTime() {
        return remainingTime;
    }

    public static void setRemainingTime(int time) {
        remainingTime = time;
    }

    // === Temperature ===
    public static int getWaterTemperature() {
        return waterTemperature;
    }

    public static void setWaterTemperature(int temp) {
        waterTemperature = temp;
    }

    // === Spin Speed ===
    public static int getSpinSpeed() {
        return spinSpeed;
    }

    public static void setSpinSpeed(int speed) {
        spinSpeed = speed;
    }

    // === Detergent ===
    public static boolean isDetergentPresent() {
        return detergentPresent;
    }

    public static void setDetergentPresent(boolean present) {
        detergentPresent = present;
    }

    // === Softener ===
    public static boolean isSoftenerPresent() {
        return softenerPresent;
    }

    public static void setSoftenerPresent(boolean present) {
        softenerPresent = present;
    }
    public static void applyProgram(WashingMachineAction action) {
        setCurrentProgram(action);
        setRemainingTime(WashActionsLGTwin.getEstimatedDuration(action));

        // Stubbed logic — later this will come from real data or Excel
        switch (action) {
            case QUICK_WASH -> {
                setWaterTemperature(30);
                setSpinSpeed(600);
            }
            case COTTON_WASH -> {
                setWaterTemperature(60);
                setSpinSpeed(1000);
            }
            case ECO_WASH -> {
                setWaterTemperature(40);
                setSpinSpeed(800);
            }
            case DELICATE_WASH -> {
                setWaterTemperature(30);
                setSpinSpeed(400);
            }
            case HEAVY_DUTY -> {
                setWaterTemperature(70);
                setSpinSpeed(1200);
            }
            case RINSE_AND_SPIN -> {
                setWaterTemperature(20);
                setSpinSpeed(1000);
            }
        }
    }

}