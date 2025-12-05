package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graphbuilder.config.GraphBundleProperties;
import ai.eigloo.agentic.graphbuilder.exception.BundleProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

/**
 * Service responsible for processing uploaded bundle files.
 * Handles file validation, extraction, and cleanup operations.
 */
@Service
public class BundleProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BundleProcessingService.class);
    private final GraphBundleProperties properties;
    
    @org.springframework.beans.factory.annotation.Autowired
    public BundleProcessingService(GraphBundleProperties properties) {
        this.properties = properties;
    }

    // Default constructor for tests
    public BundleProcessingService() {
        GraphBundleProperties props = new GraphBundleProperties();
        // Ensure defaults are reasonable for tests
        var upload = props.getUpload();
        upload.setMaxFileSize("100MB");
        upload.setAllowedExtensions(java.util.List.of(".tar", ".tar.gz", ".tgz", ".zip"));
        this.properties = props;
    }

    /**
     * Validates the uploaded file for size and format.
     * 
     * @param file The uploaded file to validate
     * @throws BundleProcessingException if validation fails
     */
    public void validateFile(MultipartFile file) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.debug("Validating uploaded file [correlationId={}]", correlationId);
            
            if (file == null || file.isEmpty()) {
                throw new BundleProcessingException("File cannot be null or empty", correlationId, "VALIDATION");
            }
            
            long maxBytes = parseSizeToBytes(properties.getUpload().getMaxFileSize());
            if (file.getSize() > maxBytes) {
                throw new BundleProcessingException(
                    "File size exceeds maximum allowed size of " + maxBytes + " bytes", 
                    correlationId, "VALIDATION");
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new BundleProcessingException("File must have a name", correlationId, "VALIDATION");
            }
            
            boolean validExtension = properties.getUpload().getAllowedExtensions().stream()
                .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
            
            if (!validExtension) {
                throw new BundleProcessingException(
                    "File must have one of the following extensions: " + properties.getUpload().getAllowedExtensions(), 
                    correlationId, "VALIDATION");
            }
            
            logger.debug("File validation passed for: {} [correlationId={}]", filename, correlationId);
            
        } catch (BundleProcessingException e) {
            logger.error("File validation failed [correlationId={}]: {}", correlationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during file validation [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Unexpected error during file validation: " + e.getMessage(), e, correlationId, "VALIDATION");
        }
    }

    private long parseSizeToBytes(String size) {
        if (size == null || size.isEmpty()) return 0L;
        String normalized = size.trim().toUpperCase();
        try {
            if (normalized.endsWith("KB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L;
            } else if (normalized.endsWith("MB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L * 1024L;
            } else if (normalized.endsWith("GB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L * 1024L * 1024L;
            } else if (normalized.endsWith("B")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 1).trim());
            } else {
                return Long.parseLong(normalized);
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse max file size '{}', defaulting to 100MB", size);
            return 100L * 1024L * 1024L;
        }
    }
    
    /**
     * Extracts an uploaded bundle file to a temporary directory.
     * 
     * @param file The uploaded file to extract
     * @param processId The process ID for unique directory naming
     * @return Path to the extraction directory
     * @throws BundleProcessingException if extraction fails
     */
    public Path extractBundle(MultipartFile file, String processId) {
        String correlationId = getOrCreateCorrelationId();
        Path extractDir = null;
        
        try {
            logger.info("Extracting bundle file for process {} [correlationId={}]", processId, correlationId);
            
            // Create temporary directory
            extractDir = Files.createTempDirectory("graph-bundle-" + processId);
            logger.debug("Created extraction directory: {} [correlationId={}]", extractDir, correlationId);
            
            // Extract the file
            extractFile(file, extractDir);
            
            // Validate the extracted structure
            validateExtractedStructure(extractDir);
            
            logger.info("Successfully extracted bundle to: {} [correlationId={}]", extractDir, correlationId);
            return extractDir;
            
        } catch (BundleProcessingException e) {
            // Clean up on failure
            if (extractDir != null) {
                cleanupTempDirectory(extractDir);
            }
            logger.error("Bundle extraction failed for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Clean up on failure
            if (extractDir != null) {
                cleanupTempDirectory(extractDir);
            }
            logger.error("Unexpected error during bundle extraction for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Failed to extract bundle: " + e.getMessage(), e, correlationId, "EXTRACTION");
        }
    }
    
    /**
     * Validates that the extracted directory structure contains required files.
     * 
     * @param extractDir The directory to validate
     * @throws BundleProcessingException if validation fails
     */
    public void validateExtractedStructure(Path extractDir) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.debug("Validating extracted directory structure: {} [correlationId={}]", extractDir, correlationId);
            
            // Check for agent_graph.dot file
            Path agentGraphFile = extractDir.resolve("agent_graph.dot");
            if (!Files.exists(agentGraphFile)) {
                throw new BundleProcessingException("Required file 'agent_graph.dot' not found in bundle", correlationId, "STRUCTURE_VALIDATION");
            }
            
            // Check for plans directory if it exists
            Path plansDir = extractDir.resolve("plans");
            if (Files.exists(plansDir) && Files.isDirectory(plansDir)) {
                validatePlanDirectories(plansDir);
            }
            
            // Check for tasks directory if it exists
            Path tasksDir = extractDir.resolve("tasks");
            if (Files.exists(tasksDir) && Files.isDirectory(tasksDir)) {
                validateTaskDirectories(tasksDir);
            }
            
            logger.debug("Directory structure validation passed for: {} [correlationId={}]", extractDir, correlationId);
            
        } catch (BundleProcessingException e) {
            logger.error("Directory structure validation failed [correlationId={}]: {}", correlationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during directory structure validation [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Unexpected error during structure validation: " + e.getMessage(), e, correlationId, "STRUCTURE_VALIDATION");
        }
    }
    
    /**
     * Cleans up temporary directory and its contents.
     * 
     * @param tempDir The temporary directory to clean up
     */
    public void cleanupTempDirectory(Path tempDir) {
        String correlationId = getOrCreateCorrelationId();
        
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                logger.debug("Starting cleanup of temporary directory: {} [correlationId={}]", tempDir, correlationId);
                
                Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete temporary file: {} [correlationId={}]", path, correlationId, e);
                        }
                    });
                    
                logger.debug("Successfully cleaned up temporary directory: {} [correlationId={}]", tempDir, correlationId);
                
            } catch (IOException e) {
                logger.error("Failed to cleanup temporary directory: {} [correlationId={}]", tempDir, correlationId, e);
                // Don't throw exception for cleanup failures - log and continue
            } catch (Exception e) {
                logger.error("Unexpected error during temporary directory cleanup: {} [correlationId={}]", tempDir, correlationId, e);
                // Don't throw exception for cleanup failures - log and continue
            }
        } else {
            logger.debug("Temporary directory does not exist or is null, skipping cleanup [correlationId={}]", correlationId);
        }
    }
    
    /**
     * Extracts an uploaded file to the specified directory.
     * 
     * @param file The uploaded file to extract
     * @param extractDir The directory to extract to
     * @throws BundleProcessingException if extraction fails
     */
    private void extractFile(MultipartFile file, Path extractDir) {
        String correlationId = getOrCreateCorrelationId();
        String filename = file.getOriginalFilename();
        
        try {
            if (filename == null) {
                throw new BundleProcessingException("File must have a name", correlationId, "EXTRACTION");
            }
            
            logger.info("Extracting file {} to directory {} [correlationId={}]", filename, extractDir, correlationId);
            
            if (filename.toLowerCase().endsWith(".zip")) {
                extractZipFile(file, extractDir);
            } else if (filename.toLowerCase().endsWith(".tar.gz") || filename.toLowerCase().endsWith(".tgz")) {
                extractTarGzFile(file, extractDir);
            } else if (filename.toLowerCase().endsWith(".tar")) {
                extractTarFile(file, extractDir);
            } else {
                throw new BundleProcessingException("Unsupported file format: " + filename, correlationId, "EXTRACTION");
            }
            
            logger.info("Successfully extracted file {} to {} [correlationId={}]", filename, extractDir, correlationId);
            
        } catch (BundleProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to extract file {} [correlationId={}]: {}", filename, correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Failed to extract file: " + e.getMessage(), e, correlationId, "EXTRACTION");
        }
    }
    
    /**
     * Extracts a ZIP file to the specified directory.
     */
    private void extractZipFile(MultipartFile file, Path extractDir) throws IOException {
        String correlationId = getOrCreateCorrelationId();
        
        try (var inputStream = file.getInputStream();
             var zipInputStream = new java.util.zip.ZipInputStream(inputStream)) {
            
            logger.debug("Starting ZIP file extraction [correlationId={}]", correlationId);
            
            java.util.zip.ZipEntry entry;
            int entryCount = 0;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryCount++;
                Path entryPath = extractDir.resolve(entry.getName()).normalize();
                
                // Security check: ensure the entry is within the extract directory
                if (!entryPath.startsWith(extractDir)) {
                    throw new IOException("Entry is outside of the target directory: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath);
                }
                zipInputStream.closeEntry();
            }
            
            logger.debug("Successfully extracted {} entries from ZIP file [correlationId={}]", entryCount, correlationId);
            
        } catch (IOException e) {
            logger.error("Failed to extract ZIP file [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Extracts a TAR.GZ file to the specified directory.
     */
    private void extractTarGzFile(MultipartFile file, Path extractDir) throws IOException {
        try (var inputStream = file.getInputStream();
             var gzipInputStream = new java.util.zip.GZIPInputStream(inputStream);
             var tarInputStream = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzipInputStream)) {
            
            extractTarStream(tarInputStream, extractDir);
        }
    }
    
    /**
     * Extracts a TAR file to the specified directory.
     */
    private void extractTarFile(MultipartFile file, Path extractDir) throws IOException {
        try (var inputStream = file.getInputStream();
             var tarInputStream = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(inputStream)) {
            
            extractTarStream(tarInputStream, extractDir);
        }
    }
    
    /**
     * Extracts entries from a TAR input stream.
     */
    private void extractTarStream(org.apache.commons.compress.archivers.tar.TarArchiveInputStream tarInputStream, 
                                 Path extractDir) throws IOException {
        org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
        while ((entry = tarInputStream.getNextTarEntry()) != null) {
            Path entryPath = extractDir.resolve(entry.getName()).normalize();
            
            // Security check: ensure the entry is within the extract directory
            if (!entryPath.startsWith(extractDir)) {
                throw new IOException("Entry is outside of the target directory: " + entry.getName());
            }
            
            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                Files.createDirectories(entryPath.getParent());
                Files.copy(tarInputStream, entryPath);
            }
        }
    }
    
    /**
     * Validates plan directories contain required plan.py files.
     */
    private void validatePlanDirectories(Path plansDir) {
        String correlationId = getOrCreateCorrelationId();
        
        try (var stream = Files.list(plansDir)) {
            logger.debug("Validating plan directories in: {} [correlationId={}]", plansDir, correlationId);
            
            stream.filter(Files::isDirectory)
                  .forEach(planDir -> {
                      Path planFile = planDir.resolve("plan.py");
                      if (!Files.exists(planFile)) {
                          throw new RuntimeException("Plan directory " + planDir.getFileName() + 
                                                   " must contain a plan.py file");
                      }
                  });
                  
            logger.debug("Plan directory validation completed [correlationId={}]", correlationId);
            
        } catch (RuntimeException e) {
            logger.error("Plan directory validation failed [correlationId={}]: {}", correlationId, e.getMessage());
            throw new BundleProcessingException(e.getMessage(), e, correlationId, "STRUCTURE_VALIDATION");
        } catch (Exception e) {
            logger.error("Unexpected error during plan directory validation [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Unexpected error during plan validation: " + e.getMessage(), e, correlationId, "STRUCTURE_VALIDATION");
        }
    }
    
    /**
     * Validates task directories contain required task.py files.
     */
    private void validateTaskDirectories(Path tasksDir) {
        String correlationId = getOrCreateCorrelationId();
        
        try (var stream = Files.list(tasksDir)) {
            logger.debug("Validating task directories in: {} [correlationId={}]", tasksDir, correlationId);
            
            stream.filter(Files::isDirectory)
                  .forEach(taskDir -> {
                      Path taskFile = taskDir.resolve("task.py");
                      if (!Files.exists(taskFile)) {
                          throw new RuntimeException("Task directory " + taskDir.getFileName() + 
                                                   " must contain a task.py file");
                      }
                  });
                  
            logger.debug("Task directory validation completed [correlationId={}]", correlationId);
            
        } catch (RuntimeException e) {
            logger.error("Task directory validation failed [correlationId={}]: {}", correlationId, e.getMessage());
            throw new BundleProcessingException(e.getMessage(), e, correlationId, "STRUCTURE_VALIDATION");
        } catch (Exception e) {
            logger.error("Unexpected error during task directory validation [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new BundleProcessingException("Unexpected error during task validation: " + e.getMessage(), e, correlationId, "STRUCTURE_VALIDATION");
        }
    }
    
    /**
     * Gets or creates a correlation ID for error tracking.
     */
    private String getOrCreateCorrelationId() {
        String mdcId = MDC.get("correlationId");
        if (mdcId != null && !mdcId.trim().isEmpty()) {
            return mdcId;
        }
        
        String newId = UUID.randomUUID().toString();
        MDC.put("correlationId", newId);
        return newId;
    }
}