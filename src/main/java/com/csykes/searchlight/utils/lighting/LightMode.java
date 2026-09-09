package com.csykes.searchlight.utils.lighting;

public enum LightMode {
    FIXTURE("Fixture"),
    SEPARATE("Separate");

    private final String displayName;

    LightMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LightMode fromString(String name) {
        if (name != null) {
            for (LightMode mode : values()) {
                if (mode.name().equalsIgnoreCase(name) || mode.displayName.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
        }
        return FIXTURE;
    }
}
