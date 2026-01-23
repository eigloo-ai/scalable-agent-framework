package ai.eigloo.agentic.admin.validation;

import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantValidationUtils.
 * Tests comprehensive input validation for tenant operations.
 */
@DisplayName("TenantValidationUtils Tests")
class TenantValidationUtilsTest {
    
    @Test
    @DisplayName("Should validate valid tenant ID")
    void shouldValidateValidTenantId() {
        String[] validTenantIds = {
            "tenant1",
            "tenant-123",
            "tenant_456",
            "TenantABC",
            "tenant123",
            "a",
            "a".repeat(50)
        };
        
        for (String tenantId : validTenantIds) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantId(tenantId);
            assertTrue(result.isValid(), "Tenant ID should be valid: " + tenantId);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    @DisplayName("Should reject invalid tenant ID")
    void shouldRejectInvalidTenantId() {
        String[] invalidTenantIds = {
            null,
            "",
            " ",
            "tenant@123",
            "tenant#456",
            "tenant$789",
            "tenant%abc",
            "tenant^def",
            "tenant&ghi",
            "tenant*jkl",
            "tenant(mno)",
            "tenant[pqr]",
            "tenant{stu}",
            "tenant<vwx>",
            "tenant,yz",
            "tenant;123",
            "tenant:456",
            "tenant\"789",
            "tenant'abc",
            "tenant\\def",
            "tenant/ghi",
            "tenant|jkl",
            "tenant~mno",
            "tenant`pqr",
            "tenant!stu",
            "tenant?vwx",
            "tenant.yz",
            "a".repeat(51)
        };
        
        for (String tenantId : invalidTenantIds) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantId(tenantId);
            assertFalse(result.isValid(), "Tenant ID should be invalid: " + tenantId);
            assertNotNull(result.getErrorMessage());
        }
    }
    
    @Test
    @DisplayName("Should validate valid tenant name")
    void shouldValidateValidTenantName() {
        String[] validTenantNames = {
            "Tenant One",
            "tenant-123",
            "tenant_456",
            "Tenant ABC",
            "tenant123",
            "a",
            "a".repeat(50),
            "Tenant with spaces",
            "Tenant-with-hyphens",
            "Tenant_with_underscores"
        };
        
        for (String tenantName : validTenantNames) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantName(tenantName);
            assertTrue(result.isValid(), "Tenant name should be valid: " + tenantName);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    @DisplayName("Should reject invalid tenant name")
    void shouldRejectInvalidTenantName() {
        String[] invalidTenantNames = {
            null,
            "",
            " ",
            "tenant@123",
            "tenant#456",
            "tenant$789",
            "tenant%abc",
            "tenant^def",
            "tenant&ghi",
            "tenant*jkl",
            "tenant(mno)",
            "tenant[pqr]",
            "tenant{stu}",
            "tenant<vwx>",
            "tenant,yz",
            "tenant;123",
            "tenant:456",
            "tenant\"789",
            "tenant'abc",
            "tenant\\def",
            "tenant/ghi",
            "tenant|jkl",
            "tenant~mno",
            "tenant`pqr",
            "tenant!stu",
            "tenant?vwx",
            "tenant.yz",
            "a".repeat(51)
        };
        
        for (String tenantName : invalidTenantNames) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantName(tenantName);
            assertFalse(result.isValid(), "Tenant name should be invalid: " + tenantName);
            assertNotNull(result.getErrorMessage());
        }
    }
    
