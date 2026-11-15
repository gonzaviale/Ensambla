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

    @Test
    @DisplayName("Arma el pedido de la propuesta con el usuario de la sesión y los mensajes en su orden")
    void toProponerRequestShouldCarryUsuarioAndMessages() {
        ProponerObjetivoWebRequest web = new ProponerObjetivoWebRequest(List.of(
                new MensajeWebRequest(RolMensaje.PERSONA, OBJETIVO),
                new MensajeWebRequest(RolMensaje.SAMI, "¿Qué producto?")));

        ProponerObjetivoConSamiRequest result = mapper.toProponerRequest(USUARIO_ID, web);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.mensajes()).containsExactly(
                new MensajeObjetivo(RolMensaje.PERSONA, OBJETIVO),
                new MensajeObjetivo(RolMensaje.SAMI, "¿Qué producto?"));
    }

    @Test
    @DisplayName("Arma el pedido de guardar con el usuario de la sesión, el texto, el seguimiento y cada mini-objetivo")
    void toGuardarRequestShouldCarryEveryField() {
        GuardarObjetivoConSamiWebRequest web = new GuardarObjetivoConSamiWebRequest(OBJETIVO, false, List.of(
                new MiniElegidoWebRequest(ModoMiniObjetivo.MEDIDO, TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID,
                        PRODUCTO, null, null),
                new MiniElegidoWebRequest(ModoMiniObjetivo.MANUAL, null, null, null, TITULO_ACCION,
                        JUSTIFICACION_ACCION)));

        GuardarObjetivoConSamiRequest result = mapper.toGuardarRequest(USUARIO_ID, web);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.texto()).isEqualTo(OBJETIVO);
        assertThat(result.seguimientoManual()).isFalse();
        assertThat(result.minis()).hasSize(2);
        assertThat(result.minis().get(0).modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(result.minis().get(0).tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.minis().get(0).registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.minis().get(0).referencia()).isEqualTo(PRODUCTO);
        assertThat(result.minis().get(1).modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(result.minis().get(1).titulo()).isEqualTo(TITULO_ACCION);
        assertThat(result.minis().get(1).justificacion()).isEqualTo(JUSTIFICACION_ACCION);
    }

    @Test
    @DisplayName("Sin lista de mini-objetivos (el seguimiento) se manda una lista vacía")
    void toGuardarRequestShouldUseAnEmptyListWithoutMinis() {
        GuardarObjetivoConSamiRequest result = mapper.toGuardarRequest(
                USUARIO_ID, new GuardarObjetivoConSamiWebRequest(OBJETIVO, true, null));

        assertThat(result.seguimientoManual()).isTrue();
        assertThat(result.minis()).isEmpty();
    }

    @Test
    @DisplayName("Mapea la propuesta con todos sus datos")
    void toResponseShouldMapTheProposal() {
        ProponerObjetivoConSamiResponse propuesta = new ProponerObjetivoConSamiResponse(
                RESPUESTA_IA, ENTENDIDO, TipoObjetivo.HIBRIDO, false,
                List.of(new ProponerObjetivoConSamiResponse.MiniPropuestoItem(ModoMiniObjetivo.MEDIDO,
                        TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO, "Título", "Razón", null, null, null,
                        false)),
                List.of(new ProponerObjetivoConSamiResponse.DatoItem(FuenteDato.ZONA, true)),
                OrigenPropuesta.IA, 4, true);

        PropuestaObjetivoWebResponse result = mapper.toResponse(propuesta);

        assertThat(result.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(result.entendido()).isEqualTo(ENTENDIDO);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(result.mensajesRestantes()).isEqualTo(4);
        assertThat(result.puedeGuardar()).isTrue();
        assertThat(result.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
            assertThat(mini.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
            assertThat(mini.registroId()).isEqualTo(PRODUCTO_ID);
            assertThat(mini.referencia()).isEqualTo(PRODUCTO);
            assertThat(mini.titulo()).isEqualTo("Título");
            assertThat(mini.justificacion()).isEqualTo("Razón");
            assertThat(mini.textoConIa()).isFalse();
        });
        assertThat(result.datos()).singleElement().satisfies(dato -> {
            assertThat(dato.fuente()).isEqualTo(FuenteDato.ZONA);
            assertThat(dato.hay()).isTrue();
        });
    }

    @Test
    @DisplayName("Mapea el objetivo guardado sin meta")
    void toResponseShouldMapTheSavedObjetivo() {
        ObjetivoResponse result = mapper.toResponse(ObjetivoItem.de(objetivoSinMeta()));

        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(result.meta()).isNull();
        assertThat(result.armadoConSami()).isTrue();
    }

    @Test
    @DisplayName("Si el pedido no dice nada del seguimiento manual, es que no")
    void toGuardarRequestShouldTreatAMissingFlagAsFalse() {
        GuardarObjetivoConSamiRequest result = mapper.toGuardarRequest(
                USUARIO_ID, new GuardarObjetivoConSamiWebRequest(OBJETIVO, null, List.of()));

        assertThat(result.seguimientoManual()).isFalse();
    }
}
