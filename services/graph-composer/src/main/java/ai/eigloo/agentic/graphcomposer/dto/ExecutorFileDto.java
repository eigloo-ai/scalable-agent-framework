package ai.eigloo.agentic.graphcomposer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for ExecutorFile operations.
 * Represents a file associated with a plan or task in the Agent Graph.
 */
public class ExecutorFileDto {
    
    @NotBlank(message = "File name cannot be blank")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String name;
    
    @Size(max = 1048576, message = "File contents cannot exceed 1MB")
    private String contents;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime creationDate;
    
    @Size(max = 50, message = "Version cannot exceed 50 characters")
    private String version;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateDate;

    public ExecutorFileDto() {
    }

    public ExecutorFileDto(String name, String contents, LocalDateTime creationDate, String version, LocalDateTime updateDate) {
        this.name = name;
        this.contents = contents;
        this.creationDate = creationDate;
        this.version = version;
        this.updateDate = updateDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }
}