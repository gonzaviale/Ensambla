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
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
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

    private static final int MENSAJES_RESTANTES = 4;

    private ProponerObjetivoConSamiResponse respuesta;
    private MiniPropuestoItem mini;

    @Test
    @DisplayName("Copia la respuesta, lo entendido, el tipo, los datos y lo que queda por escribir")
    void deShouldCopyEveryField() {
        whenSeResponde(propuestaConUnMedido(), new CuotaObjetivosIa(2, 5), MENSAJES_RESTANTES);

        thenSeCopianLaRespuestaYLoEntendido();
        thenSeCopianLosDatosYLaCuota();
    }

    @Test
    @DisplayName("Un mini-objetivo medido trae su métrica y todos sus números")
    void miniShouldCopyTheMeasuredMetric() {
        whenSeArmaElItem(MiniObjetivoPropuesto.medido(candidato()));

        thenElItemTraeLaMetricaYSusNumeros();
    }

    @Test
    @DisplayName("Un mini-objetivo manual trae sólo su texto, sin métrica ni números")
    void miniShouldCopyOnlyTheTextOfAManualMini() {
        whenSeArmaElItem(MiniObjetivoPropuesto.manual(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION), true));

        thenElItemTraeSoloElTexto();
    }

    @Test
    @DisplayName("Una meta en pesos no tiene tipo y no se puede guardar")
    void deShouldHaveNoTypeForAMonetaryGoal() {
        whenSeResponde(propuestaMonetaria(), new CuotaObjetivosIa(0, null), 5);

        thenNoHayTipoNiSePuedeGuardar();
        thenLaCuotaNoTieneLimite();
    }

    // --- act ---
    private void whenSeResponde(PropuestaObjetivo propuesta, CuotaObjetivosIa cuota, int mensajesRestantes) {
        respuesta = ProponerObjetivoConSamiResponse.de(
                propuesta, List.of(new DatoUsado(FuenteDato.ZONA, false)), mensajesRestantes, cuota);
    }

    private void whenSeArmaElItem(MiniObjetivoPropuesto propuesto) {
        mini = MiniPropuestoItem.de(propuesto);
    }

    // --- assert ---
    private void thenSeCopianLaRespuestaYLoEntendido() {
        assertThat(respuesta.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(respuesta.entendido()).isEqualTo(ENTENDIDO);
        assertThat(respuesta.tipo()).isEqualTo(TipoObjetivo.CON_DATOS);
        assertThat(respuesta.esMonetario()).isFalse();
        assertThat(respuesta.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(respuesta.mensajesRestantes()).isEqualTo(MENSAJES_RESTANTES);
        assertThat(respuesta.puedeGuardar()).isTrue();
    }

    private void thenSeCopianLosDatosYLaCuota() {
        assertThat(respuesta.cuota().usadas()).isEqualTo(2);
        assertThat(respuesta.cuota().limite()).isEqualTo(5);
        assertThat(respuesta.cuota().restantes()).isEqualTo(3);
        assertThat(respuesta.datos()).singleElement().satisfies(dato -> {
            assertThat(dato.fuente()).isEqualTo(FuenteDato.ZONA);
            assertThat(dato.hay()).isFalse();
        });
    }

    private void thenElItemTraeLaMetricaYSusNumeros() {
        assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(mini.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(mini.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(mini.referencia()).isEqualTo(PRODUCTO);
        assertThat(mini.titulo()).isEqualTo(TITULO);
        assertThat(mini.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(mini.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(mini.meta()).isEqualTo(META_MINI);
        assertThat(mini.aporteEstimado()).isEqualTo(APORTE);
        assertThat(mini.textoConIa()).isFalse();
    }

    private void thenElItemTraeSoloElTexto() {
        assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(mini.titulo()).isEqualTo(TITULO_ACCION);
        assertThat(mini.justificacion()).isEqualTo(JUSTIFICACION_ACCION);
        assertThat(mini.textoConIa()).isTrue();
        assertThat(mini.tipo()).isNull();
        assertThat(mini.registroId()).isNull();
        assertThat(mini.referencia()).isNull();
        assertThat(mini.valorInicial()).isNull();
        assertThat(mini.meta()).isNull();
        assertThat(mini.aporteEstimado()).isNull();
    }

    private void thenNoHayTipoNiSePuedeGuardar() {
        assertThat(respuesta.tipo()).isNull();
        assertThat(respuesta.entendido()).isNull();
        assertThat(respuesta.esMonetario()).isTrue();
        assertThat(respuesta.puedeGuardar()).isFalse();
        assertThat(respuesta.minis()).isEmpty();
    }

    private void thenLaCuotaNoTieneLimite() {
        assertThat(respuesta.cuota().limite()).isNull();
        assertThat(respuesta.cuota().restantes()).isNull();
    }

    // --- helpers ---
    private static PropuestaObjetivo propuestaConUnMedido() {
        return new PropuestaObjetivo(RESPUESTA_IA, ENTENDIDO, false,
                List.of(MiniObjetivoPropuesto.medido(candidato())), OrigenPropuesta.IA);
    }

    private static PropuestaObjetivo propuestaMonetaria() {
        return new PropuestaObjetivo(RESPUESTA_IA, null, true, List.of(), OrigenPropuesta.IA);
    }
}
