package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final AvisoRepositoryMapper mapper = new AvisoRepositoryMapper();

    @Test
    @DisplayName("Pasa el aviso de la entidad al dominio sin perder datos")
    void toDomainShouldMapEveryField() {
        Aviso result = mapper.toDomain(givenEntity());

        assertThat(result).usingRecursiveComparison().isEqualTo(givenAviso());
    }

    @Test
    @DisplayName("Pasa el aviso del dominio a la entidad sin perder datos")
    void toEntityShouldMapEveryField() {
        AvisoEntity result = mapper.toEntity(givenAviso());

        assertThat(result).usingRecursiveComparison().isEqualTo(givenEntity());
    }

    private static AvisoEntity givenEntity() {
        return AvisoEntity.builder()
                .id(ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoAviso.STOCK)
                .grupo(GrupoAviso.STOCK).prioridad((short) 3).entidad("registro:1").huella("STOCK:registro:1:ab")
                .datos(Map.of("cantidad", 0)).titulo("Te quedaste sin yerba").detalle("No te quedan unidades")
                .motivos(List.of("motivo")).acciones(List.of(Map.of("tipo", "ABRIR")))
                .estado(EstadoAviso.DESCARTADO).resueltoEn(AHORA).creadoEn(AHORA).actualizadoEn(AHORA)
                .build();
    }

    private static Aviso givenAviso() {
        return Aviso.builder()
                .id(ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoAviso.STOCK)
                .grupo(GrupoAviso.STOCK).prioridad((short) 3).entidad("registro:1").huella("STOCK:registro:1:ab")
                .datos(Map.of("cantidad", 0)).titulo("Te quedaste sin yerba").detalle("No te quedan unidades")
                .motivos(List.of("motivo")).acciones(List.of(Map.of("tipo", "ABRIR")))
                .estado(EstadoAviso.DESCARTADO).resueltoEn(AHORA).creadoEn(AHORA).actualizadoEn(AHORA)
                .build();
    }
}
