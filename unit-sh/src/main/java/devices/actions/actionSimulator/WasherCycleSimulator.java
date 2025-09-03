package devices.actions.actionSimulator;

import devices.actions.WashingMachineAction;
import devices.actions.advancedActions.WashActionsLGTwin;

import java.util.function.Consumer;

public class WasherCycleSimulator {

    public static void simulateCycle(WashingMachineAction action, Consumer<Integer> onTick) {
        int durationInSeconds = WashActionsLGTwin.getEstimatedDuration(action);

        System.out.println("⏳ Starting " + action.getLabel() + " cycle (" + durationInSeconds + " sec)");

        Thread thread = new Thread(() -> {
            for (int i = durationInSeconds; i >= 0; i--) {
                onTick.accept(i); // 👈 Update the label
                try {
                    Thread.sleep(1000); // simulate 1 second per minute
                } catch (InterruptedException e) {
                    System.out.println("\n❌ Cycle interrupted.");
                    return;
                }
            }
            System.out.println("\n✅ Cycle complete: " + action.getLabel());
        });

        thread.start();
    }
}
