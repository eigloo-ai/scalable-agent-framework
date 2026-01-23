package ai.eigloo.agentic.graphcomposer.exception;

/**
 * Exception thrown when a tenant tries to access resources they don't have permission for.
 */
public class TenantAccessException extends RuntimeException {
    
    private final String tenantId;
    private final String resource;
    
    public TenantAccessException(String message) {
        super(message);
        this.tenantId = null;
        this.resource = null;
    }
    
    public TenantAccessException(String message, String tenantId, String resource) {
        super(message);
        this.tenantId = tenantId;
        this.resource = resource;
    }
    
    public TenantAccessException(String message, Throwable cause) {
        super(message, cause);
        this.tenantId = null;
        this.resource = null;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public String getResource() {
        return resource;
    }
}