    @Test
    @DisplayName("Should validate valid notes")
    void shouldValidateValidNotes() {
        String[] validNotes = {
            null,
            "",
            "Valid notes",
            "Notes with numbers 123",
            "Notes with symbols: !@#$%^&*()",
            "Notes with spaces and punctuation.",
            "a".repeat(512),

            "Notes with unicode: ñáéíóú",
            "Notes with emojis: 😀🎉🚀"
        };
        
        for (String notes : validNotes) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateNotes(notes);
            assertTrue(result.isValid(), "Notes should be valid: " + notes);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    @DisplayName("Should reject invalid notes")
    void shouldRejectInvalidNotes() {
        String[] invalidNotes = {
            "a".repeat(513),
            "Notes with control characters\u0000",
            "Notes with control characters\u0001",
            "Notes with control characters\u0002",
            "Notes with control characters\u001F",
            "Notes with control characters\u007F",
            "Notes with newlines\nand tabs\tand other whitespace"
        };
        
        for (String notes : invalidNotes) {
            TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateNotes(notes);
            assertFalse(result.isValid(), "Notes should be invalid: " + notes);
            assertNotNull(result.getErrorMessage());
        }
    }
    

    
    @Test
    @DisplayName("Should validate valid ProvisionTenantRequest")
    void shouldValidateValidProvisionTenantRequest() {
        ProvisionTenantRequest request = new ProvisionTenantRequest("valid-tenant", "Valid Tenant Name", "Valid notes");
        
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateProvisionTenantRequest(request);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid ProvisionTenantRequest")
    void shouldRejectInvalidProvisionTenantRequest() {
        // Test null request
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateProvisionTenantRequest(null);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid tenant ID
        ProvisionTenantRequest invalidRequest = new ProvisionTenantRequest("@invalid", "Valid Name", "Valid notes");
        result = TenantValidationUtils.validateProvisionTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid tenant name
        invalidRequest = new ProvisionTenantRequest("valid-tenant", "@invalid", "Valid notes");
        result = TenantValidationUtils.validateProvisionTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid notes
        invalidRequest = new ProvisionTenantRequest("valid-tenant", "Valid Name", "a".repeat(513));
        result = TenantValidationUtils.validateProvisionTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should validate valid DisableTenantRequest")
    void shouldValidateValidDisableTenantRequest() {
        DisableTenantRequest request = new DisableTenantRequest("valid-tenant", "Valid notes");
        
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateDisableTenantRequest(request);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid DisableTenantRequest")
    void shouldRejectInvalidDisableTenantRequest() {
        // Test null request
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateDisableTenantRequest(null);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid tenant ID
        DisableTenantRequest invalidRequest = new DisableTenantRequest("@invalid", "Valid notes");
        result = TenantValidationUtils.validateDisableTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid notes
        invalidRequest = new DisableTenantRequest("valid-tenant", "a".repeat(513));
        result = TenantValidationUtils.validateDisableTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should validate valid DeleteTenantRequest")
    void shouldValidateValidDeleteTenantRequest() {
        DeleteTenantRequest request = new DeleteTenantRequest("valid-tenant", "Valid notes");
        
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateDeleteTenantRequest(request);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid DeleteTenantRequest")
    void shouldRejectInvalidDeleteTenantRequest() {
        // Test null request
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateDeleteTenantRequest(null);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid tenant ID
        DeleteTenantRequest invalidRequest = new DeleteTenantRequest("@invalid", "Valid notes");
        result = TenantValidationUtils.validateDeleteTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        
        // Test request with invalid notes
        invalidRequest = new DeleteTenantRequest("valid-tenant", "a".repeat(513));
        result = TenantValidationUtils.validateDeleteTenantRequest(invalidRequest);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
    

    
    @Test
    @DisplayName("Should validate edge cases for tenant ID")
    void shouldValidateEdgeCasesForTenantId() {
        // Test minimum length
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantId("a");
        assertTrue(result.isValid());
        
        // Test maximum length
        result = TenantValidationUtils.validateTenantId("a".repeat(50));
        assertTrue(result.isValid());
        
        // Test just over maximum length
        result = TenantValidationUtils.validateTenantId("a".repeat(51));
        assertFalse(result.isValid());
        
        // Test mixed valid characters
        result = TenantValidationUtils.validateTenantId("tenant-123_ABC");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate edge cases for tenant name")
    void shouldValidateEdgeCasesForTenantName() {
        // Test minimum length
        TenantValidationUtils.ValidationResult result = TenantValidationUtils.validateTenantName("a");
        assertTrue(result.isValid());
        
        // Test maximum length
        result = TenantValidationUtils.validateTenantName("a".repeat(50));
        assertTrue(result.isValid());
        
        // Test just over maximum length
        result = TenantValidationUtils.validateTenantName("a".repeat(51));
        assertFalse(result.isValid());
        
        // Test mixed valid characters
        result = TenantValidationUtils.validateTenantName("Tenant Name-123_ABC");
        assertTrue(result.isValid());
    }
} 