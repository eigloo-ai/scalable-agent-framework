package ai.eigloo.agentic.graph.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphRunStatusTest {

    @Test
    void canTransitionTo_shouldAllowDeclaredRunTransitions() {
        assertTrue(GraphRunStatus.QUEUED.canTransitionTo(GraphRunStatus.QUEUED));
        assertTrue(GraphRunStatus.QUEUED.canTransitionTo(GraphRunStatus.RUNNING));
        assertTrue(GraphRunStatus.QUEUED.canTransitionTo(GraphRunStatus.FAILED));
        assertTrue(GraphRunStatus.QUEUED.canTransitionTo(GraphRunStatus.CANCELED));

        assertTrue(GraphRunStatus.RUNNING.canTransitionTo(GraphRunStatus.RUNNING));
        assertTrue(GraphRunStatus.RUNNING.canTransitionTo(GraphRunStatus.SUCCEEDED));
        assertTrue(GraphRunStatus.RUNNING.canTransitionTo(GraphRunStatus.FAILED));
        assertTrue(GraphRunStatus.RUNNING.canTransitionTo(GraphRunStatus.CANCELED));
    }

    @Test
    void canTransitionTo_shouldRejectIllegalRunTransitions() {
        assertFalse(GraphRunStatus.QUEUED.canTransitionTo(GraphRunStatus.SUCCEEDED));
        assertFalse(GraphRunStatus.SUCCEEDED.canTransitionTo(GraphRunStatus.RUNNING));
        assertFalse(GraphRunStatus.FAILED.canTransitionTo(GraphRunStatus.RUNNING));
        assertFalse(GraphRunStatus.CANCELED.canTransitionTo(GraphRunStatus.RUNNING));
        assertFalse(GraphRunStatus.RUNNING.canTransitionTo(null));
    }

    @Test
    void isTerminal_shouldIdentifyTerminalStates() {
        assertFalse(GraphRunStatus.QUEUED.isTerminal());
        assertFalse(GraphRunStatus.RUNNING.isTerminal());
        assertTrue(GraphRunStatus.SUCCEEDED.isTerminal());
        assertTrue(GraphRunStatus.FAILED.isTerminal());
        assertTrue(GraphRunStatus.CANCELED.isTerminal());
    }
}
