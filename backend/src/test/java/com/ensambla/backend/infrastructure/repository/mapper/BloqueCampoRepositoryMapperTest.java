package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueCampoRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PADRE_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Pasa todos los campos del bloque al modelo")
    void bloqueToDomainShouldMapEveryField() {
        BloqueEntity entity = BloqueEntity.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PADRE_ID)
                .nombre("Productos")
                .posicion((short) 2)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        Bloque result = new BloqueRepositoryMapper().toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PADRE_ID);
        assertThat(result.getNombre()).isEqualTo("Productos");
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Pasa todos los campos del modelo del bloque a la entidad")
    void bloqueToEntityShouldMapEveryField() {
        Bloque bloque = Bloque.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PADRE_ID)
                .nombre("Clientes")
                .posicion((short) 3)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        BloqueEntity result = new BloqueRepositoryMapper().toEntity(bloque);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PADRE_ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getPosicion()).isEqualTo((short) 3);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Pasa todos los campos del campo al modelo")
    void campoToDomainShouldMapEveryField() {
        CampoEntity entity = CampoEntity.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(PADRE_ID)
                .nombre("Rubro")
                .tipo(TipoCampo.OPCION)
                .obligatorio(true)
                .valorInicial("Velas")
                .opciones(List.of("Velas", "Moldes"))
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        Campo result = new CampoRepositoryMapper().toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueId()).isEqualTo(PADRE_ID);
        assertThat(result.getNombre()).isEqualTo("Rubro");
        assertThat(result.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(result.getObligatorio()).isTrue();
        assertThat(result.getValorInicial()).isEqualTo("Velas");
        assertThat(result.getOpciones()).containsExactly("Velas", "Moldes");
        assertThat(result.getPosicion()).isEqualTo((short) 1);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Pasa todos los datos del modelo del campo a la entidad")
    void campoToEntityShouldMapEveryField() {
        Campo campo = Campo.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(PADRE_ID)
                .nombre("Pagado")
                .tipo(TipoCampo.SI_NO)
                .obligatorio(false)
                .valorInicial("true")
                .opciones(List.of())
                .posicion((short) 4)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        CampoEntity result = new CampoRepositoryMapper().toEntity(campo);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueId()).isEqualTo(PADRE_ID);
        assertThat(result.getNombre()).isEqualTo("Pagado");
        assertThat(result.getTipo()).isEqualTo(TipoCampo.SI_NO);
        assertThat(result.getObligatorio()).isFalse();
        assertThat(result.getValorInicial()).isEqualTo("true");
        assertThat(result.getOpciones()).isEmpty();
        assertThat(result.getPosicion()).isEqualTo((short) 4);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Un campo nuevo sin id pasa a una entidad sin id para que la base lo genere")
    void campoToEntityShouldKeepNullId() {
        CampoEntity result = new CampoRepositoryMapper().toEntity(Campo.builder().nombre("Nuevo").build());

        assertThat(result.getId()).isNull();
        assertThat(result.getNombre()).isEqualTo("Nuevo");
    }
}
