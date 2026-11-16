package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CartelesIaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime NOVIEMBRE = OffsetDateTime.parse("2026-11-16T12:00:00Z");
    private static final OffsetDateTime DICIEMBRE = OffsetDateTime.parse("2026-12-02T12:00:00Z");
    private static final int CUPO_GRATUITO = 10;

    @Autowired
    private ControlPlanService controlPlanService;

    @Autowired
    private SuscripcionesDePrueba suscripciones;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    private NegocioId negocio;

    @Test
    @DisplayName("El plan gratuito admite 10 carteles con IA por mes y al undécimo responde el error propio del cupo")
    void gratuitoShouldAllowTenAndRejectTheEleventh() {
        givenUnNegocioGratuito();

        whenSeRegistranCarteles(CUPO_GRATUITO);

        assertThatThrownBy(() -> controlPlanService.exigirCupoDeCartelesIa(negocio, NOVIEMBRE))
                .isInstanceOf(RestriccionPlanException.class);
        assertThatThrownBy(() -> controlPlanService.registrarCartelIa(negocio, NOVIEMBRE))
                .isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("El cupo empieza de nuevo cada mes")
    void cupoShouldRestartEveryMonth() {
        givenUnNegocioGratuito();
        whenSeRegistranCarteles(CUPO_GRATUITO);

        assertThatCode(() -> controlPlanService.exigirCupoDeCartelesIa(negocio, DICIEMBRE))
                .doesNotThrowAnyException();
    }

    private void givenUnNegocioGratuito() {
        UUID usuarioId = usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(NOVIEMBRE)
                .actualizadoEn(NOVIEMBRE)
                .build()).getId();
        UUID negocioId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(NOVIEMBRE)
                .actualizadoEn(NOVIEMBRE)
                .build()).getId();
        negocio = NegocioId.de(negocioId);
        suscripciones.asignarGratuito(negocio, NOVIEMBRE.minusDays(20));
    }

    private void whenSeRegistranCarteles(int cantidad) {
        IntStream.range(0, cantidad).forEach(numero -> controlPlanService.registrarCartelIa(negocio, NOVIEMBRE));
    }
}
