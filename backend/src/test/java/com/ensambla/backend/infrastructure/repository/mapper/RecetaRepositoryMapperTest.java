package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import com.ensambla.backend.support.IdsDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RecetaRepositoryMapperTest {

    private static final RecetaId ID = RecetaId.de("f0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId AZUCAR_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    private final RecetaRepositoryMapper mapper = new RecetaRepositoryMapper();

    @Test
    @DisplayName("Pasa del modelo a la entidad y de vuelta, con las cantidades como texto y sin perder decimales")
    void shouldRoundTripEveryFieldKeepingDecimals() {
        Receta receta = Receta.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(PRECIO_ID)
                .lineas(List.of(
                        new LineaReceta(HARINA_ID, new BigDecimal("0.125"), "kg"),
                        new LineaReceta(AZUCAR_ID, new BigDecimal("2"), null)))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();

        RecetaEntity entity = mapper.toEntity(receta);
        Receta result = mapper.toDomain(entity);

        assertThat(entity.getLineas().get(0)).containsEntry("cantidad", "0.125").containsEntry("unidad", "kg");
        assertThat(entity.getLineas().get(1)).containsEntry("unidad", null);
        assertThat(result).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(receta);
    }
}
