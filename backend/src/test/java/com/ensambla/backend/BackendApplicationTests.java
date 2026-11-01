package com.ensambla.backend;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class BackendApplicationTests extends PostgresRepositoryTestSupport {

    @Test
    @DisplayName("El contexto de Spring arranca con el main real y PostgreSQL en Testcontainers")
    void contextShouldLoadWhenApplicationStarts() {
        // Ejecuta el main real y verifica el arranque completo contra PostgreSQL en Testcontainers.
    }
}
