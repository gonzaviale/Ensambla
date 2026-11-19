package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LecturaCuadernoValidacionTest {

    private LecturaCuaderno lectura;
    private Throwable rechazo;

    @ParameterizedTest
    @CsvSource({"NUMERO,10.25", "FECHA,2026-10-10", "SI_NO,true", "SI_NO,false", "OPCION,A", "TEXTO,algo"})
    @DisplayName("Acepta valores con el formato del tipo declarado")
    void aceptaValoresConElFormatoDelTipoDeclarado(TipoCampo tipo, String valor) {
        dadoUnaLecturaConUnValorDeTipo(tipo, valor, List.of("A"));

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @ParameterizedTest
    @CsvSource({"NUMERO,no", "FECHA,2026-02-30", "SI_NO,si", "OPCION,B"})
    @DisplayName("Rechaza valores incompatibles con el tipo")
    void rechazaValoresIncompatiblesConElTipo(TipoCampo tipo, String valor) {
        dadoUnaLecturaConUnValorDeTipo(tipo, valor, List.of("A"));

        cuandoSeIntentaValidar();

        entoncesSeRechazaComoLecturaConMensajeQueContiene("no coincide");
    }

    @Test
    @DisplayName("Acepta el texto leído en un campo de conexión: después se resuelve a una fila")
    void aceptaElTextoLeidoEnUnCampoDeConexionDespuesSeResuelveAUnaFila() {
        dadoUnaLecturaConUnValorDeTipo(TipoCampo.RELACION, "Juan Pérez", List.of());

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Conserva un valor ilegible nulo")
    void conservaUnValorIlegibleNulo() {
        dadoUnaLecturaConUnValorDeTipo(TipoCampo.NUMERO, null, List.of());

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Rechaza dos filas con la misma página y renglón")
    void rechazaDosFilasConLaMismaPaginaYRenglon() {
        dadoUnaLecturaConLaMismaFilaDosVeces();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("repetido");
    }

    @Test
    @DisplayName("Rechaza una confianza no finita")
    void rechazaUnaConfianzaNoFinita() {
        dadoUnaLecturaConConfianzaNoFinita();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("finito");
    }

    @Test
    @DisplayName("Rechaza nombres mayores al límite del esquema")
    void rechazaNombresMayoresAlLimiteDelEsquema() {
        dadoUnaLecturaConUnBloqueDeNombreLargo();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("120");
    }

    @Test
    @DisplayName("Rechaza claves inválidas")
    void rechazaClavesInvalidas() {
        dadoUnaLecturaConUnaClaveInvalida();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("clave");
    }

    @Test
    @DisplayName("Rechaza opciones vacías")
    void rechazaOpcionesVacias() {
        dadoUnaLecturaConUnValorDeTipo(TipoCampo.OPCION, "A", List.of("A", " "));

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("opciones");
    }

    @Test
    @DisplayName("Rechaza opciones duplicadas")
    void rechazaOpcionesDuplicadas() {
        dadoUnaLecturaConUnValorDeTipo(TipoCampo.OPCION, "A", List.of("A", "A"));

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorMensajeQueContiene("opciones");
    }

    @Test
    @DisplayName("Rechaza un valor en un campo fórmula: la lectura de la IA no propone fórmulas")
    void rechazaUnValorEnUnCampoFormulaLaLecturaDeLaIaNoProponeFormulas() {
        dadoUnaLecturaConUnValorDeTipo(TipoCampo.FORMULA, "10", List.of());

        cuandoSeIntentaValidar();

        entoncesSeRechazaComoLecturaConMensajeQueContiene("no coincide");
    }

    // --- dado ---
    private void dadoUnaLecturaConUnValorDeTipo(TipoCampo tipo, String valor, List<String> opciones) {
        lectura = ProcesamientoTestData.lectura(tipo, valor, opciones);
    }

    private void dadoUnaLecturaConLaMismaFilaDosVeces() {
        LecturaCuaderno base = ProcesamientoTestData.lectura();
        lectura = unaLectura(base.bloque(), List.of(base.filas().getFirst(), base.filas().getFirst()));
    }

    private void dadoUnaLecturaConConfianzaNoFinita() {
        lectura = unaLectura(ProcesamientoTestData.lectura().bloque(),
                List.of(new FilaLeida(1, 1, Map.of("monto", new DatoLeido("1", Double.NaN)))));
    }

    private void dadoUnaLecturaConUnBloqueDeNombreLargo() {
        lectura = unaLectura(new BloquePropuesto(
                "a".repeat(121), null, ProcesamientoTestData.lectura().bloque().campos()), List.of());
    }

    private void dadoUnaLecturaConUnaClaveInvalida() {
        lectura = unaLectura(new BloquePropuesto("Ventas", null,
                List.of(new CampoPropuesto("No-valida", "Monto", TipoCampo.NUMERO, List.of()))), List.of());
    }

    private LecturaCuaderno unaLectura(BloquePropuesto bloque, List<FilaLeida> filas) {
        LecturaCuaderno base = ProcesamientoTestData.lectura();
        return new LecturaCuaderno(base.tipoRegistro(), bloque, filas, List.of(), base.consumo());
    }

    // --- cuando ---
    private void cuandoSeIntentaValidar() {
        rechazo = catchThrowable(lectura::validar);
    }

    // --- entonces ---
    private void entoncesSeAcepta() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorMensajeQueContiene(String texto) {
        assertThat(rechazo).hasMessageContaining(texto);
    }

    private void entoncesSeRechazaComoLecturaConMensajeQueContiene(String texto) {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessageContaining(texto);
    }
}
