package ai.eigloo.agentic.graphbuilder.parser;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.exception.GraphParsingException;
import ai.eigloo.agentic.graph.exception.GraphValidationException;

import java.nio.file.Path;

/**
 * Interface for graph specification parsers.
 * 
 * <p>This interface defines the contract for parsers that can read agent graph
 * specifications from various formats (GraphViz DOT, JSON, YAML, etc.) and convert
 * them into AgentGraph objects.</p>
 * 
 * <p>The expected directory structure for a graph specification is:</p>
 * <pre>
 * specification_directory/
 * ├── agent_graph.dot          # Graph specification file
 * ├── plans/                   # Python subprojects for plans
 * │   ├── plan1/
 * │   │   ├── plan.py
 * │   │   └── requirements.txt
 * │   └── plan2/
 * │       ├── plan.py
 * │       └── requirements.txt
 * └── tasks/                   # Python subprojects for tasks
 *     ├── task1/
 *     │   ├── task.py
 *     │   └── requirements.txt
 *     └── task2/
 *         ├── task.py
 *         └── requirements.txt
 * </pre>
 * 
 * <p>Implementations should:</p>
 * <ul>
 *   <li>Parse the specification file to extract nodes and edges</li>
 *   <li>Validate that all referenced plans and tasks have corresponding directories</li>
 *   <li>Verify that required Python files exist in each subproject</li>
 *   <li>Construct an AgentGraph with proper relationships</li>
 *   <li>Throw GraphParsingException for parsing errors</li>
 *   <li>Throw GraphValidationException for validation failures</li>
 * </ul>
 */
public interface GraphParser {
    
    /**
     * Parses a graph specification from the given directory.
     * 
     * <p>The directory should contain the specification file and subdirectories
     * for plans and tasks as described in the class documentation.</p>
     * 
     * @param specificationDirectory The directory containing the graph specification
     * @return A fully constructed and validated AgentGraph
     * @throws GraphParsingException if the specification cannot be parsed
     * @throws GraphValidationException if the graph violates integrity constraints
     */
    AgentGraph parse(Path specificationDirectory) throws GraphParsingException, GraphValidationException;
    
    /**
     * Determines if this parser can handle the given specification file.
     * 
     * <p>This method should examine the file extension, content, or other
     * characteristics to determine if the parser supports the file format.</p>
     * 
     * @param specificationFile The specification file to check
     * @return true if this parser can handle the file, false otherwise
     */
    boolean supports(Path specificationFile);
    
    /**
     * Gets the name of this parser.
     * 
     * @return A human-readable name for this parser
     */
    String getName();
    
    /**
     * Gets the file extensions supported by this parser.
     * 
     * @return Array of supported file extensions (e.g., [".dot", ".json"])
     */
    String[] getSupportedExtensions();
} 