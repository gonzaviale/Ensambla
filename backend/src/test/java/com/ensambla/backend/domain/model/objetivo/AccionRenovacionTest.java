package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AccionRenovacionTest {

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
        assertThat(AccionRenovacion.de(hizo, quePaso)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("Cada renovación se anota en el resumen con su clase de cambio")
    void tipoReajusteShouldMapEveryRenewal() {
        assertThat(AccionRenovacion.SUBE_UN_PASO.tipoReajuste()).isEqualTo(TipoReajuste.SUBIR_META);
        assertThat(AccionRenovacion.SE_DIVIDE.tipoReajuste()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(AccionRenovacion.SE_SIMPLIFICA.tipoReajuste()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(AccionRenovacion.CAMBIA.tipoReajuste()).isEqualTo(TipoReajuste.PROBAR_OTRO_CAMINO);
    }

    @Test
    @DisplayName("El texto de reserva de cada renovación sale del título anterior")
    void textoDeReservaShouldBuildFromThePreviousTitle() {
        assertThat(AccionRenovacion.SUBE_UN_PASO.textoDeReserva("Un cartel")).isEqualTo(new AccionManual(
                "Un paso más: Un cartel",
                "Te salió bien «Un cartel». Repetilo esta semana y sumale algo más ambicioso."));
        assertThat(AccionRenovacion.SE_DIVIDE.textoDeReserva("Un cartel")).isEqualTo(new AccionManual(
                "Lo que faltó: Un cartel",
                "Hiciste una parte de «Un cartel». Esta semana terminá lo que faltó, en un paso más chico."));
        assertThat(AccionRenovacion.SE_SIMPLIFICA.textoDeReserva("Un cartel")).isEqualTo(new AccionManual(
                "Versión corta: Un cartel",
                "No llegaste con «Un cartel». Probá una versión de 10 minutos de lo mismo."));
        assertThat(AccionRenovacion.CAMBIA.textoDeReserva("Un cartel")).isEqualTo(new AccionManual(
                "Probá otra acción para tu objetivo",
                "«Un cartel» no dio resultado. Elegí una acción distinta hacia el mismo objetivo y contale a Sami "
                        + "cuál fue."));
    }

    @ParameterizedTest
    @EnumSource(AccionRenovacion.class)
    @DisplayName("El texto de reserva siempre es una acción válida, incluso con un título de 80 caracteres")
    void textoDeReservaShouldAlwaysBeAValidAction(AccionRenovacion accion) {
        String titulo = "a".repeat(AccionManual.LARGO_MAXIMO_TITULO);

        AccionManual result = accion.textoDeReserva(titulo);

        assertThat(AccionManual.de(result.titulo(), result.justificacion())).isPresent();
        assertThat(result.titulo()).hasSizeLessThanOrEqualTo(AccionManual.LARGO_MAXIMO_TITULO);
    }

    @Test
    @DisplayName("Un título que no entra se acorta con puntos suspensivos")
    void textoDeReservaShouldCutALongTitle() {
        AccionManual result = AccionRenovacion.SE_SIMPLIFICA.textoDeReserva("b".repeat(80));

        assertThat(result.titulo()).startsWith("Versión corta: bbb").endsWith("…").hasSize(AccionManual.LARGO_MAXIMO_TITULO);
    }

    @Test
    @DisplayName("El motivo cuenta qué se renovó y por qué")
    void motivoShouldTellWhatWasRenewedAndWhy() {
        assertThat(AccionRenovacion.SUBE_UN_PASO.motivo("Viejo", "Nuevo"))
                .isEqualTo("Hiciste «Viejo» y te funcionó: seguimos con un paso más, «Nuevo».");
        assertThat(AccionRenovacion.SE_DIVIDE.motivo("Viejo", "Nuevo"))
                .isEqualTo("Hiciste «Viejo» en parte: nos quedamos con lo que faltó, «Nuevo».");
        assertThat(AccionRenovacion.SE_SIMPLIFICA.motivo("Viejo", "Nuevo"))
                .isEqualTo("No llegaste con «Viejo»: lo hacemos más simple, «Nuevo».");
        assertThat(AccionRenovacion.CAMBIA.motivo("Viejo", "Nuevo"))
                .isEqualTo("«Viejo» no dio resultado: probamos otra acción, «Nuevo».");
    }
}
