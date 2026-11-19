package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.RegistroTallerItem;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TallerPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.nuevo();
    private static final ConversarTallerRequest.Estado SIN_PIEZAS = new ConversarTallerRequest.Estado("", "mosaic", List.of());

    private final TallerPresentationMapper mapper = new TallerPresentationMapper();
    private ConversarEnTallerRequest pedido;
    private ConversarTallerResponse respuesta;
    private Throwable error;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, el mensaje sin espacios y el nombre del sistema")
    void armaElPedidoConElMensajeRecortado() {
        cuandoLaPersonaEscribeConUnInventarioYUnSaludo();

        entoncesElPedidoEsDelUsuarioConElMensaje("sumá gastos", "Mi almacén");
        entoncesNoHayFoto();
    }

    @Test
    @DisplayName("Pasa cada pieza con sus campos, registros y nombres, y el historial")
    void pasaCadaPiezaConSusCamposRegistrosYNombres() {
        cuandoLaPersonaEscribeConUnInventarioYUnSaludo();

        entoncesLaPrimeraPiezaTiene("Producto", new RegistroTallerItem("r1", Map.of("Producto", "Yerba")), "Yerba", "Fideos");
        entoncesElHistorialDice("Hola");
    }

    @Test
    @DisplayName("Una pieza sin registros ni nombres en el body (un frontend viejo) llega con listas vacías")
    void unaPiezaSinRegistrosNiNombresLlegaVacia() {
        cuandoLaPersonaEscribe(new ConversarTallerRequest("hola", new ConversarTallerRequest.Estado("", "mosaic",
                List.of(new ConversarTallerRequest.Pieza("e1", "inventory", "Inventario", null, List.of(), null, null))),
                null, null));

        entoncesLaPrimeraPiezaNoTieneRegistrosNiNombres();
    }

    @Test
    @DisplayName("Sin historial en el body, el pedido lleva una lista vacía")
    void sinHistorialElPedidoLlevaUnaListaVacia() {
        cuandoLaPersonaEscribe(new ConversarTallerRequest("hola", SIN_PIEZAS, null, null));

        entoncesNoHayHistorial();
    }

    @Test
    @DisplayName("La foto adjunta llega decodificada, con su tipo")
    void laFotoAdjuntaLlegaDecodificada() {
        cuandoLaPersonaMandaLaFoto("image/png");

        entoncesLaFotoEs("image/png", 1, 2, 3);
    }

    @Test
    @DisplayName("Una foto de un tipo que no va se rechaza con un mensaje para la persona")
    void unaFotoDeUnTipoQueNoVaSeRechaza() {
        cuandoSeIntenta(() -> cuandoLaPersonaMandaLaFoto("image/gif"));

        entoncesSeRechazaPorque(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Mapea la respuesta con acciones y botonera")
    void mapeaLaRespuestaConAccionesYBotonera() {
        cuandoSamiResponde(new ConversarEnTallerResponse("Listo",
                List.of(new ConversarEnTallerResponse.AccionTallerItem("agregar_pieza", Map.of("tipo", "expenses"))),
                List.of(new ConversarEnTallerResponse.OpcionesTallerItem("Seguir", "¿Seguimos?", List.of("Sí", "No")))));

        entoncesLaRespuestaDice("Listo");
        entoncesLaPrimeraAccionEs("agregar_pieza", "tipo", "expenses");
        entoncesLaPrimeraBotoneraEs("Seguir", "Sí", "No");
    }

    @Test
    @DisplayName("Sin botonera la respuesta no lleva opciones")
    void sinBotoneraLaRespuestaNoLlevaOpciones() {
        cuandoSamiResponde(new ConversarEnTallerResponse("Hola", List.of(), List.of()));

        entoncesNoHayBotonera();
    }

    private void cuandoLaPersonaEscribeConUnInventarioYUnSaludo() {
        cuandoLaPersonaEscribe(new ConversarTallerRequest("  sumá gastos  ",
                new ConversarTallerRequest.Estado("Mi almacén", "mosaic", List.of(new ConversarTallerRequest.Pieza(
                        "e1", "inventory", "Inventario", "General",
                        List.of(new ConversarTallerRequest.Campo("Producto", "text")),
                        List.of(new ConversarTallerRequest.Registro("r1", Map.of("Producto", "Yerba"))),
                        List.of("Yerba", "Fideos")))),
                List.of(new ConversarTallerRequest.Mensaje("sami", "Hola")), null));
    }

    private void cuandoLaPersonaMandaLaFoto(String tipo) {
        cuandoLaPersonaEscribe(new ConversarTallerRequest("te paso la foto", SIN_PIEZAS, null,
                new ConversarTallerRequest.Imagen(tipo, "AQID")));
    }

    private void cuandoLaPersonaEscribe(ConversarTallerRequest body) {
        pedido = mapper.toRequest(USUARIO_ID, body);
    }

    private void cuandoSamiResponde(ConversarEnTallerResponse deSami) {
        respuesta = mapper.toResponse(deSami);
    }

    private void cuandoSeIntenta(Runnable accion) {
        error = catchThrowable(accion::run);
    }

    private PiezaTallerItem laPrimeraPieza() {
        return pedido.estado().piezas().getFirst();
    }

    private void entoncesElPedidoEsDelUsuarioConElMensaje(String mensaje, String sistema) {
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.mensaje()).isEqualTo(mensaje);
        assertThat(pedido.estado().nombre()).isEqualTo(sistema);
    }

    private void entoncesNoHayFoto() {
        assertThat(pedido.imagen()).isNull();
    }

    private void entoncesLaPrimeraPiezaTiene(String campo, RegistroTallerItem registro, String... nombres) {
        assertThat(laPrimeraPieza().campos()).extracting("etiqueta").containsExactly(campo);
        assertThat(laPrimeraPieza().registros()).containsExactly(registro);
        assertThat(laPrimeraPieza().nombres()).containsExactly(nombres);
    }

    private void entoncesElHistorialDice(String... textos) {
        assertThat(pedido.historial()).extracting("texto").containsExactly((Object[]) textos);
    }

    private void entoncesLaPrimeraPiezaNoTieneRegistrosNiNombres() {
        assertThat(laPrimeraPieza().registros()).isEmpty();
        assertThat(laPrimeraPieza().nombres()).isEmpty();
    }

    private void entoncesNoHayHistorial() {
        assertThat(pedido.historial()).isEmpty();
    }

    private void entoncesLaFotoEs(String tipo, int... bytes) {
        assertThat(pedido.imagen().tipo()).isEqualTo(tipo);
        assertThat(pedido.imagen().datos()).containsExactly(bytes);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesLaRespuestaDice(String texto) {
        assertThat(respuesta.respuesta()).isEqualTo(texto);
    }

    private void entoncesLaPrimeraAccionEs(String herramienta, String argumento, Object valor) {
        assertThat(respuesta.acciones().getFirst().herramienta()).isEqualTo(herramienta);
        assertThat(respuesta.acciones().getFirst().argumentos()).containsEntry(argumento, valor);
    }

    private void entoncesLaPrimeraBotoneraEs(String tema, String... opciones) {
        assertThat(respuesta.preguntas().getFirst().tema()).isEqualTo(tema);
        assertThat(respuesta.preguntas().getFirst().opciones()).containsExactly(opciones);
    }

    private void entoncesNoHayBotonera() {
        assertThat(respuesta.preguntas()).isEmpty();
    }
}