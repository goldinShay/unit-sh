package devices.actions;

public enum WashingMachineAction {
    // Power-related
    ON("Power On"),
    OFF("Power Off"),

    // Wash programs
    QUICK_WASH("Quick Wash", 40, 800),
    ECO_WASH("Eco Wash", 30, 600),
    DELICATE_WASH("Delicate Wash", 40, 600),
    HEAVY_DUTY("Heavy Duty", 60, 1000),
    RINSE_AND_SPIN("Rinse & Spin", 20, 1200),
    COTTON_WASH("Cotton Wash",40 ,1000 );

    private final String label;
    private final int waterTempCelsius;
    private final int spinSpeedRPM;

    // 🔧 Constructor for wash programs
    WashingMachineAction(String label, int temp, int spinSpeed) {
        this.label = label;
        this.waterTempCelsius = temp;
        this.spinSpeedRPM = spinSpeed;
    }

    // 🔧 Constructor for power actions
    WashingMachineAction(String label) {
        this(label, -1, -1); // Indicates non-washing program
    }

    public String getLabel() {
        return label;
    }

    public int getWaterTemp() {
        return waterTempCelsius;
    }

    public int getSpinSpeed() {
        return spinSpeedRPM;
    }

    public boolean isProgram() {
        return waterTempCelsius >= 0 && spinSpeedRPM >= 0;
    }

    public boolean isPowerAction() {
        return this == ON || this == OFF;
    }

    @Override
    public String toString() {
        return label;
    }

    public static WashingMachineAction fromString(String input) {
        for (WashingMachineAction action : values()) {
            if (action.name().equalsIgnoreCase(input) ||
                    action.label.equalsIgnoreCase(input)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown WashingMachineAction: " + input);
    }
}
