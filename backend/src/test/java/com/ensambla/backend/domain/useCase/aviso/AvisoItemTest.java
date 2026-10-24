package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoItemTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Test
    @DisplayName("Pasa el aviso al item con todos sus datos")
    void deShouldMapEveryField() {
        AvisoItem result = AvisoItem.de(givenAviso());

        assertThat(result).isEqualTo(new AvisoItem(ID, PROYECTO_ID, TipoAviso.STOCK, GrupoAviso.STOCK, (short) 3,
                EstadoAviso.PENDIENTE, "registro:1", "Te quedaste sin yerba", "No te quedan unidades",
                List.of("motivo"), List.of(Map.of("tipo", "ABRIR")), Map.of("cantidad", 0), AHORA, null));
    }

    private static Aviso givenAviso() {
        return Aviso.builder()
                .id(ID).proyectoId(PROYECTO_ID).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 3)
                .estado(EstadoAviso.PENDIENTE).entidad("registro:1").titulo("Te quedaste sin yerba")
                .detalle("No te quedan unidades").motivos(List.of("motivo"))
                .acciones(List.of(Map.of("tipo", "ABRIR"))).datos(Map.of("cantidad", 0)).creadoEn(AHORA)
                .build();
    }
}
