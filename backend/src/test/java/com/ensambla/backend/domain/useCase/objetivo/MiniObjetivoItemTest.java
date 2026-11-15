package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoItemTest {

    @Test
    @DisplayName("Copia todos los datos del mini-objetivo y le suma su sentido y su avance")
    void deShouldCopyEveryFieldAndAddDirectionAndProgress() {
        MiniObjetivo mini = miniObjetivo();
        mini.setValorActual(new BigDecimal("13391.50"));
        mini.setTextoConIa(true);

        MiniObjetivoItem result = MiniObjetivoItem.de(mini);

        assertThat(result.id()).isEqualTo(MINI_ID);
        assertThat(result.posicion()).isEqualTo(1);
        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(result.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(result.titulo()).isEqualTo(TITULO);
        assertThat(result.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.valorActual()).isEqualTo(new BigDecimal("13391.50"));
        assertThat(result.meta()).isEqualTo(META_MINI);
        assertThat(result.avance()).isEqualByComparingTo("0.5");
        assertThat(result.aporteEstimado()).isEqualTo(APORTE);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
        assertThat(result.textoConIa()).isTrue();
    }
}
