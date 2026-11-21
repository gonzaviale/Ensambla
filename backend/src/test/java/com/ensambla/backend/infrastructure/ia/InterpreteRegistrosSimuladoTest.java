package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpreteRegistrosSimuladoTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId FECHA_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId OTRO_MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final LocalDate HOY = LocalDate.parse("2026-10-27");

    private final InterpreteRegistrosSimulado interprete = new InterpreteRegistrosSimulado();
    private final TablaDisponible gastos = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(FECHA_ID, "Fecha", TipoCampo.FECHA, List.of()),
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()),
            new CampoConocido(OTRO_MONTO_ID, "Saldo", TipoCampo.NUMERO, List.of()))), "Mi panadería");
    private final TablaDisponible ventas = new TablaDisponible(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
            new CampoConocido(CONCEPTO_ID, "Producto", TipoCampo.TEXTO, List.of()))), "Mi panadería");

    private InterpretacionRegistros interpretacion;

    @Test
    @DisplayName("Con una sola tabla la elige, pone el primer número en la primera columna numérica y la fecha de hoy")
    void conUnaSolaTablaLaEligePoneElPrimerNumeroEnLaPrimeraColumnaNumericaYLaFechaDeHoy() {
        cuandoSeInterpretaUnGastoConMontoEnUnaSolaTabla();

        entoncesCompletaMontoFechaYConceptoDeLaUnicaTabla();
    }

    @Test
    @DisplayName("Con varias tablas elige la que el texto nombra")
    void conVariasTablasEligeLaQueElTextoNombra() {
        cuandoSeInterpreta(consulta("anoté las ventas de hoy", null, gastos, ventas));

        entoncesLaTablaEs(VENTAS_ID);
    }

    @Test
    @DisplayName("Con la tabla ya elegida usa esa aunque el texto nombre otra")
    void conLaTablaYaElegidaUsaEsaAunqueElTextoNombreOtra() {
        cuandoSeInterpreta(consulta("gastos de hoy", VENTAS_ID, gastos, ventas));

        entoncesLaTablaEs(VENTAS_ID);
    }

    @Test
    @DisplayName("Con una tabla elegida que ya no existe no se arriesga")
    void conUnaTablaElegidaQueYaNoExisteNoSeArriesga() {
        cuandoSeInterpreta(consulta("hola", BloqueId.nuevo(), gastos, ventas));

        entoncesNoHayTabla();
    }

    @Test
    @DisplayName("Con varias tablas y ninguna nombrada no se arriesga: vuelve vacía")
    void conVariasTablasYNingunaNombradaNoSeArriesgaVuelveVacia() {
        cuandoSeInterpreta(consulta("5000 de harina", null, gastos, ventas));

        entoncesNoHayTabla();
        entoncesNoHayFilas();
    }

    @Test
    @DisplayName("Con columnas de texto largo y numéricas completa la primera de cada clase y salta las demás")
    void conColumnasDeTextoLargoYNumericasCompletaLaPrimeraDeCadaClaseYSaltaLasDemas() {
        cuandoSeInterpretaUnaTablaDeNotasConTextoLargoNumeroYOpcion();

        entoncesSoloSeCompletaLaColumnaDeTextoLargo();
    }

    @Test
    @DisplayName("Sin números ni texto sobrante deja las columnas sin completar")
    void sinNumerosNiTextoSobranteDejaLasColumnasSinCompletar() {
        cuandoSeInterpreta(consulta("   ", null, ventas));

        entoncesLaPrimeraFilaQuedaSinCompletar();
    }

    // --- cuando ---

    private void cuandoSeInterpreta(ConsultaInterpretacion consulta) {
        interpretacion = interprete.interpretar(consulta);
    }

    private void cuandoSeInterpretaUnGastoConMontoEnUnaSolaTabla() {
        cuandoSeInterpreta(consulta("gasté 5000,50 en harina", null, gastos));
    }

    private void cuandoSeInterpretaUnaTablaDeNotasConTextoLargoNumeroYOpcion() {
        TablaDisponible tabla = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Notas", List.of(
                new CampoConocido(CONCEPTO_ID, "Detalle", TipoCampo.TEXTO_LARGO, List.of()),
                new CampoConocido(MONTO_ID, "Cantidad", TipoCampo.NUMERO, List.of()),
                new CampoConocido(OTRO_MONTO_ID, "Estado", TipoCampo.OPCION, List.of("Ok")))), "Mi panadería");
        cuandoSeInterpreta(consulta("tres cosas", null, tabla));
    }

    // --- entonces ---

    private void entoncesCompletaMontoFechaYConceptoDeLaUnicaTabla() {
        assertThat(interpretacion.bloqueId()).isEqualTo(GASTOS_ID);
        assertThat(interpretacion.confianza()).isEqualTo(0.9);
        assertThat(interpretacion.filas()).hasSize(1);
        assertThat(interpretacion.filas().getFirst()).containsEntry(MONTO_ID, "5000.50")
                .containsEntry(FECHA_ID, "2026-10-27").containsEntry(CONCEPTO_ID, "gasté  en harina")
                .doesNotContainKey(OTRO_MONTO_ID);
    }

    private void entoncesLaTablaEs(BloqueId esperada) {
        assertThat(interpretacion.bloqueId()).isEqualTo(esperada);
    }

    private void entoncesNoHayTabla() {
        assertThat(interpretacion.bloqueId()).isNull();
    }

    private void entoncesNoHayFilas() {
        assertThat(interpretacion.filas()).isEmpty();
    }

    private void entoncesSoloSeCompletaLaColumnaDeTextoLargo() {
        assertThat(interpretacion.filas().getFirst()).containsOnlyKeys(CONCEPTO_ID);
    }

    private void entoncesLaPrimeraFilaQuedaSinCompletar() {
        assertThat(interpretacion.filas().getFirst()).isEmpty();
    }

    // --- datos ---

    private static ConsultaInterpretacion consulta(String texto, BloqueId destino, TablaDisponible... tablas) {
        return new ConsultaInterpretacion(texto, List.of(tablas), HOY, destino);
    }
}
