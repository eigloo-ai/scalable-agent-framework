package ai.eigloo.agentic.executorjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Python process execution settings.
 */
@ConfigurationProperties(prefix = "executor.python")
public class ExecutorPythonProperties {

    private String command = "python3";
    private int timeoutSeconds = 120;
    private String commonPyPath = "services/common-py";
    private String workingRoot = "${java.io.tmpdir}/executor-java";

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getCommonPyPath() {
        return commonPyPath;
    }

    public void setCommonPyPath(String commonPyPath) {
        this.commonPyPath = commonPyPath;
    }

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }
}
