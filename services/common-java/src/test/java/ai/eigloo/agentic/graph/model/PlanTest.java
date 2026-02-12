package ai.eigloo.agentic.graph.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanTest {

    @TempDir
    Path tempDir;

    private Path testPlanPath;

    @BeforeEach
    void setUp() {
        testPlanPath = tempDir.resolve("test_plan");
    }

    @Test
    void testValidConstruction() {
        Plan plan = Plan.of("test_plan", testPlanPath);

        assertThat(plan.name()).isEqualTo("test_plan");
        assertThat(plan.planSource()).isEqualTo(testPlanPath);
        assertThat(plan.files()).isEmpty();
    }

    @Test
    void testValidationInConstructor() {
        assertThatThrownBy(() -> Plan.of(null, testPlanPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan name cannot be null or empty");

        assertThatThrownBy(() -> Plan.of("", testPlanPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan name cannot be null or empty");

        assertThatThrownBy(() -> Plan.of("   ", testPlanPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan name cannot be null or empty");

        assertThatThrownBy(() -> Plan.of("test_plan", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan source cannot be null");
    }

    @Test
    void testWithFiles() {
        Plan plan = Plan.of("test_plan", testPlanPath);
        ExecutorFile file1 = ExecutorFile.of("plan.py", "def plan(): pass");
        ExecutorFile file2 = ExecutorFile.of("requirements.txt", "requests==2.28.0");

        Plan newPlan = plan.withFiles(java.util.List.of(file1, file2));

        assertThat(newPlan.name()).isEqualTo("test_plan");
        assertThat(newPlan.planSource()).isEqualTo(testPlanPath);
        assertThat(newPlan.files()).containsExactly(file1, file2);
        assertThat(plan.files()).isEmpty();
    }

    @Test
    void testWithFile() {
        Plan plan = Plan.of("test_plan", testPlanPath);
        ExecutorFile file = ExecutorFile.of("plan.py", "def plan(): pass");

        Plan newPlan = plan.withFile(file);

        assertThat(newPlan.name()).isEqualTo("test_plan");
        assertThat(newPlan.planSource()).isEqualTo(testPlanPath);
        assertThat(newPlan.files()).containsExactly(file);
        assertThat(plan.files()).isEmpty();
    }

    @Test
    void testFileImmutability() {
        ExecutorFile file = ExecutorFile.of("plan.py", "def plan(): pass");
        Plan plan = Plan.of("test_plan", testPlanPath).withFile(file);

        assertThatThrownBy(() -> plan.files().add(ExecutorFile.of("test.py", "test")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testNullFiles() {
        Plan plan = new Plan("test_plan", "test_plan", testPlanPath, null);

        assertThat(plan.files()).isEmpty();
    }
}
