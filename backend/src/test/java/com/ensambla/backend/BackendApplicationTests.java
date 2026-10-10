package com.ensambla.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@ActiveProfiles("h2-test")
class BackendApplicationTests {

    @Test
    @DisplayName("El contexto de Spring arranca con el main real y la base H2 aislada")
    void contextShouldLoadWhenApplicationStarts() {
        // Ejecuta el main real y verifica el arranque completo con la base aislada H2.
    }
}
