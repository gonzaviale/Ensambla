package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaStockBajoTest {

    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final ReglaStockBajo regla = new ReglaStockBajo();

    @Test
    @DisplayName("Es la regla de los avisos de stock")
    void tipoShouldBeStock() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.STOCK);
    }

    @Test
    @DisplayName("Un producto agotado genera un aviso urgente")
    void evaluarShouldReturnUrgentAvisoWhenProductIsSoldOut() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 0, 5));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.prioridad()).isEqualTo((short) 3);
            assertThat(resultado.titulo()).isEqualTo("Te quedaste sin Yerba");
            assertThat(resultado.detalle())
                    .isEqualTo("No te quedan unidades y tu mínimo es 5. Para no quedarte corto, pedí 10.");
            assertThat(resultado.entidad()).isEqualTo("registro:" + REGISTRO_ID);
        });
    }

    @Test
    @DisplayName("Un producto por debajo del mínimo genera un aviso para hoy")
    void evaluarShouldReturnAvisoForTodayWhenProductIsBelowMinimum() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 3, 5));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.titulo()).isEqualTo("Yerba está por debajo del mínimo");
            assertThat(resultado.detalle())
                    .isEqualTo("Te quedan 3 y tu mínimo es 5. Para no quedarte corto, pedí 7.");
            assertThat(resultado.motivos()).containsExactly(
                    "Definiste un mínimo de 5 y quedan 3.", "Para no quedarte corto, pedí 7.");
            assertThat(resultado.datos()).containsEntry("cantidad", new BigDecimal("3"))
                    .containsEntry("minimo", new BigDecimal("5"));
        });
    }

    @Test
    @DisplayName("Un producto agotado sin mínimo avisa que se agotó y sugiere pedir diez")
    void evaluarShouldSuggestTenWhenProductHasNoMinimum() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Azúcar", 0, null));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).isEqualTo("No te quedan unidades. Para no quedarte corto, pedí 10.");
            assertThat(resultado.motivos().get(0))
                    .isEqualTo("No tiene un mínimo definido: te avisamos porque se agotó.");
            assertThat(resultado.datos()).containsEntry("minimo", BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("Pide al menos una unidad y escribe los decimales con coma")
    void evaluarShouldAskForAtLeastOneUnitAndUseCommaDecimals() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Harina", 1.5, 1.5));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.detalle())
                        .isEqualTo("Te quedan 1,5 y tu mínimo es 1,5. Para no quedarte corto, pedí 1,5."));
    }

    @Test
    @DisplayName("Un producto con stock por encima del mínimo no genera aviso")
    void evaluarShouldReturnNothingWhenStockIsAboveMinimum() {
        assertThat(whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 6, 5))).isEmpty();
    }

    @Test
    @DisplayName("Un producto con stock y sin mínimo no genera aviso")
    void evaluarShouldReturnNothingWhenThereIsStockAndNoMinimum() {
        assertThat(whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 1, null))).isEmpty();
    }

    @Test
    @DisplayName("Un mínimo en cero es lo mismo que no tener mínimo")
    void evaluarShouldTreatZeroMinimumAsNoMinimum() {
        assertThat(whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 3, 0))).isEmpty();
    }

    @Test
    @DisplayName("Un producto que no cargó la cantidad no genera aviso")
    void evaluarShouldReturnNothingWhenQuantityIsMissing() {
        assertThat(whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", null, 5))).isEmpty();
    }

    @Test
    @DisplayName("Un producto sin nombre se nombra como un producto sin nombre")
    void evaluarShouldUseGenericNameWhenProductHasNoName() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "  ", 0, null));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.titulo()).isEqualTo("Te quedaste sin Un producto sin nombre"));
    }

    @Test
    @DisplayName("Un producto que no cargó el nombre se nombra como un producto sin nombre")
    void evaluarShouldUseGenericNameWhenNameIsMissing() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, null, 0, null));

        assertThat(resultados.get(0).titulo()).isEqualTo("Te quedaste sin Un producto sin nombre");
    }

    @Test
    @DisplayName("Con tabla de entradas la acción es registrar una entrada")
    void evaluarShouldSuggestRegisteringEntryWhenProjectHasEntradas() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, List.of(AvisoTestData.inventario(
                AvisoTestData.ENTRADAS_ID, AvisoTestData.registro(REGISTRO_ID, "Yerba", 0, null))));

        List<ResultadoRegla> resultados = regla.evaluar(contexto);

        assertThat(resultados.get(0).acciones()).containsExactly(Map.of(
                "tipo", "REGISTRAR", "texto", "Registrar entrada",
                "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                "bloqueId", AvisoTestData.ENTRADAS_ID.toString()));
    }

    @Test
    @DisplayName("Sin tabla de entradas la acción es abrir el producto")
    void evaluarShouldSuggestOpeningProductWhenProjectHasNoEntradas() {
        List<ResultadoRegla> resultados = whenEvaluar(AvisoTestData.registro(REGISTRO_ID, "Yerba", 0, null));

        assertThat(resultados.get(0).acciones()).containsExactly(Map.of(
                "tipo", "ABRIR", "texto", "Ver producto",
                "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                "registroId", REGISTRO_ID.toString()));
    }

    @Test
    @DisplayName("Sin tablas de inventario no hay avisos")
    void evaluarShouldReturnNothingWhenThereAreNoInventarios() {
        assertThat(regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, List.of()))).isEmpty();
    }

    private List<ResultadoRegla> whenEvaluar(com.ensambla.backend.domain.model.registro.Registro registro) {
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventario(null, registro))));
    }
}
