package ui.deviceActionMenu;

import autoOp.AutoOpController;
import devices.Device;
import devices.WashingMachine;
import devices.actions.WashingMachineAction;
import devices.actions.actionSimulator.WasherCycleSimulator;
import devices.actions.advancedActions.WashActionsLGTwin;
import devices.state.WasherState;

import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class WasherActionsMenu {

    public static void show(Device device) {
        WashingMachine washer = asWasher(device);
        runWasherMenu(washer, seconds -> {
            int min = seconds / 60;
            int sec = seconds % 60;
            System.out.printf("🕒 Remaining: %02d:%02d%n", min, sec);
        });
    }

    private static void runWasherMenu(WashingMachine washer, Consumer<Integer> onTick) {
        Scanner scanner = new Scanner(System.in);
        List<WashingMachineAction> availablePrograms = WashActionsLGTwin.getAvailablePrograms();
        WashingMachineAction currentProgram = WasherState.getCurrentProgram();

        while (true) {
            printHeader(washer, currentProgram);
            printOptions();

            System.out.print("Choose an option: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> {
                    washer.turnOn();
                    System.out.println("🟢 Washer turned ON.");
                }
                case "2" -> {
                    washer.turnOff();
                    System.out.println("🔴 Washer turned OFF.");
                }
                case "3" -> {
                    if (!washer.isOn()) {
                        System.out.println("⚠️ Washer must be ON to start a cycle.");
                        break;
                    }
                    System.out.println("🚿 Starting " + currentProgram.getLabel());
                    WasherCycleSimulator.simulateCycle(currentProgram, onTick);
                }
                case "4" -> {
                    currentProgram = selectProgram(scanner, availablePrograms);
                    if (currentProgram != null) {
                        WasherState.applyProgram(currentProgram);
                        washer.setMode(currentProgram);
                        System.out.println("✅ Program set to " + currentProgram.getLabel());
                    }
                }
                case "5" -> AutoOpController.display(washer);
                case "6" -> System.out.println("Not available from here at the moment");
                case "7" -> {
                    if (!washer.isOn()) {
                        washer.testDevice();
                        System.out.println("🔍 Test initiated.");
                    } else {
                        System.out.println("⚠️ Turn OFF the washer before testing.");
                    }
                }
                case "8" -> {
                    System.out.println("↩️ Returning to device menu.");
                    return;
                }
                default -> System.out.println("❌ Invalid option. Try again.");
            }
        }
    }

    private static void printHeader(WashingMachine washer, WashingMachineAction currentProgram) {
        String power = washer.isOn() ? "ON" : "OFF";
        String running = washer.isRunning() ? "YES" : "NO";
        String automation = washer.isAutomationEnabled() ? "ENABLED" : "DISABLED";
        String program = currentProgram != null ? currentProgram.getLabel() : "—";

        System.out.println("\n=== Washing Machine Actions ===");
        System.out.printf("Power: %s | Running: %s | Program: %s%n", power, running, program);
        System.out.println("Automation: " + automation);
    }

    private static void printOptions() {
        System.out.println("1 - Turn ON");
        System.out.println("2 - Turn OFF");
        System.out.println("3 - Start Program");
        System.out.println("4 - Change Program");
        System.out.println("5 - AutoOp");
        System.out.println("6 - Schedule");
        System.out.println("7 - Test Device");
        System.out.println("8 - Back");
    }

    private static WashingMachineAction selectProgram(Scanner scanner, List<WashingMachineAction> programs) {
        System.out.println("\nAvailable Programs:");
        for (int i = 0; i < programs.size(); i++) {
            WashingMachineAction action = programs.get(i);
            int duration = WashActionsLGTwin.getEstimatedDuration(action);
            System.out.printf("%d - %s (%d min)%n", i + 1, action.getLabel(), duration);
        }

        System.out.print("Select program: ");
        String input = scanner.nextLine().trim();
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < programs.size()) {
                return programs.get(index);
            } else {
                System.out.println("❌ Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Please enter a number.");
        }
        return null;
    }

    private static WashingMachine asWasher(Device device) {
        if (!(device instanceof WashingMachine washer)) {
            throw new IllegalArgumentException("⚠️ This menu is only for Washing Machines.");
        }
        return washer;
    }
}
