package ai.eigloo.agentic.graphbuilder;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for loading test resources from the classpath.
 */
public class TestResourceUtils {
    
    /**
     * Loads a resource from the classpath and returns its Path.
     * 
     * @param resourcePath the resource path relative to the test resources directory
     * @return the Path to the resource
     * @throws IllegalArgumentException if the resource is not found
     */
    public static Path getResourcePath(String resourcePath) {
        URL resourceUrl = TestResourceUtils.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try {
            return Paths.get(resourceUrl.toURI());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert resource URL to Path: " + resourcePath, e);
        }
    }
} 