package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoItemTest {

    private static final BigDecimal VALOR_ACTUAL = new BigDecimal("13391.50");

    private MiniObjetivo mini;
    private MiniObjetivoItem item;

    @Test
    @DisplayName("Copia todos los datos del mini-objetivo y le suma su sentido y su avance")
    void copiaTodosLosDatosDelMiniObjetivoYLeSumaSuSentidoYSuAvance() {
        dadoUnMedidoConIa();

        cuandoSeArmaElItem();

        entoncesSeCopianTodosLosDatosConSentidoYAvance();
    }

    @Test
    @DisplayName("Un mini-objetivo manual copia su texto y deja sin números todo lo medido")
    void unMiniObjetivoManualCopiaSuTextoYDejaSinNumerosTodoLoMedido() {
        dadoUnManual();

        cuandoSeArmaElItem();

        entoncesSeCopiaElTextoYNoHayNumeros();
    }

    // --- arrange ---
    private void dadoUnMedidoConIa() {
        mini = miniObjetivo();
        mini.setValorActual(VALOR_ACTUAL);
        mini.setTextoConIa(true);
    }

    private void dadoUnManual() {
        mini = miniManual();
    }

    // --- act ---
    private void cuandoSeArmaElItem() {
        item = MiniObjetivoItem.de(mini);
    }

    // --- assert ---
    private void entoncesSeCopianTodosLosDatosConSentidoYAvance() {
        assertThat(item.id()).isEqualTo(MINI_ID);
        assertThat(item.posicion()).isEqualTo(1);
        assertThat(item.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(item.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(item.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(item.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(item.titulo()).isEqualTo(TITULO);
        assertThat(item.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(item.referencia()).isEqualTo(PRODUCTO);
        assertThat(item.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(item.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(item.valorActual()).isEqualTo(VALOR_ACTUAL);
        assertThat(item.meta()).isEqualTo(META_MINI);
        assertThat(item.avance()).isEqualByComparingTo("0.5");
        assertThat(item.aporteEstimado()).isEqualTo(APORTE);
        assertThat(item.semanaInicio()).isEqualTo(SEMANA);
        assertThat(item.textoConIa()).isTrue();
    }

    private void entoncesSeCopiaElTextoYNoHayNumeros() {
        assertThat(item.id()).isEqualTo(OTRO_MINI_ID);
        assertThat(item.posicion()).isEqualTo(2);
        assertThat(item.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(item.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(item.titulo()).isEqualTo(TITULO_MANUAL);
        assertThat(item.justificacion()).isEqualTo(JUSTIFICACION_MANUAL);
        assertThat(item.semanaInicio()).isEqualTo(SEMANA);
        assertThat(item.textoConIa()).isTrue();
        assertThat(item.tipo()).isNull();
        assertThat(item.sentido()).isNull();
        assertThat(item.referencia()).isNull();
        assertThat(item.registroId()).isNull();
        assertThat(item.valorInicial()).isNull();
        assertThat(item.valorActual()).isNull();
        assertThat(item.meta()).isNull();
        assertThat(item.avance()).isNull();
        assertThat(item.aporteEstimado()).isNull();
    }
}
