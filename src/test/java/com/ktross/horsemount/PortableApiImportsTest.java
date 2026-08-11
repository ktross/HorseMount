package com.ktross.horsemount;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PortableApiImportsTest {

    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?(?:"
                    + "io\\.papermc\\."
                    + "|com\\.destroystokyo\\.paper\\."
                    + "|org\\.spigotmc\\."
                    + "|net\\.md_5\\."
                    + "|org\\.bukkit\\.craftbukkit\\."
                    + "|net\\.minecraft\\.)");

    @Test
    void mainSourcesDoNotImportServerSpecificApis() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        List<String> violations = new ArrayList<>();

        try (var sourcePaths = Files.walk(sourceRoot)) {
            for (Path sourcePath : sourcePaths.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(sourcePath);
                for (int index = 0; index < lines.size(); index++) {
                    if (FORBIDDEN_IMPORT.matcher(lines.get(index)).find()) {
                        violations.add(sourcePath + ":" + (index + 1) + ": " + lines.get(index).trim());
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "The universal JAR cannot import server-specific APIs:\n"
                        + String.join("\n", violations));
    }
}
