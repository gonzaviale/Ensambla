package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.IdsDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RegistroRepositoryMapperTest {

    private static final RegistroId ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final FotoId FOTO_ID = FotoId.de("f0000000-0000-4000-8000-000000000001");
    private static final Map<String, Object> VALORES = Map.of("campo", "dato");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final RegistroRepositoryMapper mapper = new RegistroRepositoryMapper();

    @Test
    @DisplayName("Pasa todos los campos de la entidad al modelo")
    void toDomainShouldMapEveryField() {
        RegistroEntity entity = RegistroEntity.builder()
                .id(ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .bloqueId(BLOQUE_ID.valor())
                .valores(VALORES)
                .fotoOrigenId(FOTO_ID.valor())
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        Registro result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getValores()).isEqualTo(VALORES);
        assertThat(result.getFotoOrigenId()).isEqualTo(FOTO_ID);
        assertThat(result.getCreadoEn()).isEqualTo(CREADO);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Pasa todos los campos del modelo a la entidad")
    void toEntityShouldMapEveryField() {
        Registro registro = Registro.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(VALORES)
                .fotoOrigenId(FOTO_ID)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();

        RegistroEntity result = mapper.toEntity(registro);

        assertThat(result).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(RegistroEntity.builder()
                .id(ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .bloqueId(BLOQUE_ID.valor())
                .valores(VALORES)
                .fotoOrigenId(FOTO_ID.valor())
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build());
    }

    @Test
    @DisplayName("Una entidad vacía se mapea a un modelo con valores nulos")
    void toDomainShouldKeepNullsWhenEntityIsEmpty() {
        Registro result = mapper.toDomain(new RegistroEntity());

        assertThat(result.getId()).isNull();
        assertThat(result.getValores()).isNull();
        assertThat(result.getFotoOrigenId()).isNull();
    }
}
