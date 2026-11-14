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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RespuestaMiniObjetivoTest {

    @Test
    @DisplayName("Arma la respuesta con el mini-objetivo, lo que contó la persona y la semana en que respondió")
    void deShouldCopyEveryField() {
        RespuestaMiniObjetivo result = RespuestaMiniObjetivo.de(
                miniManual(), HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, COMENTARIO, AHORA);

        assertThat(result.id()).isNotNull();
        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.respondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("El comentario se guarda sin espacios en los extremos")
    void deShouldStripTheComment() {
        RespuestaMiniObjetivo result = whenResponder(HizoManual.SI, QuePaso.FUNCIONO, "  Funcionó bien \n");

        assertThat(result.comentario()).isEqualTo("Funcionó bien");
    }

    @Test
    @DisplayName("Un comentario vacío o en blanco queda sin comentario")
    void deShouldDropABlankComment() {
        assertThat(whenResponder(HizoManual.SI, QuePaso.FUNCIONO, "   ").comentario()).isNull();
        assertThat(whenResponder(HizoManual.SI, QuePaso.FUNCIONO, null).comentario()).isNull();
    }

    @Test
    @DisplayName("Un comentario de justo 300 caracteres se acepta")
    void deShouldAcceptACommentOfTheMaximumLength() {
        RespuestaMiniObjetivo result = whenResponder(HizoManual.NO, QuePaso.OTRA, "a".repeat(300));

        assertThat(result.comentario()).hasSize(300);
    }

    @Test
    @DisplayName("Un comentario de más de 300 caracteres se rechaza")
    void deShouldRejectACommentThatIsTooLong() {
        assertThatThrownBy(() -> whenResponder(HizoManual.NO, QuePaso.OTRA, "a".repeat(301)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El comentario no puede tener más de 300 caracteres");
    }

    @Test
    @DisplayName("Decir que lo hizo y que no tuvo tiempo es contradictorio")
    void deShouldRejectDoneWithoutTime() {
        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.NO_TUVE_TIEMPO, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Si lo hiciste, no puede ser que no hayas tenido tiempo");
    }

    @Test
    @DisplayName("Lo hizo en parte o no lo hizo pero no tuvo tiempo es una respuesta válida")
    void deShouldAcceptNoTimeWhenNotFullyDone() {
        assertThat(whenResponder(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, null).quePaso())
                .isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(whenResponder(HizoManual.NO, QuePaso.NO_TUVE_TIEMPO, null).hizo()).isEqualTo(HizoManual.NO);
    }

    // --- act ---
    private RespuestaMiniObjetivo whenResponder(HizoManual hizo, QuePaso quePaso, String comentario) {
        return RespuestaMiniObjetivo.de(miniManual(), hizo, quePaso, comentario, AHORA);
    }
}
