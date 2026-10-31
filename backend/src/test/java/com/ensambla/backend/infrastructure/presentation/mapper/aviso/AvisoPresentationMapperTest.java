package com.ensambla.backend.infrastructure.presentation.mapper.aviso;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoAvisos;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoDeAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ImpactoAvisosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final AvisoPresentationMapper mapper = new AvisoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listado con el usuario y el estado opcional")
    void toListarRequestShouldCarryUsuarioAndEstado() {
        assertThat(mapper.toListarRequest(USUARIO_ID, EstadoAviso.PENDIENTE))
                .isEqualTo(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.PENDIENTE));
    }

    @Test
    @DisplayName("Arma el pedido de aplicar o descartar con el usuario y el aviso")
    void toResolverRequestShouldCarryUsuarioAndAviso() {
        assertThat(mapper.toResolverRequest(USUARIO_ID, AVISO_ID))
                .isEqualTo(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    @Test
    @DisplayName("Pasa el aviso a la respuesta con todos sus datos")
    void toResponseShouldMapEveryField() {
        AvisoResponse result = mapper.toResponse(givenItem());

        assertThat(result).isEqualTo(new AvisoResponse(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK,
                (short) 3, EstadoAviso.PENDIENTE, "registro:1", "titulo", "detalle", List.of("motivo"),
                List.of(Map.of("tipo", "ABRIR")), Map.of("cantidad", 0), AHORA, null));
    }

    @Test
    @DisplayName("Pasa una lista de avisos conservando el orden")
    void toResponseShouldMapList() {
        assertThat(mapper.toResponse(List.of(givenItem()))).extracting(AvisoResponse::id).containsExactly(AVISO_ID);
    }

    @Test
    @DisplayName("Pasa el contador a la respuesta")
    void toResponseShouldMapCounter() {
        assertThat(mapper.toResponse(new ContadorAvisos(5, 2))).isEqualTo(new ContadorAvisosResponse(5, 2));
    }

    @Test
    @DisplayName("Pasa el impacto a la respuesta con el cambio de ingreso semanal de cada aviso medido")
    void toResponseShouldMapImpact() {
        var impacto = new ImpactoAviso(14, new BigDecimal("1.00"), new BigDecimal("2.00"),
                new BigDecimal("10.00"), new BigDecimal("20.00"));
        var medido = new ImpactoDeAviso(AVISO_ID, TipoAviso.ESTANCADO, "Mermelada cara", AHORA, impacto);

        ImpactoAvisosResponse result = mapper.toResponse(new ImpactoAvisos(3, 1, new BigDecimal("70.00"), List.of(medido)));

        assertThat(result).isEqualTo(new ImpactoAvisosResponse(3, 1, new BigDecimal("70.00"), List.of(
                new ImpactoAvisosResponse.Medido(AVISO_ID, TipoAviso.ESTANCADO, "Mermelada cara", AHORA, 14,
                        new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("10.00"),
                        new BigDecimal("20.00"), new BigDecimal("70.00")))));
    }

    private static AvisoItem givenItem() {
        return new AvisoItem(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK, (short) 3, EstadoAviso.PENDIENTE,
                "registro:1", "titulo", "detalle", List.of("motivo"), List.of(Map.of("tipo", "ABRIR")),
                Map.of("cantidad", 0), AHORA, null);
    }
}
