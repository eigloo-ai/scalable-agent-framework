package ai.eigloo.agentic.admin.controller;

import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
import ai.eigloo.agentic.admin.repository.TenantInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Web controller for serving the tenant management HTML interface.
 * Provides Thymeleaf templates for tenant administration.
 */
@Controller
@RequestMapping("/admin")
public class TenantAdminWebController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantAdminWebController.class);
    
    private final TenantInfoRepository tenantInfoRepository;
    
    @Autowired
    public TenantAdminWebController(TenantInfoRepository tenantInfoRepository) {
        this.tenantInfoRepository = tenantInfoRepository;
    }
    
    /**
     * Serve the main tenant management page.
     * 
     * @param model the Thymeleaf model
     * @return the template name
     */
    @GetMapping
    public String tenantAdminPage(Model model) {
        try {
            logger.debug("Serving tenant admin page");
            
            // Get all tenants for display
            List<TenantInfoEntity> tenants = tenantInfoRepository.findAll();
            model.addAttribute("tenants", tenants);
            
            // Add form objects for the templates
            model.addAttribute("provisionForm", new ProvisionTenantRequest());
            model.addAttribute("disableForm", new DisableTenantRequest());
            model.addAttribute("deleteForm", new DeleteTenantRequest());
            
            logger.debug("Added {} tenants to model", tenants.size());
            return "tenant-admin";
            
        } catch (Exception e) {
            logger.error("Error serving tenant admin page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading tenant data: " + e.getMessage());
            return "tenant-admin";
        }
    }
} 