package ai.eigloo.agentic.graphbuilder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web controller for graph bundle upload and status monitoring pages.
 * Serves HTML templates for the web interface.
 */
@Controller
@RequestMapping("/graph-bundle")
public class GraphBundleWebController {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphBundleWebController.class);
    
    /**
     * Serve the graph bundle upload page.
     * 
     * @param model The Spring model for template rendering
     * @return The template name for the upload page
     */
    @GetMapping
    public String uploadPage(Model model) {
        logger.debug("Serving graph bundle upload page");
        
        // Add any model attributes needed for the upload page
        model.addAttribute("pageTitle", "Graph Bundle Upload");
        model.addAttribute("maxFileSize", "100MB");
        model.addAttribute("allowedExtensions", ".tar, .tar.gz, .zip");
        
        return "graph-bundle/upload";
    }
    
    /**
     * Serve the status monitoring page.
     * 
     * @param processId Optional process ID to monitor specific upload
     * @param model The Spring model for template rendering
     * @return The template name for the status page
     */
    @GetMapping("/status")
    public String statusPage(@RequestParam(value = "processId", required = false) String processId,
                           Model model) {
        logger.debug("Serving graph bundle status page for process ID: {}", processId);
        
        // Add model attributes for the status page
        model.addAttribute("pageTitle", "Processing Status");
        model.addAttribute("processId", processId);
        
        return "graph-bundle/status";
    }
}