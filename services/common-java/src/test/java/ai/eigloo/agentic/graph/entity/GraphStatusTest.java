package ai.eigloo.agentic.graph.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphStatusTest {

    @Test
    void canTransitionTo_shouldAllowDeclaredLifecycleTransitions() {
        assertTrue(GraphStatus.NEW.canTransitionTo(GraphStatus.NEW));
        assertTrue(GraphStatus.NEW.canTransitionTo(GraphStatus.ACTIVE));
        assertTrue(GraphStatus.NEW.canTransitionTo(GraphStatus.ARCHIVED));
        assertTrue(GraphStatus.ACTIVE.canTransitionTo(GraphStatus.ARCHIVED));
        assertTrue(GraphStatus.ACTIVE.canTransitionTo(GraphStatus.ACTIVE));
        assertTrue(GraphStatus.ARCHIVED.canTransitionTo(GraphStatus.ARCHIVED));
    }

    @Test
    void canTransitionTo_shouldRejectIllegalLifecycleTransitions() {
        assertFalse(GraphStatus.ACTIVE.canTransitionTo(GraphStatus.NEW));
        assertFalse(GraphStatus.ARCHIVED.canTransitionTo(GraphStatus.NEW));
        assertFalse(GraphStatus.ARCHIVED.canTransitionTo(GraphStatus.ACTIVE));
        assertFalse(GraphStatus.NEW.canTransitionTo(null));
    }
}
