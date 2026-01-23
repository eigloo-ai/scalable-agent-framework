package ai.eigloo.agentic.graphbuilder.enums;

/**
 * Enumeration of possible processing step statuses.
 */
public enum StepStatus {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    StepStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}