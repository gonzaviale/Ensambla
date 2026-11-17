package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AccionRenovacionTest {

    private static final String TITULO_ANTERIOR = "Un cartel";

    private AccionRenovacion renovacionElegida;
    private AccionManual textoDeReserva;
    private TipoReajuste tipoDeReajuste;
    private String motivo;

    @ParameterizedTest
    @CsvSource({
            "SI, FUNCIONO, SUBE_UN_PASO",
            "SI, OTRA, SUBE_UN_PASO",
            "SI, NO_TUVE_TIEMPO, SUBE_UN_PASO",
            "SI, NO_FUNCIONO, CAMBIA",
            "EN_PARTE, FUNCIONO, SE_DIVIDE",
            "EN_PARTE, NO_TUVE_TIEMPO, SE_DIVIDE",
            "EN_PARTE, NO_FUNCIONO, SE_DIVIDE",
            "NO, NO_TUVE_TIEMPO, SE_SIMPLIFICA",
            "NO, OTRA, SE_SIMPLIFICA",
            "NO, FUNCIONO, SE_SIMPLIFICA",
            "NO, NO_FUNCIONO, CAMBIA"})
    @DisplayName("Según lo que respondió la persona, el manual sube un paso, se divide, se simplifica o cambia")
    void deShouldFollowTheRenewalTable(HizoManual hizo, QuePaso quePaso, AccionRenovacion esperada) {
        whenSeElige(hizo, quePaso);

        thenLaRenovacionEs(esperada);
    }

    @ParameterizedTest
    @CsvSource({
            "SUBE_UN_PASO, SUBIR_META",
            "SE_DIVIDE, REEMPLAZAR",
            "SE_SIMPLIFICA, REEMPLAZAR",
            "CAMBIA, PROBAR_OTRO_CAMINO"})
    @DisplayName("Cada renovación se anota en el resumen con su clase de cambio")
    void tipoReajusteShouldMapEveryRenewal(AccionRenovacion accion, TipoReajuste esperado) {
        whenSeAnotaElReajusteDe(accion);

        thenElReajusteEs(esperado);
    }

    @Test
    @DisplayName("Subir un paso repite lo que salió bien y suma algo más ambicioso")
    void textoDeReservaShouldAskForAStepUpWhenSubeUnPaso() {
        whenSeArmaElTextoDeReserva(AccionRenovacion.SUBE_UN_PASO, TITULO_ANTERIOR);

        thenElTextoDeReservaEs("Un paso más: Un cartel",
                "Te salió bien «Un cartel». Repetilo esta semana y sumale algo más ambicioso.");
    }

    @Test
    @DisplayName("Dividir pide terminar lo que faltó en un paso más chico")
    void textoDeReservaShouldAskForWhatWasMissingWhenSeDivide() {
        whenSeArmaElTextoDeReserva(AccionRenovacion.SE_DIVIDE, TITULO_ANTERIOR);

        thenElTextoDeReservaEs("Lo que faltó: Un cartel",
                "Hiciste una parte de «Un cartel». Esta semana terminá lo que faltó, en un paso más chico.");
    }

    @Test
    @DisplayName("Simplificar propone una versión de diez minutos")
    void textoDeReservaShouldAskForAShortVersionWhenSeSimplifica() {
        whenSeArmaElTextoDeReserva(AccionRenovacion.SE_SIMPLIFICA, TITULO_ANTERIOR);

        thenElTextoDeReservaEs("Versión corta: Un cartel",
                "No llegaste con «Un cartel». Probá una versión de 10 minutos de lo mismo.");
    }

    @Test
    @DisplayName("Cambiar pide elegir otra acción hacia el mismo objetivo")
    void textoDeReservaShouldAskForAnotherActionWhenCambia() {
        whenSeArmaElTextoDeReserva(AccionRenovacion.CAMBIA, TITULO_ANTERIOR);

        thenElTextoDeReservaEs("Probá otra acción para tu objetivo",
                "«Un cartel» no dio resultado. Elegí una acción distinta hacia el mismo objetivo y contale a Sami "
                        + "cuál fue.");
    }

    @ParameterizedTest
    @EnumSource(AccionRenovacion.class)
    @DisplayName("El texto de reserva siempre es una acción válida, incluso con un título de 80 caracteres")
    void textoDeReservaShouldAlwaysBeAValidAction(AccionRenovacion accion) {
        whenSeArmaElTextoDeReserva(accion, tituloDeLargoMaximo());

        thenElTextoDeReservaEsUnaAccionValida();
    }

    @Test
    @DisplayName("Un título que no entra se acorta con puntos suspensivos")
    void textoDeReservaShouldCutALongTitle() {
        whenSeArmaElTextoDeReserva(AccionRenovacion.SE_SIMPLIFICA, "b".repeat(80));

        thenElTituloQuedaAcortadoConPuntosSuspensivos();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "SUBE_UN_PASO|Hiciste «Viejo» y te funcionó: seguimos con un paso más, «Nuevo».",
            "SE_DIVIDE|Hiciste «Viejo» en parte: nos quedamos con lo que faltó, «Nuevo».",
            "SE_SIMPLIFICA|No llegaste con «Viejo»: lo hacemos más simple, «Nuevo».",
            "CAMBIA|«Viejo» no dio resultado: probamos otra acción, «Nuevo»."})
    @DisplayName("El motivo cuenta qué se renovó y por qué")
    void motivoShouldTellWhatWasRenewedAndWhy(AccionRenovacion accion, String esperado) {
        whenSeCuentaElMotivoDe(accion);

        thenElMotivoEs(esperado);
    }

    // --- act ---
    private void whenSeElige(HizoManual hizo, QuePaso quePaso) {
        renovacionElegida = AccionRenovacion.de(hizo, quePaso);
    }

    private void whenSeAnotaElReajusteDe(AccionRenovacion accion) {
        tipoDeReajuste = accion.tipoReajuste();
    }

    private void whenSeArmaElTextoDeReserva(AccionRenovacion accion, String titulo) {
        textoDeReserva = accion.textoDeReserva(titulo);
    }

    private void whenSeCuentaElMotivoDe(AccionRenovacion accion) {
        motivo = accion.motivo("Viejo", "Nuevo");
    }

    // --- assert ---
    private void thenLaRenovacionEs(AccionRenovacion esperada) {
        assertThat(renovacionElegida).isEqualTo(esperada);
    }

    private void thenElReajusteEs(TipoReajuste esperado) {
        assertThat(tipoDeReajuste).isEqualTo(esperado);
    }

    private void thenElTextoDeReservaEs(String titulo, String justificacion) {
        assertThat(textoDeReserva).isEqualTo(new AccionManual(titulo, justificacion));
    }

    private void thenElTextoDeReservaEsUnaAccionValida() {
        assertThat(AccionManual.de(textoDeReserva.titulo(), textoDeReserva.justificacion())).isPresent();
        assertThat(textoDeReserva.titulo()).hasSizeLessThanOrEqualTo(AccionManual.LARGO_MAXIMO_TITULO);
    }

    private void thenElTituloQuedaAcortadoConPuntosSuspensivos() {
        assertThat(textoDeReserva.titulo()).startsWith("Versión corta: bbb").endsWith("…")
                .hasSize(AccionManual.LARGO_MAXIMO_TITULO);
    }

    private void thenElMotivoEs(String esperado) {
        assertThat(motivo).isEqualTo(esperado);
    }

    // --- helpers ---
    private String tituloDeLargoMaximo() {
        return "a".repeat(AccionManual.LARGO_MAXIMO_TITULO);
    }
}
