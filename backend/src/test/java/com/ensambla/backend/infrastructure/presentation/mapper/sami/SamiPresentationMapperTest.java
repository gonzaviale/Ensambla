package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.OpcionSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PreguntaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PropuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.CrearProyectoSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.CrearProyectoSamiRequest.ColumnaSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.RespuestaSamiRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SamiPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.nuevo();
    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();

    private final SamiPresentationMapper mapper = new SamiPresentationMapper();
    private ConversarConSamiRequest conversacion;
    private CrearProyectoDesdeSamiRequest creacion;
    private ConversarSamiResponse respuesta;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, el texto sin espacios sobrantes y las respuestas")
    void armaElPedidoConElTextoRecortadoYLasRespuestas() {
        cuandoLaPersonaEscribe(new ConversarSamiRequest("  vendo velas  ", List.of(new RespuestaSamiRequest("principal", "ventas"))));

        entoncesLaConversacionEsDelUsuarioConElTexto("vendo velas");
        entoncesLasRespuestasSon("principal", "ventas");
    }

    @Test
    @DisplayName("Sin respuestas en el body, el pedido lleva una lista vacía")
    void sinRespuestasElPedidoLlevaUnaListaVacia() {
        cuandoLaPersonaEscribe(new ConversarSamiRequest("hola", null));

        entoncesNoHayRespuestas();
    }

    @Test
    @DisplayName("Arma el pedido de crear proyecto con el usuario de la sesión, la plantilla y el nombre")
    void armaElPedidoDeCrearProyecto() {
        cuandoSeCreaElProyectoConLasColumnas();

        entoncesElProyectoEsDelUsuarioConLaPlantilla("Velas");
    }

    @Test
    @DisplayName("Arma el pedido de crear proyecto con las columnas aceptadas")
    void armaElPedidoDeCrearProyectoConLasColumnas() {
        cuandoSeCreaElProyectoConLasColumnas();

        entoncesLasColumnasSonSaldoPendienteYFormaDePago();
    }

    @Test
    @DisplayName("Mapea una respuesta con pregunta")
    void mapeaUnaRespuestaConPregunta() {
        cuandoSamiResponde(new ConversarConSamiResponse("PREGUNTA",
                new PreguntaSamiItem("principal", "¿Qué querés anotar?", List.of(new OpcionSamiItem("ventas", "Vender"))),
                null, 1));

        entoncesPreguntaConLaOpcion("ventas", "Vender");
        entoncesSeHicieronPreguntas(1);
    }

    @Test
    @DisplayName("Mapea una respuesta con propuesta")
    void mapeaUnaRespuestaConPropuesta() {
        cuandoSamiResponde(new ConversarConSamiResponse("PROPUESTA", null,
                new PropuestaSamiItem(PLANTILLA_ID, "Ventas",
                        List.of(new ColumnaSamiItem("Saldo pendiente", "NUMERO", List.of())), 0.9, "ALGORITMO"),
                0));

        entoncesProponeLaPlantillaConLaColumna("Saldo pendiente", "NUMERO", "ALGORITMO");
    }

    private void cuandoLaPersonaEscribe(ConversarSamiRequest body) {
        conversacion = mapper.toRequest(USUARIO_ID, body);
    }

    private void cuandoSeCreaElProyectoConLasColumnas() {
        creacion = mapper.toCrearProyectoRequest(USUARIO_ID, new CrearProyectoSamiRequest(PLANTILLA_ID, "Velas", List.of(
                new ColumnaSamiRequest("Saldo pendiente", "NUMERO", null),
                new ColumnaSamiRequest("Forma de pago", "OPCION", List.of("A", "B")))));
    }

    private void cuandoSamiResponde(ConversarConSamiResponse deSami) {
        respuesta = mapper.toResponse(deSami);
    }

    private void entoncesLaConversacionEsDelUsuarioConElTexto(String texto) {
        assertThat(conversacion.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(conversacion.texto()).isEqualTo(texto);
    }

    private void entoncesLasRespuestasSon(String pregunta, String opcion) {
        assertThat(conversacion.respuestas()).extracting("preguntaId", "opcionId").containsExactly(tuple(pregunta, opcion));
    }

    private void entoncesNoHayRespuestas() {
        assertThat(conversacion.respuestas()).isEmpty();
    }

    private void entoncesElProyectoEsDelUsuarioConLaPlantilla(String nombre) {
        assertThat(creacion.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(creacion.plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(creacion.nombre()).isEqualTo(nombre);
    }

    private void entoncesLasColumnasSonSaldoPendienteYFormaDePago() {
        assertThat(creacion.columnas()).extracting("nombre", "tipo", "opciones").containsExactly(
                tuple("Saldo pendiente", "NUMERO", null),
                tuple("Forma de pago", "OPCION", List.of("A", "B")));
    }

    private void entoncesPreguntaConLaOpcion(String id, String texto) {
        assertThat(respuesta.tipo()).isEqualTo("PREGUNTA");
        assertThat(respuesta.pregunta().opciones()).extracting("id", "texto").containsExactly(tuple(id, texto));
        assertThat(respuesta.propuesta()).isNull();
    }

    private void entoncesSeHicieronPreguntas(int preguntas) {
        assertThat(respuesta.preguntasHechas()).isEqualTo(preguntas);
    }

    private void entoncesProponeLaPlantillaConLaColumna(String nombre, String tipo, String origen) {
        assertThat(respuesta.pregunta()).isNull();
        assertThat(respuesta.propuesta().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(respuesta.propuesta().columnas()).extracting("nombre", "tipo").containsExactly(tuple(nombre, tipo));
        assertThat(respuesta.propuesta().origen()).isEqualTo(origen);
    }
}