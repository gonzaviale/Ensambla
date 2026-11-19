package com.ensambla.backend;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class BackendApplicationTests extends PostgresRepositoryTestSupport {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("El contexto de Spring arranca con el main real y PostgreSQL en Testcontainers")
    void elContextoDeSpringArrancaConElMainRealYPostgresqlEnTestcontainers() {
        entoncesElContextoQuedoCargado();
    }

    // --- entonces ---

    private void entoncesElContextoQuedoCargado() {
        assertThat(contexto.getBeanDefinitionCount()).isPositive();
    }
}
