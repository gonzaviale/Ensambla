package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.MensajeObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.RolMensaje;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.GuardarObjetivoConSamiWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MensajeWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniElegidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PropuestaObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProponerObjetivoWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class SamiObjetivoPresentationMapperTest {

    private final SamiObjetivoPresentationMapper mapper =
            new SamiObjetivoPresentationMapper(new ObjetivoPresentationMapper());
    private ProponerObjetivoConSamiRequest pedidoDeProponer;
    private GuardarObjetivoConSamiRequest pedidoDeGuardar;
    private PropuestaObjetivoWebResponse propuestaMapeada;
    private ObjetivoResponse objetivoMapeado;

    @Test
    @DisplayName("Arma el pedido de la propuesta con el usuario de la sesión y los mensajes en su orden")
    void toProponerRequestShouldCarryUsuarioAndMessages() {
        whenSeArmaElPedidoDeProponer();

        thenElPedidoDeProponerTraeUsuarioYMensajesEnOrden();
    }

    @Test
    @DisplayName("Arma el pedido de guardar con el usuario de la sesión, el texto, el seguimiento y cada mini-objetivo")
    void toGuardarRequestShouldCarryEveryField() {
        whenSeArmaElPedidoDeGuardar(guardarConUnMedidoYUnManual());

        thenElPedidoDeGuardarTraeTodosLosCampos();
    }

    @Test
    @DisplayName("Sin lista de mini-objetivos (el seguimiento) se manda una lista vacía")
    void toGuardarRequestShouldUseAnEmptyListWithoutMinis() {
        whenSeArmaElPedidoDeGuardar(new GuardarObjetivoConSamiWebRequest(OBJETIVO, true, null));

        thenEsSeguimientoManualSinMinis();
    }

    @Test
    @DisplayName("Mapea la propuesta con todos sus datos")
    void toResponseShouldMapTheProposal() {
        whenSeMapeaLaPropuesta();

        thenSeMapeaLaPropuestaConTodosSusDatos();
    }

    @Test
    @DisplayName("Mapea el objetivo guardado sin meta")
    void toResponseShouldMapTheSavedObjetivo() {
        whenSeMapeaElObjetivoGuardado();

        thenElObjetivoNoTieneMetaYLoArmoSami();
    }

    @Test
    @DisplayName("Si el pedido no dice nada del seguimiento manual, es que no")
    void toGuardarRequestShouldTreatAMissingFlagAsFalse() {
        whenSeArmaElPedidoDeGuardar(new GuardarObjetivoConSamiWebRequest(OBJETIVO, null, List.of()));

        thenNoEsSeguimientoManual();
    }

    // --- act ---
    private void whenSeArmaElPedidoDeProponer() {
        pedidoDeProponer = mapper.toProponerRequest(USUARIO_ID, new ProponerObjetivoWebRequest(List.of(
                new MensajeWebRequest(RolMensaje.PERSONA, OBJETIVO),
                new MensajeWebRequest(RolMensaje.SAMI, "¿Qué producto?"))));
    }

    private void whenSeArmaElPedidoDeGuardar(GuardarObjetivoConSamiWebRequest web) {
        pedidoDeGuardar = mapper.toGuardarRequest(USUARIO_ID, web);
    }

    private void whenSeMapeaLaPropuesta() {
        propuestaMapeada = mapper.toResponse(propuestaDeSami());
    }

    private void whenSeMapeaElObjetivoGuardado() {
        objetivoMapeado = mapper.toResponse(ObjetivoItem.de(objetivoSinMeta()));
    }

    // --- assert ---
    private void thenElPedidoDeProponerTraeUsuarioYMensajesEnOrden() {
        assertThat(pedidoDeProponer.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeProponer.mensajes()).containsExactly(
                new MensajeObjetivo(RolMensaje.PERSONA, OBJETIVO),
                new MensajeObjetivo(RolMensaje.SAMI, "¿Qué producto?"));
    }

    private void thenElPedidoDeGuardarTraeTodosLosCampos() {
        assertThat(pedidoDeGuardar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeGuardar.texto()).isEqualTo(OBJETIVO);
        assertThat(pedidoDeGuardar.seguimientoManual()).isFalse();
        assertThat(pedidoDeGuardar.minis()).hasSize(2);
        assertThat(pedidoDeGuardar.minis().get(0).modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(pedidoDeGuardar.minis().get(0).tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(pedidoDeGuardar.minis().get(0).registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(pedidoDeGuardar.minis().get(0).referencia()).isEqualTo(PRODUCTO);
        assertThat(pedidoDeGuardar.minis().get(1).modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(pedidoDeGuardar.minis().get(1).titulo()).isEqualTo(TITULO_ACCION);
        assertThat(pedidoDeGuardar.minis().get(1).justificacion()).isEqualTo(JUSTIFICACION_ACCION);
    }

    private void thenEsSeguimientoManualSinMinis() {
        assertThat(pedidoDeGuardar.seguimientoManual()).isTrue();
        assertThat(pedidoDeGuardar.minis()).isEmpty();
    }

    private void thenNoEsSeguimientoManual() {
        assertThat(pedidoDeGuardar.seguimientoManual()).isFalse();
    }

    private void thenSeMapeaLaPropuestaConTodosSusDatos() {
        assertThat(propuestaMapeada.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(propuestaMapeada.entendido()).isEqualTo(ENTENDIDO);
        assertThat(propuestaMapeada.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(propuestaMapeada.esMonetario()).isFalse();
        assertThat(propuestaMapeada.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(propuestaMapeada.mensajesRestantes()).isEqualTo(4);
        assertThat(propuestaMapeada.puedeGuardar()).isTrue();
        assertThat(propuestaMapeada.cuota().usadas()).isEqualTo(2);
        assertThat(propuestaMapeada.cuota().limite()).isEqualTo(5);
        assertThat(propuestaMapeada.cuota().restantes()).isEqualTo(3);
        assertThat(propuestaMapeada.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
            assertThat(mini.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
            assertThat(mini.registroId()).isEqualTo(PRODUCTO_ID);
            assertThat(mini.referencia()).isEqualTo(PRODUCTO);
            assertThat(mini.titulo()).isEqualTo("Título");
            assertThat(mini.justificacion()).isEqualTo("Razón");
            assertThat(mini.textoConIa()).isFalse();
        });
        assertThat(propuestaMapeada.datos()).singleElement().satisfies(dato -> {
            assertThat(dato.fuente()).isEqualTo(FuenteDato.ZONA);
            assertThat(dato.hay()).isTrue();
        });
    }

    private void thenElObjetivoNoTieneMetaYLoArmoSami() {
        assertThat(objetivoMapeado.id()).isEqualTo(OBJETIVO_ID);
        assertThat(objetivoMapeado.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoMapeado.meta()).isNull();
        assertThat(objetivoMapeado.armadoConSami()).isTrue();
    }

    // --- helpers ---
    private static GuardarObjetivoConSamiWebRequest guardarConUnMedidoYUnManual() {
        return new GuardarObjetivoConSamiWebRequest(OBJETIVO, false, List.of(
                new MiniElegidoWebRequest(ModoMiniObjetivo.MEDIDO, TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID,
                        PRODUCTO, null, null),
                new MiniElegidoWebRequest(ModoMiniObjetivo.MANUAL, null, null, null, TITULO_ACCION,
                        JUSTIFICACION_ACCION)));
    }

    private static ProponerObjetivoConSamiResponse propuestaDeSami() {
        return new ProponerObjetivoConSamiResponse(
                RESPUESTA_IA, ENTENDIDO, TipoObjetivo.HIBRIDO, false,
                List.of(new ProponerObjetivoConSamiResponse.MiniPropuestoItem(ModoMiniObjetivo.MEDIDO,
                        TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO, "Título", "Razón", null, null, null,
                        false)),
                List.of(new ProponerObjetivoConSamiResponse.DatoItem(FuenteDato.ZONA, true)),
                OrigenPropuesta.IA, 4, true,
                new ProponerObjetivoConSamiResponse.CuotaItem(2, 5, 3));
    }
}
