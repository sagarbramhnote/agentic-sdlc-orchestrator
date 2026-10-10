package com.sdlc.agentic.tools;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

@Component
public class FileSystemTools {

    public void createSandbox(Path path) throws IOException {
        Files.createDirectories(path);
    }

    public void writeFile(Path dir, String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void rollbackSandbox(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                     .sorted(Comparator.reverseOrder())
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                         } catch (IOException ignored) {}
                     });
            }
        } catch (IOException ignored) {}
    }
}
