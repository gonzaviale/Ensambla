package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RegistroSuscripcionRollbackPostgresTest extends PostgresRepositoryTestSupport {

    private static final String EMAIL = "fallo-suscripcion@example.com";
    private static final String NEGOCIO = "Negocio que debe revertirse";

    @Autowired
    private RegistrarUsuario registrarUsuario;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private SuscripcionRepository suscripcionRepository;

    private Throwable fallo;

    @Test
    @DisplayName("Si falla la suscripción se revierten el usuario y el negocio en PostgreSQL")
    void siFallaLaSuscripcionSeRevierteElUsuarioYElNegocioEnPostgresql() {
        dadoQueGuardarLaSuscripcionFalla();

        cuandoSeRegistraUnaCuenta();

        entoncesSeRevierteLaCuentaCompleta();
    }

    // --- dado ---

    private void dadoQueGuardarLaSuscripcionFalla() {
        when(suscripcionRepository.save(any())).thenThrow(new DataIntegrityViolationException("Fallo de prueba"));
    }

    // --- cuando ---

    private void cuandoSeRegistraUnaCuenta() {
        fallo = catchThrowable(() -> registrarUsuario.execute(
                new RegistrarUsuarioRequest("Persona", NEGOCIO, EMAIL, "ClaveSegura123!")));
    }

    // --- entonces ---

    private void entoncesSeRevierteLaCuentaCompleta() {
        assertThat(fallo).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(usuarios.findByEmail(EMAIL)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM negocio WHERE nombre = ?", Long.class, NEGOCIO)).isZero();
    }
}
