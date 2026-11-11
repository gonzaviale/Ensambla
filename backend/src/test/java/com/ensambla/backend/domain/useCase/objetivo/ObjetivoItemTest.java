package com.ensambla.backend.domain.useCase.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoItemTest {

    @Test
    @DisplayName("Copia todos los datos del objetivo")
    void deShouldCopyEveryField() {
        ObjetivoItem result = ObjetivoItem.de(objetivo());

        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.texto()).isEqualTo(TEXTO);
        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.creadoEn()).isEqualTo(AHORA);
    }
}
