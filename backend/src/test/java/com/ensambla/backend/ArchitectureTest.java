package com.ensambla.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas de {@code ARQUITECTURA.md} que se pueden comprobar sin dependencias: bytecode, fuentes y classpath. */
class ArchitectureTest {

    private static final Path CLASSES = Path.of(URI.create(
            BackendApplication.class.getProtectionDomain().getCodeSource().getLocation().toString()));
    private static final Path DOMAIN_CLASSES = CLASSES.resolve("com/ensambla/backend/domain");
    // target/classes -> src/main/java
    private static final Path DOMAIN_SOURCES = CLASSES.getParent().getParent()
            .resolve("src/main/java/com/ensambla/backend/domain");
    private static final String INFRASTRUCTURE_PACKAGE = "com/ensambla/backend/infrastructure";
    private static final String SPRING_PACKAGE = "org/springframework";
    private static final Pattern UUID_REFERENCE = Pattern.compile("\\bUUID\\b");
    // Los value objects de id, su interfaz y las dos reglas que derivan UUID de un texto.
    private static final Pattern UUID_ALLOWED = Pattern.compile(
            ".*/model/.*/\\w+Id\\.java|.*/model/common/(Identificador|Huella)\\.java|.*/model/sistema/IdSistema\\.java");

    @Test
    @DisplayName("Las clases del dominio no dependen de la infraestructura")
    void domainClassesShouldNotDependOnInfrastructure() throws IOException {
        List<Path> classes = domainClasses();

        thenNoClassReferences(classes, INFRASTRUCTURE_PACKAGE);
    }

    @Test
    @DisplayName("Las clases del dominio no dependen de Spring")
    void domainClassesShouldNotDependOnSpring() throws IOException {
        List<Path> classes = domainClasses();

        thenNoClassReferences(classes, SPRING_PACKAGE);
    }

    @Test
    @DisplayName("Los casos de uso no llevan el sufijo UseCase")
    void useCasesShouldNotEndWithUseCase() throws IOException {
        List<Path> classes = domainClasses();

        assertThat(classes)
                .filteredOn(path -> path.toString().replace('\\', '/').contains("/domain/useCase/"))
                .isNotEmpty()
                .extracting(this::simpleName)
                .noneMatch(name -> name.endsWith("UseCase"));
    }

    @Test
    @DisplayName("No existen los paquetes de DTOs, mappers ni enums del dominio")
    void domainShouldNotHaveDtoMapperOrEnumsPackages() {
        assertThat(DOMAIN_CLASSES.resolve("dto")).doesNotExist();
        assertThat(DOMAIN_CLASSES.resolve("mapper")).doesNotExist();
        assertThat(DOMAIN_CLASSES.resolve("model/enums")).doesNotExist();
    }

    @Test
    @DisplayName("Dentro del dominio solo los ids tipados usan UUID")
    void onlyTypedIdsShouldUseUuidInDomain() throws IOException {
        List<Path> sources = domainSources();

        thenOnlyAllowedSourcesMentionUuid(sources);
    }

    @Test
    @DisplayName("Los tests no usan H2")
    void testsShouldNotUseH2() {
        assertThat(ArchitectureTest.class.getResource("/application-h2-test.properties")).isNull();
        assertThatThrownBy(() -> Class.forName("org.h2.Driver")).isInstanceOf(ClassNotFoundException.class);
    }

    // --- act ---
    private List<Path> domainClasses() throws IOException {
        return files(DOMAIN_CLASSES, ".class");
    }

    private List<Path> domainSources() throws IOException {
        return files(DOMAIN_SOURCES, ".java");
    }

    // --- assert ---
    private void thenNoClassReferences(List<Path> classes, String forbiddenPackage) throws IOException {
        assertThat(classes).isNotEmpty();
        for (Path type : classes) {
            assertThat(bytecodeOf(type)).as("dependencias de %s", type)
                    .doesNotContain(forbiddenPackage);
        }
    }

    private void thenOnlyAllowedSourcesMentionUuid(List<Path> sources) throws IOException {
        assertThat(sources).isNotEmpty();
        for (Path source : sources) {
            if (UUID_ALLOWED.matcher(source.toString().replace('\\', '/')).matches()) {
                continue;
            }
            assertThat(UUID_REFERENCE.matcher(Files.readString(source)).find()).as("UUID en %s", source).isFalse();
        }
    }

    // --- helpers ---
    private List<Path> files(Path root, String extension) throws IOException {
        assertThat(root).exists();
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(extension)).toList();
        }
    }

    // La tabla de constantes de una clase conserva las referencias de tipos JVM.
    private String bytecodeOf(Path type) throws IOException {
        return new String(Files.readAllBytes(type), StandardCharsets.ISO_8859_1);
    }

    // Nombre de la clase de nivel superior: sin la extensión ni las clases anidadas ($).
    private String simpleName(Path type) {
        String file = type.getFileName().toString();
        return file.substring(0, file.length() - ".class".length()).split("\\$")[0];
    }
}
