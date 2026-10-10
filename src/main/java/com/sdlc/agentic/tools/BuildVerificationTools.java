package com.sdlc.agentic.tools;

import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BuildVerificationTools {

    public boolean verifySyntax(String javaCode) {
        // Basic syntax checks
        if (javaCode == null || javaCode.trim().isEmpty()) {
            return false;
        }

        // Check for balanced braces
        long openBraces = javaCode.chars().filter(c -> c == '{').count();
        long closeBraces = javaCode.chars().filter(c -> c == '}').count();
        if (openBraces != closeBraces) {
            return false;
        }

        // Check for balanced parentheses
        long openParens = javaCode.chars().filter(c -> c == '(').count();
        long closeParens = javaCode.chars().filter(c -> c == ')').count();
        if (openParens != closeParens) {
            return false;
        }

        // Check for class declaration
        Pattern classPattern = Pattern.compile("class\\s+\\w+");
        if (!classPattern.matcher(javaCode).find()) {
            return false;
        }

        // Check for package declaration
        Pattern packagePattern = Pattern.compile("package\\s+[\\w.]+;");
        if (!packagePattern.matcher(javaCode).find()) {
            return false;
        }

        return true;
    }

    public boolean compileCode(String javaCode, Path outputDir) {
        try {
            // Create temporary file for compilation
            Path tempFile = Files.createTempFile("temp_", ".java");
            Files.writeString(tempFile, javaCode);

            // Get Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                // Fallback to syntax check if compiler not available
                Files.deleteIfExists(tempFile);
                return verifySyntax(javaCode);
            }

            // Setup compilation
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            List<File> compilationUnits = new ArrayList<>();
            compilationUnits.add(tempFile.toFile());

            // Create output directory if it doesn't exist
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Iterable<String> options = Arrays.asList(
                "-d", outputDir.toString(),
                "-classpath", System.getProperty("java.class.path")
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, 
                fileManager.getJavaFileObjectsFromFiles(compilationUnits)
            );

            boolean success = task.call();
            fileManager.close();
            Files.deleteIfExists(tempFile);

            return success;

        } catch (IOException e) {
            return false;
        }
    }

    public List<String> getCompilationErrors(String javaCode, Path outputDir) {
        List<String> errors = new ArrayList<>();

        try {
            Path tempFile = Files.createTempFile("temp_", ".java");
            Files.writeString(tempFile, javaCode);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                Files.deleteIfExists(tempFile);
                errors.add("Java compiler not available in current environment");
                return errors;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            List<File> compilationUnits = new ArrayList<>();
            compilationUnits.add(tempFile.toFile());

            Iterable<String> options = Arrays.asList(
                "-d", outputDir.toString(),
                "-classpath", System.getProperty("java.class.path")
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, 
                fileManager.getJavaFileObjectsFromFiles(compilationUnits)
            );

            task.call();
            fileManager.close();
            Files.deleteIfExists(tempFile);

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(String.format("Line %d: %s", 
                        diagnostic.getLineNumber(), diagnostic.getMessage(null)));
                }
            }

        } catch (IOException e) {
            errors.add("Compilation failed: " + e.getMessage());
        }

        return errors;
    }

    public boolean verifyCodeQuality(String javaCode) {
        // Code quality checks
        int qualityScore = 100;

        // Check for proper exception handling
        if (javaCode.contains("catch (Exception e) {}") || 
            javaCode.contains("catch(Exception e){}")) {
            qualityScore -= 20;
        }

        // Check for hardcoded values
        if (javaCode.matches(".*\"[a-zA-Z0-9+/=]{20,}\".*")) {
            qualityScore -= 10;
        }

        // Check for System.out.println (should use logging)
        if (javaCode.contains("System.out.println")) {
            qualityScore -= 10;
        }

        // Check for printStackTrace
        if (javaCode.contains("printStackTrace")) {
            qualityScore -= 15;
        }

        // Check for proper method documentation
        if (!javaCode.contains("/**")) {
            qualityScore -= 15;
        }

        return qualityScore >= 70;
    }
}
