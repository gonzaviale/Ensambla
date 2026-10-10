package com.ensambla.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    private static final Path DOMAIN_CLASSES = Path.of("target/classes/com/ensambla/backend/domain");
    private static final String INFRASTRUCTURE_PACKAGE = "com/ensambla/backend/infrastructure";

    @Test
    @DisplayName("Las clases del dominio no dependen de la infraestructura")
    void domainClassesShouldNotDependOnInfrastructure() throws IOException {
        List<Path> classes = domainClasses();

        thenNoClassReferencesInfrastructure(classes);
    }

    // --- act ---
    private List<Path> domainClasses() throws IOException {
        assertThat(DOMAIN_CLASSES).exists();
        try (Stream<Path> paths = Files.walk(DOMAIN_CLASSES)) {
            return paths.filter(path -> path.toString().endsWith(".class")).toList();
        }
    }

    // --- assert ---
    private void thenNoClassReferencesInfrastructure(List<Path> classes) throws IOException {
        assertThat(classes).isNotEmpty();
        for (Path type : classes) {
            assertThat(bytecodeOf(type)).as("dependencias de %s", type)
                    .doesNotContain(INFRASTRUCTURE_PACKAGE);
        }
    }

    // --- helpers ---
    // La tabla de constantes de una clase conserva las referencias de tipos JVM.
    private String bytecodeOf(Path type) throws IOException {
        return new String(Files.readAllBytes(type), StandardCharsets.ISO_8859_1);
    }
}
