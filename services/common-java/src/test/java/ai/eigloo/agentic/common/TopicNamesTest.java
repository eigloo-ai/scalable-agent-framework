package ai.eigloo.agentic.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicNamesTest {

    @Test
    void extractTenantIdRetainsHyphenatedSuffix() {
        assertEquals("tenant-dev", TopicNames.extractTenantId("plan-inputs-tenant-dev"));
        assertEquals("tenant-dev", TopicNames.extractTenantId("task-inputs-tenant-dev"));
        assertEquals("tenant-dev", TopicNames.extractTenantId("persisted-plan-executions-tenant-dev"));
    }

    @Test
    void extractTenantIdRejectsUnknownTopic() {
        assertNull(TopicNames.extractTenantId("random-topic"));
    }

    @Test
    void validTopicNameRequiresKnownPrefixAndNonEmptyTenant() {
        assertTrue(TopicNames.isValidTopicName("plan-inputs-tenant-dev"));
        assertTrue(TopicNames.isValidTopicName("task-executions-tenant1"));
        assertFalse(TopicNames.isValidTopicName("plan-inputs-"));
        assertFalse(TopicNames.isValidTopicName("unknown-tenant-dev"));
    }
}
