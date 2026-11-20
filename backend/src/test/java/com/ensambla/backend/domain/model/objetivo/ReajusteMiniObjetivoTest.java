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

    private MiniObjetivo nuevo;
    private ReajusteMiniObjetivo reajuste;

    @Test
    @DisplayName("Guarda el anterior y el nuevo con sus títulos y metas, el tipo y el motivo")
    void guardaElAnteriorYElNuevoConSusTitulosYMetasElTipoYElMotivo() {
        dadoUnMiniObjetivoNuevoQueReemplazaAlAnterior();

        cuandoSeRegistraElReajuste();

        entoncesGuardaAmbosMiniObjetivosYElMotivo();
    }

    // --- dado ---

    private void dadoUnMiniObjetivoNuevoQueReemplazaAlAnterior() {
        nuevo = miniObjetivo();
        nuevo.setId(OTRO_MINI_ID);
        nuevo.setTitulo("Vender un 10 % más por semana");
        nuevo.setMeta(new BigDecimal("179667.00"));
    }

    // --- cuando ---

    private void cuandoSeRegistraElReajuste() {
        reajuste = ReajusteMiniObjetivo.de(
                miniObjetivo(), nuevo, TipoReajuste.REEMPLAZAR, "Hace 2 semanas que no avanza", AHORA);
    }

    // --- entonces ---

    private void entoncesGuardaAmbosMiniObjetivosYElMotivo() {
        assertThat(reajuste.id()).isNotNull();
        assertThat(reajuste.miniAnteriorId()).isEqualTo(MINI_ID);
        assertThat(reajuste.miniNuevoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(reajuste.motivo()).isEqualTo("Hace 2 semanas que no avanza");
        assertThat(reajuste.tituloAnterior()).isEqualTo(TITULO);
        assertThat(reajuste.metaAnterior()).isEqualTo(META_MINI);
        assertThat(reajuste.tituloNuevo()).isEqualTo("Vender un 10 % más por semana");
        assertThat(reajuste.metaNueva()).isEqualByComparingTo("179667.00");
        assertThat(reajuste.creadoEn()).isEqualTo(AHORA);
    }
}
