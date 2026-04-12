package com.daniphord.mahanga.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PythonAiService {

    private static final Logger logger = LoggerFactory.getLogger(PythonAiService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final URI baseUri;
    private final Path projectRoot;
    private final Path sidecarScriptPath;
    private final Path helperScriptPath;
    private final Object sidecarMonitor = new Object();

    private volatile Process managedSidecarProcess;

    public PythonAiService(
            ObjectMapper objectMapper,
            @Value("${froms.ai.base-url:http://127.0.0.1:8091}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.baseUri = URI.create(this.baseUrl);
        this.projectRoot = Path.of("").toAbsolutePath().normalize();
        this.sidecarScriptPath = projectRoot.resolve("python_ai_service").resolve("app.py");
        this.helperScriptPath = projectRoot.resolve("scripts").resolve("run-ai-service.cmd");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Optional<Map<String, Object>> health() {
        Optional<Map<String, Object>> response = getJson("/health");
        if (response.isPresent()) {
            return response;
        }
        ensureLocalSidecarRunning();
        return getJson("/health");
    }

    public Optional<Map<String, Object>> recommendIncident(Map<String, Object> payload) {
        Optional<Map<String, Object>> response = postJson("/recommend", payload);
        if (response.isPresent()) {
            return response;
        }
        ensureLocalSidecarRunning();
        return postJson("/recommend", payload);
    }

    public Optional<Map<String, Object>> analyzeSystemAudit(
            Map<String, Object> snapshot,
            List<Map<String, Object>> checks,
            Map<String, Object> runtimeStatus
    ) {
        Optional<Map<String, Object>> response = postJson("/system-audit", Map.of(
                "snapshot", snapshot,
                "checks", checks,
                "runtimeStatus", runtimeStatus
        ));
        if (response.isPresent()) {
            return response;
        }
        ensureLocalSidecarRunning();
        return postJson("/system-audit", Map.of(
                "snapshot", snapshot,
                "checks", checks,
                "runtimeStatus", runtimeStatus
        ));
    }

    @PreDestroy
    void stopManagedSidecar() {
        Process process = managedSidecarProcess;
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void ensureLocalSidecarRunning() {
        if (!canManageLocalSidecar()) {
            return;
        }
        if (probeHealth().isPresent()) {
            return;
        }

        synchronized (sidecarMonitor) {
            if (probeHealth().isPresent()) {
                return;
            }
            if (managedSidecarProcess != null && managedSidecarProcess.isAlive()) {
                waitForSidecar();
                return;
            }
            try {
                managedSidecarProcess = startLocalSidecarProcess();
                waitForSidecar();
            } catch (Exception exception) {
                logger.warn("Unable to start local Python AI sidecar from {}", sidecarScriptPath, exception);
            }
        }
    }

    private Optional<Map<String, Object>> probeHealth() {
        return getJson("/health");
    }

    private Process startLocalSidecarProcess() throws Exception {
        List<String> command = launchCommand();
        if (command.isEmpty()) {
            throw new IllegalStateException("No Python launcher or helper script is available");
        }

        Path logDirectory = projectRoot.resolve("logs");
        Files.createDirectories(logDirectory);
        File logFile = logDirectory.resolve("python-ai-sidecar.log").toFile();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(projectRoot.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        processBuilder.environment().put("FROMS_AI_HOST", resolvedHost());
        processBuilder.environment().put("FROMS_AI_PORT", Integer.toString(resolvedPort()));

        logger.info("Starting local Python AI sidecar using {}", String.join(" ", command));
        return processBuilder.start();
    }

    private void waitForSidecar() {
        long deadline = System.nanoTime() + Duration.ofSeconds(6).toNanos();
        while (System.nanoTime() < deadline) {
            if (probeHealth().isPresent()) {
                return;
            }
            Process process = managedSidecarProcess;
            if (process != null && !process.isAlive()) {
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private List<String> launchCommand() {
        if (isWindows() && Files.exists(helperScriptPath)) {
            return List.of("cmd", "/c", helperScriptPath.toString());
        }
        if (Files.exists(sidecarScriptPath)) {
            if (isWindows()) {
                return List.of("py", "-3", sidecarScriptPath.toString());
            }
            return List.of("python3", sidecarScriptPath.toString());
        }
        return List.of();
    }

    private boolean canManageLocalSidecar() {
        String host = resolvedHost();
        return Files.exists(sidecarScriptPath)
                && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "::1".equals(host));
    }

    private String resolvedHost() {
        String host = baseUri.getHost();
        if (host == null || host.isBlank()) {
            return "127.0.0.1";
        }
        return host;
    }

    private int resolvedPort() {
        return baseUri.getPort() > 0 ? baseUri.getPort() : 8091;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Optional<Map<String, Object>> getJson(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(response.body(), new TypeReference<>() {}));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> postJson(String path, Object payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(response.body(), new TypeReference<>() {}));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8091";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
