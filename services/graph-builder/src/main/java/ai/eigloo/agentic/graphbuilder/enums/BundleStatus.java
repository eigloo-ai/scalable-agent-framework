package ai.eigloo.agentic.graphbuilder.enums;

/**
 * Enumeration of possible bundle processing statuses.
 */
public enum BundleStatus {
    UPLOADED("UPLOADED"),
    EXTRACTING("EXTRACTING"),
    PARSING("PARSING"),
    PERSISTING("PERSISTING"),
    BUILDING_IMAGES("BUILDING_IMAGES"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    BundleStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Gets the BundleStatus enum from its string value.
     * 
     * @param value The string value
     * @return The corresponding BundleStatus enum
     * @throws IllegalArgumentException if no matching enum is found
     */
    public static BundleStatus fromValue(String value) {
        for (BundleStatus status : BundleStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BundleStatus value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}