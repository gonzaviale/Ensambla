package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IAvisoJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private IAvisoJpaRepository avisos;

    private UUID negocioA;
    private UUID negocioB;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    @Test
    @DisplayName("Trae de una vez los avisos pedidos del negocio y deja afuera los que no pidió")
    void findByIdInAndNegocioIdShouldReturnOnlyTheRequestedOnes() {
        UUID uno = givenAviso(negocioA, "uno");
        UUID dos = givenAviso(negocioA, "dos");
        givenAviso(negocioA, "tres");

        List<AvisoEntity> result = avisos.findByIdInAndNegocioId(List.of(uno, dos), negocioA);

        assertThat(result).extracting(AvisoEntity::getId).containsExactlyInAnyOrder(uno, dos);
    }

    @Test
    @DisplayName("No trae un aviso de otro negocio aunque se pida por su id")
    void findByIdInAndNegocioIdShouldNotReturnAvisosOfAnotherBusiness() {
        UUID propio = givenAviso(negocioA, "propio");
        UUID ajeno = givenAviso(negocioB, "ajeno");

        List<AvisoEntity> result = avisos.findByIdInAndNegocioId(List.of(propio, ajeno), negocioA);

        assertThat(result).extracting(AvisoEntity::getId).containsExactly(propio);
    }

    @Test
    @DisplayName("Cuenta los avisos en el estado pedido de cada negocio y deja afuera los de otros estados")
    void contarPorNegocioShouldCountOnlyTheRequestedStatePerBusiness() {
        givenAviso(negocioA, "uno");
        givenAviso(negocioA, "dos");
        givenAviso(negocioB, "tres");
        givenAviso(negocioB, "vencido", EstadoAviso.VENCIDO);

        List<IAvisoJpaRepository.AvisosPorNegocio> result = avisos.contarPorNegocio(
                List.of(negocioA, negocioB), EstadoAviso.PENDIENTE);

        assertThat(result).extracting(IAvisoJpaRepository.AvisosPorNegocio::getNegocioId,
                        IAvisoJpaRepository.AvisosPorNegocio::getCantidad)
                .containsExactlyInAnyOrder(tuple(negocioA, 2L), tuple(negocioB, 1L));
    }

    @Test
    @DisplayName("No cuenta los avisos de un negocio que no se pidió")
    void contarPorNegocioShouldIgnoreBusinessesNotRequested() {
        givenAviso(negocioA, "propio");
        givenAviso(negocioB, "ajeno");

        List<IAvisoJpaRepository.AvisosPorNegocio> result = avisos.contarPorNegocio(
                List.of(negocioA), EstadoAviso.PENDIENTE);

        assertThat(result).extracting(IAvisoJpaRepository.AvisosPorNegocio::getNegocioId).containsExactly(negocioA);
    }

    // --- helpers ---
    private UUID givenAviso(UUID negocioId, String nombre) {
        return givenAviso(negocioId, nombre, EstadoAviso.PENDIENTE);
    }

    private UUID givenAviso(UUID negocioId, String nombre, EstadoAviso estado) {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 2)
                .entidad("registro:" + nombre).huella("STOCK:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(List.of()).estado(estado)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }
}
