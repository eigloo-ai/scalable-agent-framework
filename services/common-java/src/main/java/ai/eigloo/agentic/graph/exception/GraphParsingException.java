package ai.eigloo.agentic.graph.exception;

/**
 * Exception thrown when agent graph parsing fails.
 * 
 * <p>This exception is thrown when the graph specification file cannot be parsed
 * due to syntax errors, missing files, or other parsing-related issues. The
 * exception includes context information about where the parsing failed to help
 * with debugging.</p>
 * 
 * <p>Common causes include:</p>
 * <ul>
 *   <li>Malformed GraphViz DOT syntax</li>
 *   <li>Missing specification file</li>
 *   <li>Invalid file format</li>
 *   <li>I/O errors reading the specification</li>
 * </ul>
 */
public class GraphParsingException extends RuntimeException {
    
    private final String fileName;
    private final Integer lineNumber;
    
    /**
     * Creates a new GraphParsingException with a formatted message.
     * 
     * @param message The error message format
     * @param args The message arguments
     * @return A new GraphParsingException
     */
    public static GraphParsingException parsingError(String message, Object... args) {
        return new GraphParsingException(String.format(message, args));
    }
    
    /**
     * Creates a new GraphParsingException with file context.
     * 
     * @param fileName The name of the file being parsed
     * @param message The error message
     * @return A new GraphParsingException
     */
    public static GraphParsingException parsingError(String fileName, String message) {
        return new GraphParsingException(fileName, null, message);
    }
    
    /**
     * Creates a new GraphParsingException with file and line context.
     * 
     * @param fileName The name of the file being parsed
     * @param lineNumber The line number where parsing failed
     * @param message The error message
     * @return A new GraphParsingException
     */
    public static GraphParsingException parsingError(String fileName, Integer lineNumber, String message) {
        return new GraphParsingException(fileName, lineNumber, message);
    }
    
    /**
     * Creates a new GraphParsingException with file context and cause.
     * 
     * @param fileName The name of the file being parsed
     * @param message The error message
     * @param cause The underlying cause
     * @return A new GraphParsingException
     */
    public static GraphParsingException parsingError(String fileName, String message, Throwable cause) {
        return new GraphParsingException(fileName, null, message, cause);
    }
    
    /**
     * Creates a new GraphParsingException with file and line context and cause.
     * 
     * @param fileName The name of the file being parsed
     * @param lineNumber The line number where parsing failed
     * @param message The error message
     * @param cause The underlying cause
     * @return A new GraphParsingException
     */
    public static GraphParsingException parsingError(String fileName, Integer lineNumber, String message, Throwable cause) {
        return new GraphParsingException(fileName, lineNumber, message, cause);
    }
    
    /**
     * Creates a new GraphParsingException.
     * 
     * @param message The error message
     */
    public GraphParsingException(String message) {
        super(message);
        this.fileName = null;
        this.lineNumber = null;
    }
    
    /**
     * Creates a new GraphParsingException with cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public GraphParsingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.lineNumber = null;
    }
    
    /**
     * Creates a new GraphParsingException with file context.
     * 
     * @param fileName The name of the file being parsed
     * @param lineNumber The line number where parsing failed
     * @param message The error message
     */
    public GraphParsingException(String fileName, Integer lineNumber, String message) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
    
    /**
     * Creates a new GraphParsingException with file context and cause.
     * 
     * @param fileName The name of the file being parsed
     * @param lineNumber The line number where parsing failed
     * @param message The error message
     * @param cause The underlying cause
     */
    public GraphParsingException(String fileName, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
    
    /**
     * Gets the name of the file being parsed.
     * 
     * @return The file name, or null if not specified
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Gets the line number where parsing failed.
     * 
     * @return The line number, or null if not specified
     */
    public Integer getLineNumber() {
        return lineNumber;
    }
    
    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder();
        
        if (fileName != null) {
            message.append("File: ").append(fileName);
            if (lineNumber != null) {
                message.append(":").append(lineNumber);
            }
            message.append(" - ");
        }
        
        message.append(super.getMessage());
        return message.toString();
    }
} 