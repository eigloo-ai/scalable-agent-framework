package ai.eigloo.agentic.graph.model;

import java.time.LocalDateTime;

/**
 * Represents a file associated with a plan or task in the agent graph.
 * 
 * <p>ExecutorFile stores the content and metadata for files that are part of
 * plan or task implementations, such as Python source files and requirements.txt files.
 * Each file tracks its creation and update timestamps along with version information.</p>
 * 
 * @param fileName The name of the file (cannot be null or empty)
 * @param contents The file contents (cannot be null)
 * @param creationDate The creation timestamp (cannot be null)
 * @param version The version string (cannot be null or empty)
 * @param updateDate The last update timestamp (cannot be null)
 */
public record ExecutorFile(
    String fileName,
    String contents,
    LocalDateTime creationDate,
    String version,
    LocalDateTime updateDate
) {
    
    /**
     * Compact constructor with validation.
     * 
     * @param fileName The file name
     * @param contents The file contents
     * @param creationDate The creation date
     * @param version The version string
     * @param updateDate The update date
     * @throws IllegalArgumentException if validation fails
     */
    public ExecutorFile {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (contents == null) {
            throw new IllegalArgumentException("File contents cannot be null");
        }
        if (creationDate == null) {
            throw new IllegalArgumentException("Creation date cannot be null");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        if (updateDate == null) {
            throw new IllegalArgumentException("Update date cannot be null");
        }
        
        // Normalize the fileName
        fileName = fileName.trim();
        version = version.trim();
    }
    
    /**
     * Creates a new ExecutorFile with the specified name and contents.
     * Creation and update dates are set to the current time, and version is set to "1.0".
     * 
     * @param fileName The file name (cannot be null or empty)
     * @param contents The file contents (cannot be null)
     * @return A new ExecutorFile
     */
    public static ExecutorFile of(String fileName, String contents) {
        LocalDateTime now = LocalDateTime.now();
        return new ExecutorFile(fileName, contents, now, "1.0", now);
    }
}