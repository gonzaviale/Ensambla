package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PropuestaIaSamiTest {

    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 100, 20);
    private static final PlantillaId VENTAS_ID = PlantillaId.nuevo();
    private static final PlantillaProyecto VENTAS = new PlantillaProyecto(VENTAS_ID, "Ventas", "",
            new EstructuraPlantilla(List.of(new BloquePlantilla("Ventas", List.of(
                    new CampoPlantilla("Fecha", TipoCampo.FECHA, false, null, null),
                    new CampoPlantilla("Monto", TipoCampo.NUMERO, false, null, null))))));
    private static final PlantillaProyecto GASTOS = new PlantillaProyecto(PlantillaId.nuevo(), "Gastos", "",
            new EstructuraPlantilla(List.of()));

    @Test
    @DisplayName("Resuelve la plantilla elegida sin importar mayúsculas ni espacios y conserva la confianza")
    void aInterpretacionShouldMatchTemplateIgnoringCaseAndSpaces() {
        PropuestaIaSami propuesta = propuesta(" ventas ", 0.8);

        Optional<InterpretacionSami> result = propuesta.aInterpretacion(List.of(GASTOS, VENTAS));

        assertThat(result).isPresent();
        assertThat(result.get().plantillaId()).isEqualTo(VENTAS_ID);
        assertThat(result.get().plantilla()).isEqualTo("Ventas");
        assertThat(result.get().confianza()).isEqualTo(0.8);
        assertThat(result.get().puntajes()).isEmpty();
    }

    @Test
    @DisplayName("Si la IA inventó una plantilla no hay interpretación")
    void aInterpretacionShouldBeEmptyForUnknownTemplate() {
        assertThat(propuesta("Peluquería", 0.9).aInterpretacion(List.of(VENTAS, GASTOS))).isEmpty();
    }

    @Test
    @DisplayName("Si la IA no eligió plantilla no hay interpretación")
    void aInterpretacionShouldBeEmptyWithoutTemplate() {
        assertThat(propuesta(null, 0.9).aInterpretacion(List.of(VENTAS))).isEmpty();
    }

    @Test
    @DisplayName("La confianza queda entre 0 y 1")
    void aInterpretacionShouldClampConfidence() {
        assertThat(propuesta("Ventas", 7.0).aInterpretacion(List.of(VENTAS)).get().confianza()).isEqualTo(1.0);
        assertThat(propuesta("Ventas", -3.0).aInterpretacion(List.of(VENTAS)).get().confianza()).isZero();
    }

    @Test
    @DisplayName("Conserva las columnas válidas con el nombre sin espacios sobrantes")
    void aInterpretacionShouldKeepValidColumns() {
        PropuestaIaSami propuesta = new PropuestaIaSami("Ventas", List.of(
                new ColumnaSugerida("  Saldo pendiente ", TipoCampo.NUMERO, List.of("ignorada")),
                new ColumnaSugerida("Entregado", TipoCampo.SI_NO, null),
                new ColumnaSugerida("Notas", TipoCampo.TEXTO, List.of()),
                new ColumnaSugerida("Vence", TipoCampo.FECHA, List.of())), 0.9, CONSUMO);

        List<ColumnaSugerida> columnas = propuesta.aInterpretacion(List.of(VENTAS)).get().columnasExtra();

        assertThat(columnas).extracting(ColumnaSugerida::nombre)
                .containsExactly("Saldo pendiente", "Entregado", "Notas", "Vence");
        assertThat(columnas.getFirst().opciones()).isEmpty();
    }

    @Test
    @DisplayName("Descarta las columnas sin nombre, con nombre largo o de un tipo que la IA no puede proponer")
    void aInterpretacionShouldDropInvalidColumns() {
        PropuestaIaSami propuesta = new PropuestaIaSami("Ventas", List.of(
                new ColumnaSugerida(null, TipoCampo.TEXTO, null),
                new ColumnaSugerida("   ", TipoCampo.TEXTO, null),
                new ColumnaSugerida("x".repeat(PropuestaIaSami.MAX_LARGO_NOMBRE + 1), TipoCampo.TEXTO, null),
                new ColumnaSugerida("Cliente", TipoCampo.RELACION, null),
                new ColumnaSugerida("Total", TipoCampo.FORMULA, null),
                new ColumnaSugerida("Sin tipo", null, null),
                new ColumnaSugerida("Justo", TipoCampo.TEXTO, null)), 0.9, CONSUMO);

        List<ColumnaSugerida> columnas = propuesta.aInterpretacion(List.of(VENTAS)).get().columnasExtra();

        assertThat(columnas).extracting(ColumnaSugerida::nombre).containsExactly("Justo");
    }

    @Test
    @DisplayName("Una columna de opciones necesita al menos dos distintas; limpia espacios, vacías y repetidas")
    void aInterpretacionShouldCleanOptionColumns() {
        List<String> sucias = new ArrayList<>(Arrays.asList(" Efectivo ", "Tarjeta", null, "  ", "Efectivo"));
        PropuestaIaSami propuesta = new PropuestaIaSami("Ventas", List.of(
                new ColumnaSugerida("Forma de pago", TipoCampo.OPCION, sucias),
                new ColumnaSugerida("Una sola", TipoCampo.OPCION, List.of("Sí")),
                new ColumnaSugerida("Sin opciones", TipoCampo.OPCION, null)), 0.9, CONSUMO);

        List<ColumnaSugerida> columnas = propuesta.aInterpretacion(List.of(VENTAS)).get().columnasExtra();

        assertThat(columnas).hasSize(1);
        assertThat(columnas.getFirst().nombre()).isEqualTo("Forma de pago");
        assertThat(columnas.getFirst().opciones()).containsExactly("Efectivo", "Tarjeta");
    }

    @Test
    @DisplayName("Descarta las columnas repetidas y las que la plantilla ya trae")
    void aInterpretacionShouldDropDuplicatesAndExistingColumns() {
        PropuestaIaSami propuesta = new PropuestaIaSami("Ventas", List.of(
                new ColumnaSugerida("monto", TipoCampo.NUMERO, null),
                new ColumnaSugerida("Saldo", TipoCampo.NUMERO, null),
                new ColumnaSugerida("SALDO ", TipoCampo.TEXTO, null)), 0.9, CONSUMO);

        List<ColumnaSugerida> columnas = propuesta.aInterpretacion(List.of(VENTAS)).get().columnasExtra();

        assertThat(columnas).extracting(ColumnaSugerida::nombre).containsExactly("Saldo");
    }

    @Test
    @DisplayName("Conserva como máximo seis columnas")
    void aInterpretacionShouldKeepAtMostSixColumns() {
        List<ColumnaSugerida> muchas = new ArrayList<>();
        for (int i = 1; i <= PropuestaIaSami.MAX_COLUMNAS + 3; i++) {
            muchas.add(new ColumnaSugerida("Columna " + i, TipoCampo.TEXTO, null));
        }

        List<ColumnaSugerida> columnas = new PropuestaIaSami("Ventas", muchas, 0.9, CONSUMO)
                .aInterpretacion(List.of(VENTAS)).get().columnasExtra();

        assertThat(columnas).hasSize(PropuestaIaSami.MAX_COLUMNAS);
    }

    private PropuestaIaSami propuesta(String plantilla, double confianza) {
        return new PropuestaIaSami(plantilla, List.of(), confianza, CONSUMO);
    }
}
