package com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami;

import com.ensambla.backend.domain.model.objetivo.AccionManual;
import com.ensambla.backend.domain.model.objetivo.DatoUsado;
import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoPropuesto;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse.MiniPropuestoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static org.assertj.core.api.Assertions.assertThat;

class ProponerObjetivoConSamiResponseTest {

    @Test
    @DisplayName("Copia la respuesta, lo entendido, el tipo, los datos y lo que queda por escribir")
    void deShouldCopyEveryField() {
        PropuestaObjetivo propuesta = new PropuestaObjetivo(RESPUESTA_IA, ENTENDIDO, false,
                List.of(MiniObjetivoPropuesto.medido(candidato())), OrigenPropuesta.IA);

        ProponerObjetivoConSamiResponse result = ProponerObjetivoConSamiResponse.de(
                propuesta, List.of(new DatoUsado(FuenteDato.ZONA, false)), 4);

        assertThat(result.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(result.entendido()).isEqualTo(ENTENDIDO);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.CON_DATOS);
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(result.mensajesRestantes()).isEqualTo(4);
        assertThat(result.puedeGuardar()).isTrue();
        assertThat(result.datos()).singleElement().satisfies(dato -> {
            assertThat(dato.fuente()).isEqualTo(FuenteDato.ZONA);
            assertThat(dato.hay()).isFalse();
        });
    }

    @Test
    @DisplayName("Un mini-objetivo medido trae su métrica y todos sus números")
    void miniShouldCopyTheMeasuredMetric() {
        MiniPropuestoItem result = MiniPropuestoItem.de(MiniObjetivoPropuesto.medido(candidato()));

        assertThat(result.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
        assertThat(result.titulo()).isEqualTo(TITULO);
        assertThat(result.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.meta()).isEqualTo(META_MINI);
        assertThat(result.aporteEstimado()).isEqualTo(APORTE);
        assertThat(result.textoConIa()).isFalse();
    }

    @Test
    @DisplayName("Un mini-objetivo manual trae sólo su texto, sin métrica ni números")
    void miniShouldCopyOnlyTheTextOfAManualMini() {
        MiniPropuestoItem result = MiniPropuestoItem.de(
                MiniObjetivoPropuesto.manual(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION), true));

        assertThat(result.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(result.titulo()).isEqualTo(TITULO_ACCION);
        assertThat(result.justificacion()).isEqualTo(JUSTIFICACION_ACCION);
        assertThat(result.textoConIa()).isTrue();
        assertThat(result.tipo()).isNull();
        assertThat(result.registroId()).isNull();
        assertThat(result.referencia()).isNull();
        assertThat(result.valorInicial()).isNull();
        assertThat(result.meta()).isNull();
        assertThat(result.aporteEstimado()).isNull();
    }

    @Test
    @DisplayName("Una meta en pesos no tiene tipo y no se puede guardar")
    void deShouldHaveNoTypeForAMonetaryGoal() {
        PropuestaObjetivo propuesta = new PropuestaObjetivo(RESPUESTA_IA, null, true, List.of(), OrigenPropuesta.IA);

        ProponerObjetivoConSamiResponse result = ProponerObjetivoConSamiResponse.de(propuesta, List.of(), 5);

        assertThat(result.tipo()).isNull();
        assertThat(result.entendido()).isNull();
        assertThat(result.esMonetario()).isTrue();
        assertThat(result.puedeGuardar()).isFalse();
        assertThat(result.minis()).isEmpty();
    }
}
