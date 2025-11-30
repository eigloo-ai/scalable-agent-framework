package ai.eigloo.agentic.graph.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorFileTest {

    @Test
    @DisplayName("Should create ExecutorFile with name and contents")
    void shouldCreateExecutorFileWithNameAndContents() {
        // Given
        String name = "test.py";
        String contents = "print('hello world')";
        
        // When
        ExecutorFile file = ExecutorFile.of(name, contents);
        
        // Then
        assertEquals(name, file.fileName());
        assertEquals(contents, file.contents());
        assertEquals("1.0", file.version());
        assertNotNull(file.creationDate());
        assertNotNull(file.updateDate());
        assertEquals(file.creationDate(), file.updateDate());
    }

    @Test
    @DisplayName("Should create ExecutorFile with all fields specified")
    void shouldCreateExecutorFileWithAllFields() {
        // Given
        String name = "task.py";
        String contents = "def task(): pass";
        LocalDateTime creationDate = LocalDateTime.of(2023, 1, 1, 10, 0);
        String version = "2.1";
        LocalDateTime updateDate = LocalDateTime.of(2023, 1, 2, 15, 30);
        
        // When
        ExecutorFile file = new ExecutorFile(name, contents, creationDate, version, updateDate);
        
        // Then
        assertEquals(name, file.fileName());
        assertEquals(contents, file.contents());
        assertEquals(creationDate, file.creationDate());
        assertEquals(version, file.version());
        assertEquals(updateDate, file.updateDate());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid file names")
    void shouldThrowExceptionForInvalidFileNames(String invalidName) {
        // Given
        String contents = "some content";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> ExecutorFile.of(invalidName, contents));
    }

    @Test
    @DisplayName("Should throw exception for null contents")
    void shouldThrowExceptionForNullContents() {
        // Given
        String name = "test.py";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> ExecutorFile.of(name, null));
    }

    @Test
    @DisplayName("Should throw exception for null creation date")
    void shouldThrowExceptionForNullCreationDate() {
        // Given
        String name = "test.py";
        String contents = "content";
        String version = "1.0";
        LocalDateTime updateDate = LocalDateTime.now();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> new ExecutorFile(name, contents, null, version, updateDate));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid versions")
    void shouldThrowExceptionForInvalidVersions(String invalidVersion) {
        // Given
        String name = "test.py";
        String contents = "content";
        LocalDateTime creationDate = LocalDateTime.now();
        LocalDateTime updateDate = LocalDateTime.now();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> new ExecutorFile(name, contents, creationDate, invalidVersion, updateDate));
    }

    @Test
    @DisplayName("Should throw exception for null update date")
    void shouldThrowExceptionForNullUpdateDate() {
        // Given
        String name = "test.py";
        String contents = "content";
        LocalDateTime creationDate = LocalDateTime.now();
        String version = "1.0";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> new ExecutorFile(name, contents, creationDate, version, null));
    }



    @Test
    @DisplayName("Should trim whitespace from name and version")
    void shouldTrimWhitespaceFromNameAndVersion() {
        // Given
        String nameWithSpaces = "  test.py  ";
        String versionWithSpaces = "  1.0  ";
        String contents = "content";
        LocalDateTime now = LocalDateTime.now();
        
        // When
        ExecutorFile file = new ExecutorFile(nameWithSpaces, contents, now, versionWithSpaces, now);
        
        // Then
        assertEquals("test.py", file.fileName());
        assertEquals("1.0", file.version());
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        LocalDateTime creationDate = LocalDateTime.of(2023, 1, 1, 10, 0);
        LocalDateTime updateDate = LocalDateTime.of(2023, 1, 2, 15, 30);
        
        ExecutorFile file1 = new ExecutorFile("test.py", "content", creationDate, "1.0", updateDate);
        ExecutorFile file2 = new ExecutorFile("test.py", "content", creationDate, "1.0", updateDate);
        ExecutorFile file3 = new ExecutorFile("other.py", "content", creationDate, "1.0", updateDate);
        
        // Then
        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
        assertNotEquals(file1, file3);
        assertNotEquals(file1.hashCode(), file3.hashCode());
        
        // Test with null
        assertNotEquals(file1, null);
        
        // Test with different class
        assertNotEquals(file1, "not a file");
        
        // Test reflexivity
        assertEquals(file1, file1);
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        ExecutorFile file = ExecutorFile.of("test.py", "print('hello')");
        
        // When
        String toString = file.toString();
        
        // Then
        assertTrue(toString.contains("test.py"));
        assertTrue(toString.contains("1.0"));
        assertTrue(toString.contains("ExecutorFile"));
    }

    @Test
    @DisplayName("Should handle empty contents")
    void shouldHandleEmptyContents() {
        // Given
        String name = "empty.py";
        String emptyContents = "";
        
        // When
        ExecutorFile file = ExecutorFile.of(name, emptyContents);
        
        // Then
        assertEquals(name, file.fileName());
        assertEquals(emptyContents, file.contents());
        assertEquals("1.0", file.version());
    }
}