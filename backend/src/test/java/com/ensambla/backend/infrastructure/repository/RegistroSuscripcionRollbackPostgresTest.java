package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RegistroSuscripcionRollbackPostgresTest extends PostgresRepositoryTestSupport {

    private static final String EMAIL = "fallo-suscripcion@example.com";
    private static final String NEGOCIO = "Negocio que debe revertirse";
    private static final OffsetDateTime INSTANTE = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Autowired
    private RegistroService registroService;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private SuscripcionRepository suscripcionRepository;

    @Test
    @DisplayName("Si falla la suscripción se revierten el usuario y el negocio en PostgreSQL")
    void registrarShouldRollbackAccountWhenSubscriptionFails() {
        when(suscripcionRepository.save(any())).thenThrow(new DataIntegrityViolationException("Fallo de prueba"));

        assertThatThrownBy(() -> registroService.registrar(
                new RegistrarUsuarioRequest("Persona", NEGOCIO, EMAIL, "ClaveSegura123!"), INSTANTE))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(usuarios.findByEmail(EMAIL)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM negocio WHERE nombre = ?", Long.class, NEGOCIO)).isZero();
    }
}
