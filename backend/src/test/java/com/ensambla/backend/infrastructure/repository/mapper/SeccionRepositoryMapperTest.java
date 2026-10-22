package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeccionRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final SeccionRepositoryMapper mapper = new SeccionRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        SeccionEntity entity = SeccionEntity.builder().id(ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID)
                .nombre("Ventas").posicion((short) 2).creadoEn(AHORA).actualizadoEn(AHORA).build();

        Seccion result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getNombre()).isEqualTo("Ventas");
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        Seccion seccion = Seccion.builder().id(ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).nombre("Ventas")
                .posicion((short) 2).creadoEn(AHORA).actualizadoEn(AHORA).build();

        SeccionEntity result = mapper.toEntity(seccion);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getNombre()).isEqualTo("Ventas");
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }
}
