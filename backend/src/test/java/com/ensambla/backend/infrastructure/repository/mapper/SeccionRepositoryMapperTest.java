package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeccionRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");
    private static final short POSICION = 2;

    private final SeccionRepositoryMapper mapper = new SeccionRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void unaEntidadSeLeeComoSeccionConTodosSusDatos() {
        SeccionEntity entidad = dadoUnaEntidadDeLaSeccionVentas();

        Seccion seccion = cuandoSeConvierteEnSeccion(entidad);

        entoncesLaSeccionTieneTodosLosDatosDeLaEntidad(seccion);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void unaSeccionSeGuardaConTodosSusDatos() {
        Seccion seccion = dadoLaSeccionVentas();

        SeccionEntity entidad = cuandoSeConvierteEnEntidad(seccion);

        entoncesLaEntidadTieneTodosLosDatosDeLaSeccion(entidad);
    }

    // --- dado ---
    private SeccionEntity dadoUnaEntidadDeLaSeccionVentas() {
        return SeccionEntity.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PROYECTO_ID.valor())
                .nombre("Ventas")
                .posicion(POSICION)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    private Seccion dadoLaSeccionVentas() {
        return Seccion.builder()
                .id(SeccionId.de(ID))
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .nombre("Ventas")
                .posicion(POSICION)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private Seccion cuandoSeConvierteEnSeccion(SeccionEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private SeccionEntity cuandoSeConvierteEnEntidad(Seccion seccion) {
        return mapper.toEntity(seccion);
    }

    // --- entonces ---
    private void entoncesLaSeccionTieneTodosLosDatosDeLaEntidad(Seccion seccion) {
        assertThat(seccion.getId()).isEqualTo(SeccionId.de(ID));
        assertThat(seccion.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(seccion.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(seccion.getNombre()).isEqualTo("Ventas");
        assertThat(seccion.getPosicion()).isEqualTo(POSICION);
        assertThat(seccion.getCreadoEn()).isEqualTo(AHORA);
        assertThat(seccion.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeLaSeccion(SeccionEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(ID);
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getProyectoId()).isEqualTo(PROYECTO_ID.valor());
        assertThat(entidad.getNombre()).isEqualTo("Ventas");
        assertThat(entidad.getPosicion()).isEqualTo(POSICION);
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }
}
