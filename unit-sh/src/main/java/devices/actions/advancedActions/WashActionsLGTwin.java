package devices.actions.advancedActions;

import devices.actions.WashingMachineAction;

import java.util.List;

public class WashActionsLGTwin {

    public static List<WashingMachineAction> getAvailablePrograms() {
        return List.of(
                WashingMachineAction.QUICK_WASH,
                WashingMachineAction.COTTON_WASH,
                WashingMachineAction.ECO_WASH,
                WashingMachineAction.DELICATE_WASH,
                WashingMachineAction.HEAVY_DUTY,
                WashingMachineAction.RINSE_AND_SPIN
        );
    }

    public static int getEstimatedDuration(WashingMachineAction action) {
        return switch (action) {
            case QUICK_WASH -> 90;
            case COTTON_WASH -> 100;
            case ECO_WASH -> 180;
            case DELICATE_WASH -> 120;
            case HEAVY_DUTY -> 150;
            case RINSE_AND_SPIN -> 60;
            default -> 0;
        };
    }

    public static boolean supportsAdvancedPrograms() {
        return false; // Based on your CLI note
    }

    public static boolean isCompatible(String brand, String model) {
        return "LG".equalsIgnoreCase(brand) && "TwinWash".equalsIgnoreCase(model);
    }
}
