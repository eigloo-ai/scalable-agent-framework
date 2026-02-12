package ai.eigloo.agentic.graph.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable task node shape for internal graph lookups.
 */
public class GraphLookupTask {

    private String name;
    private String upstreamPlanName;
    private String downstreamPlanName;
    private List<GraphLookupFile> files = new ArrayList<>();

    public GraphLookupTask() {
    }

    public GraphLookupTask(
            String name,
            String upstreamPlanName,
            String downstreamPlanName,
            List<GraphLookupFile> files) {
        this.name = name;
        this.upstreamPlanName = upstreamPlanName;
        this.downstreamPlanName = downstreamPlanName;
        this.files = files != null ? files : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUpstreamPlanName() {
        return upstreamPlanName;
    }

    public void setUpstreamPlanName(String upstreamPlanName) {
        this.upstreamPlanName = upstreamPlanName;
    }

    public String getDownstreamPlanName() {
        return downstreamPlanName;
    }

    public void setDownstreamPlanName(String downstreamPlanName) {
        this.downstreamPlanName = downstreamPlanName;
    }

    public List<GraphLookupFile> getFiles() {
        return files;
    }

    public void setFiles(List<GraphLookupFile> files) {
        this.files = files != null ? files : new ArrayList<>();
    }
}
