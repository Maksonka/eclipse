package com.example.shadowvibe.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WhisperService {

    private final String python;
    private final String script;

    public WhisperService(@Value("${app.whisper.python:python}") String python,
                          @Value("${app.whisper.script:whisper/whisper_local.py}") String script) {
        this.python = python;
        this.script = script;
    }

    public boolean isAvailable() {
        return Files.exists(Path.of(script));
    }

    public synchronized String transcribe(byte[] wavBytes) {
        String base = "svtrans_" + System.nanoTime();
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path inFile = tmpDir.resolve(base + ".wav");
        try {
            Files.write(inFile, wavBytes);
            ProcessBuilder pb = new ProcessBuilder(python, script, inFile.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Распознавание превысило лимит времени");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Ошибка распознавания (код " + process.exitValue() + ")");
            }
            List<String> lines = new String(out, StandardCharsets.UTF_8).lines()
                    .filter(l -> !l.isBlank())
                    .toList();
            if (lines.isEmpty()) {
                return "";
            }
            return lines.get(lines.size() - 1).trim();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при распознавании речи: " + e.getMessage(), e);
        } finally {
            try { Files.deleteIfExists(inFile); } catch (IOException ignored) {}
        }
    }
}