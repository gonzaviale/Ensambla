package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.TITULO_AVISO;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class DecisionObjetivoTest {

    @Test
    @DisplayName("Una decisión sobre un aviso copia el aviso, el producto y el mini-objetivo vinculado")
    void deAvisoShouldCopyTheAvisoAndLinkTheMiniObjetivo() {
        DecisionObjetivo result = DecisionObjetivo.deAviso(
                objetivo(), aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO,
                miniObjetivo(), AHORA);

        assertThat(result.id()).isNotNull();
        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(result.avisoId()).isEqualTo(AVISO_ID);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(result.tomadaEn()).isEqualTo(AHORA);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Sin mini-objetivo del producto la decisión queda sin vincular")
    void deAvisoShouldLeaveTheMiniObjetivoEmptyWhenThereIsNone() {
        DecisionObjetivo result = DecisionObjetivo.deAviso(
                objetivo(), aviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO), TipoDecision.AVISO_DESCARTADO, null,
                AHORA);

        assertThat(result.miniObjetivoId()).isNull();
        assertThat(result.tipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
    }

    @Test
    @DisplayName("Un aviso que no es de un producto no trae producto")
    void deAvisoShouldHaveNoProductWhenTheAvisoIsNotAboutOne() {
        Aviso deLaZona = aviso(TipoAviso.CONSUMO_ZONA, EstadoAviso.APLICADO);
        deLaZona.setEntidad("zona");

        DecisionObjetivo result = DecisionObjetivo.deAviso(
                objetivo(), deLaZona, TipoDecision.AVISO_APLICADO, null, AHORA);

        assertThat(result.registroId()).isNull();
    }

    @Test
    @DisplayName("La semana es la del lunes en hora argentina, aunque en UTC ya sea otro día")
    void deAvisoShouldUseTheArgentineWeek() {
        OffsetDateTime domingoALaNoche = OffsetDateTime.parse("2026-11-09T01:30:00Z");

        DecisionObjetivo result = DecisionObjetivo.deAviso(
                objetivo(), aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO, null,
                domingoALaNoche);

        assertThat(result.semanaInicio()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    @DisplayName("Una decisión sobre un paso copia el mini-objetivo, su producto y su título")
    void dePasoShouldCopyTheMiniObjetivo() {
        DecisionObjetivo result = DecisionObjetivo.dePaso(miniObjetivo(), DecisionPaso.ACEPTADO, AHORA);

        assertThat(result.id()).isNotNull();
        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(result.avisoId()).isNull();
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.descripcion()).isEqualTo(TITULO);
        assertThat(result.tomadaEn()).isEqualTo(AHORA);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Ignorar el paso es otro tipo de decisión")
    void dePasoShouldBeIgnoredWhenThePersonSaysNotNow() {
        DecisionObjetivo result = DecisionObjetivo.dePaso(miniObjetivo(), DecisionPaso.IGNORADO, AHORA);

        assertThat(result.tipo()).isEqualTo(TipoDecision.PASO_IGNORADO);
    }
}
