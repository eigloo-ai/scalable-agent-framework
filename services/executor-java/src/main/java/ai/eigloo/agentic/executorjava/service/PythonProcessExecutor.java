package ai.eigloo.agentic.executorjava.service;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.executorjava.config.ExecutorPythonProperties;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.PlanResult;
import ai.eigloo.proto.model.Common.TaskInput;
import ai.eigloo.proto.model.Common.TaskResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PythonProcessExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonProcessExecutor.class);

    private final ExecutorPythonProperties pythonProperties;

    private Path runnerScriptPath;

    public PythonProcessExecutor(ExecutorPythonProperties pythonProperties) {
        this.pythonProperties = pythonProperties;
    }

    @PostConstruct
    public void initializeRunnerScript() throws IOException {
        Path runnerDir = resolveWorkingRootPath().resolve("runner");
        Files.createDirectories(runnerDir);

        runnerScriptPath = runnerDir.resolve("executor_runner.py");
        ClassPathResource resource = new ClassPathResource("python/executor_runner.py");
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, runnerScriptPath, StandardCopyOption.REPLACE_EXISTING);
        }
        logger.info("Initialized python runner at {}", runnerScriptPath);
    }

    public Path resolveWorkingRootPath() {
        String configured = pythonProperties.getWorkingRoot();
        if (configured == null || configured.isBlank()) {
            return Path.of(System.getProperty("java.io.tmpdir"), "executor-java");
        }
        String resolved = configured.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
        return Path.of(resolved);
    }

    public PlanResult executePlan(Path scriptPath, PlanInput planInput, String tenantId, Path workingDirectory) {
        byte[] resultBytes = runPython(
                "plan",
                scriptPath,
                planInput.toByteArray(),
                tenantId,
                workingDirectory
        );
        PlanResult result = ProtobufUtils.deserializePlanResult(resultBytes);
        if (result == null) {
            throw new IllegalStateException("Python runner returned an invalid PlanResult payload");
        }
        return result;
    }

    public TaskResult executeTask(Path scriptPath, TaskInput taskInput, String tenantId, Path workingDirectory) {
        byte[] resultBytes = runPython(
                "task",
                scriptPath,
                taskInput.toByteArray(),
                tenantId,
                workingDirectory
        );
        TaskResult result = ProtobufUtils.deserializeTaskResult(resultBytes);
        if (result == null) {
            throw new IllegalStateException("Python runner returned an invalid TaskResult payload");
        }
        return result;
    }

    private byte[] runPython(
            String mode,
            Path scriptPath,
            byte[] inputMessage,
            String tenantId,
            Path workingDirectory) {
        Duration timeout = Duration.ofSeconds(Math.max(1, pythonProperties.getTimeoutSeconds()));
        List<String> command = new ArrayList<>();
        command.add(pythonProperties.getCommand());
        command.add(runnerScriptPath.toString());
        command.add("--mode");
        command.add(mode);
        command.add("--script");
        command.add(scriptPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        configureEnvironment(processBuilder, workingDirectory, tenantId);

        String encodedInput = Base64.getEncoder().encodeToString(inputMessage);
        try {
            Process process = processBuilder.start();
            CompletableFuture<byte[]> stdoutFuture = CompletableFuture.supplyAsync(() -> readAllBytes(process.getInputStream()));
            CompletableFuture<byte[]> stderrFuture = CompletableFuture.supplyAsync(() -> readAllBytes(process.getErrorStream()));

            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(encodedInput.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Python process timed out after " + timeout.getSeconds() + " seconds");
            }

            byte[] stdout = awaitOutput(stdoutFuture);
            byte[] stderr = awaitOutput(stderrFuture);
            String stdoutText = new String(stdout, StandardCharsets.UTF_8).trim();
            String stderrText = new String(stderr, StandardCharsets.UTF_8).trim();

            if (process.exitValue() != 0) {
                throw new IllegalStateException("Python runner failed: " + stderrText);
            }
            if (stdoutText.isBlank()) {
                throw new IllegalStateException("Python runner produced empty output");
            }

            return Base64.getDecoder().decode(stdoutText);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to execute python process", e);
        }
    }

    private void configureEnvironment(ProcessBuilder processBuilder, Path workingDirectory, String tenantId) {
        String existingPythonPath = processBuilder.environment().get("PYTHONPATH");
        Path commonPy = resolveCommonPyPath();

        StringBuilder pythonPath = new StringBuilder();
        pythonPath.append(commonPy.toString());
        pythonPath.append(System.getProperty("path.separator"));
        pythonPath.append(workingDirectory);
        if (existingPythonPath != null && !existingPythonPath.isBlank()) {
            pythonPath.append(System.getProperty("path.separator"));
            pythonPath.append(existingPythonPath);
        }

        processBuilder.environment().put("PYTHONPATH", pythonPath.toString());
        processBuilder.environment().put("TENANT_ID", tenantId);
    }

    private Path resolveCommonPyPath() {
        String configured = pythonProperties.getCommonPyPath();
        if (configured == null || configured.isBlank()) {
            return Path.of("services/common-py").toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static byte[] readAllBytes(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read process stream", e);
        }
    }

    private static byte[] awaitOutput(CompletableFuture<byte[]> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading process output", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to read process output", e);
        }
    }
}
