package ai.eigloo.agentic.graph.api;

/**
 * Serializable executor file payload for internal graph lookups.
 */
public class GraphLookupFile {

    private String name;
    private String contents;

    public GraphLookupFile() {
    }

    public GraphLookupFile(String name, String contents) {
        this.name = name;
        this.contents = contents;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }
}
