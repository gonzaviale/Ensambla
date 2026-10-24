package com.ensambla.backend.infrastructure.presentation.mapper.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.dto.aviso.ListarAvisosRequest;
import com.ensambla.backend.domain.dto.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoPresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID AVISO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
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

    private static AvisoItem givenItem() {
        return new AvisoItem(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK, (short) 3, EstadoAviso.PENDIENTE,
                "registro:1", "titulo", "detalle", List.of("motivo"), List.of(Map.of("tipo", "ABRIR")),
                Map.of("cantidad", 0), AHORA, null);
    }
}
