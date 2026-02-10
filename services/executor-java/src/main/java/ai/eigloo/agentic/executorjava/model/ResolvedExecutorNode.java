package ai.eigloo.agentic.executorjava.model;

import java.util.List;

public record ResolvedExecutorNode(
        NodeType nodeType,
        String graphId,
        String lifetimeId,
        String nodeName,
        String scriptFileName,
        List<ExecutorFilePayload> files) {
}
