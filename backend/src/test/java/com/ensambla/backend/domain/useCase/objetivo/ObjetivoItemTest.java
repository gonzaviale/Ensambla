package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoItemTest {

    private ObjetivoItem item;

    @Test
    @DisplayName("Copia todos los datos del objetivo")
    void deShouldCopyEveryField() {
        whenSeArmaElItemDe(objetivo());

        thenSeCopianTodosLosDatos();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami no tiene meta y lo dice")
    void deShouldKeepTheMissingGoalOfAnObjetivoBuiltWithSami() {
        whenSeArmaElItemDe(objetivoSinMeta());

        thenNoTieneMetaYLoArmoSami();
    }

    // --- act ---
    private void whenSeArmaElItemDe(Objetivo objetivo) {
        item = ObjetivoItem.de(objetivo);
    }

    // --- assert ---
    private void thenSeCopianTodosLosDatos() {
        assertThat(item.id()).isEqualTo(OBJETIVO_ID);
        assertThat(item.texto()).isEqualTo(TEXTO);
        assertThat(item.tipo()).isEqualTo(TipoObjetivo.MONETARIO);
        assertThat(item.meta()).isEqualTo(META);
        assertThat(item.armadoConSami()).isFalse();
        assertThat(item.creadoEn()).isEqualTo(AHORA);
    }

    private void thenNoTieneMetaYLoArmoSami() {
        assertThat(item.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(item.meta()).isNull();
        assertThat(item.armadoConSami()).isTrue();
    }
}
