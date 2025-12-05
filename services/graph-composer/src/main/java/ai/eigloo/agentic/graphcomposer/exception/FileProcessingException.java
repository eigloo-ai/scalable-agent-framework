package ai.eigloo.agentic.graphcomposer.exception;

/**
 * Exception thrown when file processing operations fail.
 */
public class FileProcessingException extends RuntimeException {
    
    private final String fileName;
    private final String operation;
    
    public FileProcessingException(String message) {
        super(message);
        this.fileName = null;
        this.operation = null;
    }
    
    public FileProcessingException(String message, String fileName, String operation) {
        super(message);
        this.fileName = fileName;
        this.operation = operation;
    }
    
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.operation = null;
    }
    
    public FileProcessingException(String message, String fileName, String operation, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
        this.operation = operation;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getOperation() {
        return operation;
    }
}