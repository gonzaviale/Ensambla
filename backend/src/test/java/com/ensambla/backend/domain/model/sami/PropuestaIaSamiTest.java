package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    private static final String SIN_PLANTILLA = null;

    private PropuestaIaSami propuesta;
    private Optional<InterpretacionSami> interpretacion;

    @Test
    @DisplayName("Resuelve la plantilla elegida sin importar mayúsculas ni espacios y conserva la confianza")
    void resuelveLaPlantillaSinImportarMayusculasNiEspacios() {
        dadoQueLaIaEligio(" ventas ", 0.8);

        cuandoSeInterpretaEntre(GASTOS, VENTAS);

        entoncesSeInterpretoComoVentasCon(0.8);
    }

    @Test
    @DisplayName("Si la IA inventó una plantilla no hay interpretación")
    void siLaIaInventoUnaPlantillaNoHayInterpretacion() {
        dadoQueLaIaEligio("Peluquería", 0.9);

        cuandoSeInterpretaEntre(VENTAS, GASTOS);

        entoncesNoHayInterpretacion();
    }

    @Test
    @DisplayName("Si la IA no eligió plantilla no hay interpretación")
    void siLaIaNoEligioPlantillaNoHayInterpretacion() {
        dadoQueLaIaEligio(SIN_PLANTILLA, 0.9);

        cuandoSeInterpretaEntre(VENTAS);

        entoncesNoHayInterpretacion();
    }

    @ParameterizedTest
    @CsvSource({"7.0,1.0", "-3.0,0.0"})
    @DisplayName("La confianza queda entre 0 y 1")
    void laConfianzaQuedaEntreCeroYUno(double propuesta, double queda) {
        dadoQueLaIaEligio("Ventas", propuesta);

        cuandoSeInterpretaEntre(VENTAS);

        entoncesLaConfianzaEs(queda);
    }

    @Test
    @DisplayName("Conserva las columnas válidas con el nombre sin espacios sobrantes")
    void conservaLasColumnasValidas() {
        dadoQueLaIaPropusoLasColumnas(
                new ColumnaSugerida("  Saldo pendiente ", TipoCampo.NUMERO, List.of("ignorada")),
                new ColumnaSugerida("Entregado", TipoCampo.SI_NO, null),
                new ColumnaSugerida("Notas", TipoCampo.TEXTO, List.of()),
                new ColumnaSugerida("Vence", TipoCampo.FECHA, List.of()));

        cuandoSeInterpretaEntre(VENTAS);

        entoncesLasColumnasSon("Saldo pendiente", "Entregado", "Notas", "Vence");
        entoncesLaPrimeraColumnaNoTieneOpciones();
    }

    @Test
    @DisplayName("Descarta las columnas sin nombre, con nombre largo o de un tipo que la IA no puede proponer")
    void descartaLasColumnasInvalidas() {
        dadoQueLaIaPropusoLasColumnas(
                new ColumnaSugerida(null, TipoCampo.TEXTO, null),
                new ColumnaSugerida("   ", TipoCampo.TEXTO, null),
                new ColumnaSugerida("x".repeat(PropuestaIaSami.MAX_LARGO_NOMBRE + 1), TipoCampo.TEXTO, null),
                new ColumnaSugerida("Cliente", TipoCampo.RELACION, null),
                new ColumnaSugerida("Total", TipoCampo.FORMULA, null),
                new ColumnaSugerida("Sin tipo", null, null),
                new ColumnaSugerida("Justo", TipoCampo.TEXTO, null));

        cuandoSeInterpretaEntre(VENTAS);

        entoncesLasColumnasSon("Justo");
    }

    @Test
    @DisplayName("Una columna de opciones necesita al menos dos distintas; limpia espacios, vacías y repetidas")
    void unaColumnaDeOpcionesNecesitaDosDistintas() {
        dadoQueLaIaPropusoLasColumnas(
                new ColumnaSugerida("Forma de pago", TipoCampo.OPCION,
                        Arrays.asList(" Efectivo ", "Tarjeta", null, "  ", "Efectivo")),
                new ColumnaSugerida("Una sola", TipoCampo.OPCION, List.of("Sí")),
                new ColumnaSugerida("Sin opciones", TipoCampo.OPCION, null));

        cuandoSeInterpretaEntre(VENTAS);

        entoncesLasColumnasSon("Forma de pago");
        entoncesLaPrimeraColumnaTieneLasOpciones("Efectivo", "Tarjeta");
    }

    @Test
    @DisplayName("Descarta las columnas repetidas y las que la plantilla ya trae")
    void descartaLasRepetidasYLasQueLaPlantillaYaTrae() {
        dadoQueLaIaPropusoLasColumnas(
                new ColumnaSugerida("monto", TipoCampo.NUMERO, null),
                new ColumnaSugerida("Saldo", TipoCampo.NUMERO, null),
                new ColumnaSugerida("SALDO ", TipoCampo.TEXTO, null));

        cuandoSeInterpretaEntre(VENTAS);

        entoncesLasColumnasSon("Saldo");
    }

    @Test
    @DisplayName("Conserva como máximo seis columnas")
    void conservaComoMaximoSeisColumnas() {
        dadoQueLaIaPropusoColumnasDeMas(3);

        cuandoSeInterpretaEntre(VENTAS);

        entoncesHayColumnas(PropuestaIaSami.MAX_COLUMNAS);
    }

    private void dadoQueLaIaEligio(String plantilla, double confianza) {
        propuesta = new PropuestaIaSami(plantilla, List.of(), confianza, CONSUMO);
    }

    private void dadoQueLaIaPropusoLasColumnas(ColumnaSugerida... columnas) {
        propuesta = new PropuestaIaSami("Ventas", List.of(columnas), 0.9, CONSUMO);
    }

    private void dadoQueLaIaPropusoColumnasDeMas(int deMas) {
        dadoQueLaIaPropusoLasColumnas(IntStream.rangeClosed(1, PropuestaIaSami.MAX_COLUMNAS + deMas)
                .mapToObj(i -> new ColumnaSugerida("Columna " + i, TipoCampo.TEXTO, null))
                .toArray(ColumnaSugerida[]::new));
    }

    private void cuandoSeInterpretaEntre(PlantillaProyecto... plantillas) {
        interpretacion = propuesta.aInterpretacion(List.of(plantillas));
    }

    private List<ColumnaSugerida> columnasExtra() {
        return interpretacion.orElseThrow().columnasExtra();
    }

    private void entoncesSeInterpretoComoVentasCon(double confianza) {
        InterpretacionSami resuelta = interpretacion.orElseThrow();
        assertThat(resuelta.plantillaId()).isEqualTo(VENTAS_ID);
        assertThat(resuelta.plantilla()).isEqualTo("Ventas");
        assertThat(resuelta.confianza()).isEqualTo(confianza);
        assertThat(resuelta.puntajes()).isEmpty();
    }

    private void entoncesNoHayInterpretacion() {
        assertThat(interpretacion).isEmpty();
    }

    private void entoncesLaConfianzaEs(double confianza) {
        assertThat(interpretacion.orElseThrow().confianza()).isEqualTo(confianza);
    }

    private void entoncesLasColumnasSon(String... nombres) {
        assertThat(columnasExtra()).extracting(ColumnaSugerida::nombre).containsExactly(nombres);
    }

    private void entoncesHayColumnas(int cantidad) {
        assertThat(columnasExtra()).hasSize(cantidad);
    }

    private void entoncesLaPrimeraColumnaNoTieneOpciones() {
        assertThat(columnasExtra().getFirst().opciones()).isEmpty();
    }

    private void entoncesLaPrimeraColumnaTieneLasOpciones(String... opciones) {
        assertThat(columnasExtra().getFirst().opciones()).containsExactly(opciones);
    }
}