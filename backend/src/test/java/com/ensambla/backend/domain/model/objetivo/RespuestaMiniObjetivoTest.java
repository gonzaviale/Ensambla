package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.SEMANA_RESPUESTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RespuestaMiniObjetivoTest {

    private static final int LARGO_MAXIMO_DE_COMENTARIO = 300;

    private RespuestaMiniObjetivo respuesta;
    private Throwable error;

    @Test
    @DisplayName("Arma la respuesta con el mini-objetivo, lo que contó la persona y la semana en que respondió")
    void deShouldCopyEveryField() {
        whenSeResponde(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, COMENTARIO);

        thenLaRespuestaCopiaTodosLosDatos();
    }

    @Test
    @DisplayName("El comentario se guarda sin espacios en los extremos")
    void deShouldStripTheComment() {
        whenSeResponde(HizoManual.SI, QuePaso.FUNCIONO, "  Funcionó bien \n");

        thenElComentarioEs("Funcionó bien");
    }

    @Test
    @DisplayName("Un comentario en blanco queda sin comentario")
    void deShouldDropABlankComment() {
        whenSeResponde(HizoManual.SI, QuePaso.FUNCIONO, "   ");

        thenNoHayComentario();
    }

    @Test
    @DisplayName("Un comentario nulo queda sin comentario")
    void deShouldDropANullComment() {
        whenSeResponde(HizoManual.SI, QuePaso.FUNCIONO, null);

        thenNoHayComentario();
    }

    @Test
    @DisplayName("Un comentario de justo 300 caracteres se acepta")
    void deShouldAcceptACommentOfTheMaximumLength() {
        whenSeResponde(HizoManual.NO, QuePaso.OTRA, "a".repeat(LARGO_MAXIMO_DE_COMENTARIO));

        thenElComentarioTieneLargo(LARGO_MAXIMO_DE_COMENTARIO);
    }

    @Test
    @DisplayName("Un comentario de más de 300 caracteres se rechaza")
    void deShouldRejectACommentThatIsTooLong() {
        whenSeRespondeConError(HizoManual.NO, QuePaso.OTRA, "a".repeat(LARGO_MAXIMO_DE_COMENTARIO + 1));

        thenSeRechazaConMensaje("El comentario no puede tener más de 300 caracteres");
    }

    @Test
    @DisplayName("Decir que lo hizo y que no tuvo tiempo es contradictorio")
    void deShouldRejectDoneWithoutTime() {
        whenSeRespondeConError(HizoManual.SI, QuePaso.NO_TUVE_TIEMPO, null);

        thenSeRechazaConMensaje("Si lo hiciste, no puede ser que no hayas tenido tiempo");
    }

    @Test
    @DisplayName("Haberlo hecho en parte pero no tener tiempo es una respuesta válida")
    void deShouldAcceptNoTimeWhenDoneInPart() {
        whenSeResponde(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, null);

        thenQuePasoEs(QuePaso.NO_TUVE_TIEMPO);
    }

    @Test
    @DisplayName("No haberlo hecho y no tener tiempo es una respuesta válida")
    void deShouldAcceptNoTimeWhenNotDone() {
        whenSeResponde(HizoManual.NO, QuePaso.NO_TUVE_TIEMPO, null);

        thenHizoEs(HizoManual.NO);
    }

    // --- act ---
    private void whenSeResponde(HizoManual hizo, QuePaso quePaso, String comentario) {
        respuesta = RespuestaMiniObjetivo.de(miniManual(), hizo, quePaso, comentario, AHORA);
    }

    private void whenSeRespondeConError(HizoManual hizo, QuePaso quePaso, String comentario) {
        error = catchThrowable(() -> whenSeResponde(hizo, quePaso, comentario));
    }

    // --- assert ---
    private void thenLaRespuestaCopiaTodosLosDatos() {
        assertThat(respuesta.id()).isNotNull();
        assertThat(respuesta.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(respuesta.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(respuesta.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(respuesta.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(respuesta.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(respuesta.comentario()).isEqualTo(COMENTARIO);
        assertThat(respuesta.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(respuesta.respondidaEn()).isEqualTo(AHORA);
    }

    private void thenElComentarioEs(String esperado) {
        assertThat(respuesta.comentario()).isEqualTo(esperado);
    }

    private void thenNoHayComentario() {
        assertThat(respuesta.comentario()).isNull();
    }

    private void thenElComentarioTieneLargo(int largo) {
        assertThat(respuesta.comentario()).hasSize(largo);
    }

    private void thenQuePasoEs(QuePaso esperado) {
        assertThat(respuesta.quePaso()).isEqualTo(esperado);
    }

    private void thenHizoEs(HizoManual esperado) {
        assertThat(respuesta.hizo()).isEqualTo(esperado);
    }

    private void thenSeRechazaConMensaje(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }
}
