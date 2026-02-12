package ai.eigloo.agentic.graph.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable task node shape for internal graph lookups.
 */
public class GraphLookupTask {

    private String name;
    private List<GraphLookupFile> files = new ArrayList<>();

    public GraphLookupTask() {
    }

    public GraphLookupTask(
            String name,
            List<GraphLookupFile> files) {
        this.name = name;
        this.files = files != null ? files : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GraphLookupFile> getFiles() {
        return files;
    }

    public void setFiles(List<GraphLookupFile> files) {
        this.files = files != null ? files : new ArrayList<>();
    }
}
