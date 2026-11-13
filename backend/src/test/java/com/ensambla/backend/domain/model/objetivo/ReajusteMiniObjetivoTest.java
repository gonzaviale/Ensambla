package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ReajusteMiniObjetivoTest {

    @Test
    @DisplayName("Guarda el anterior y el nuevo con sus títulos y metas, el tipo y el motivo")
    void deShouldKeepBothMiniObjetivosAndTheReason() {
        MiniObjetivo nuevo = miniObjetivo();
        nuevo.setId(OTRO_MINI_ID);
        nuevo.setTitulo("Vender un 10 % más por semana");
        nuevo.setMeta(new BigDecimal("179667.00"));

        ReajusteMiniObjetivo result = ReajusteMiniObjetivo.de(
                miniObjetivo(), nuevo, TipoReajuste.REEMPLAZAR, "Hace 2 semanas que no avanza", AHORA);

        assertThat(result.id()).isNotNull();
        assertThat(result.miniAnteriorId()).isEqualTo(MINI_ID);
        assertThat(result.miniNuevoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(result.motivo()).isEqualTo("Hace 2 semanas que no avanza");
        assertThat(result.tituloAnterior()).isEqualTo(TITULO);
        assertThat(result.metaAnterior()).isEqualTo(META_MINI);
        assertThat(result.tituloNuevo()).isEqualTo("Vender un 10 % más por semana");
        assertThat(result.metaNueva()).isEqualByComparingTo("179667.00");
        assertThat(result.creadoEn()).isEqualTo(AHORA);
    }
}
