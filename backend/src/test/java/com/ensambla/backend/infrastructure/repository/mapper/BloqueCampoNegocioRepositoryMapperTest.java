package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueCampoNegocioRepositoryMapperTest {

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
    @DisplayName("Pasa todos los campos del negocio al modelo")
    void negocioToDomainShouldMapEveryField() {
        NegocioEntity entity = NegocioEntity.builder()
                .id(ID)
                .nombre("Velas del Sur")
                .duenioId(PADRE_ID)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        Negocio result = new NegocioRepositoryMapper().toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNombre()).isEqualTo("Velas del Sur");
        assertThat(result.getDuenioId()).isEqualTo(PADRE_ID);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }
}
