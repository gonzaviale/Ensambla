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

    // --- helpers ---
    private UUID givenAviso(UUID negocioId, String nombre) {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 2)
                .entidad("registro:" + nombre).huella("STOCK:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(List.of()).estado(EstadoAviso.PENDIENTE)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }
}
