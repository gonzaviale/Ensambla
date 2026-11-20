package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.RegistroTallerItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarEnTallerDtosTest {

    private EstadoTallerItem estadoItem;
    private EstadoTaller estado;
    private ConversarEnTallerRequest pedido;
    private ConversarEnTallerResponse respuesta;
    private List<MensajeTaller> historial;
    private ImagenTaller imagen;

    @Test
    @DisplayName("Convierte el estado del DTO al del dominio con sus piezas y campos")
    void convierteElEstadoDelDtoAlDelDominioConSusPiezasYCampos() {
        dadoUnEstadoConUnaPiezaDeInventarioConUnCampo();

        cuandoSeConvierteElEstado();

        entoncesElEstadoDelDominioTieneLaPiezaConSuCampo();
    }

    @Test
    @DisplayName("Convierte los registros de cada pieza")
    void convierteLosRegistrosDeCadaPieza() {
        dadoUnaPiezaConDosRegistrosYOtraSinLista();

        cuandoSeConvierteElEstado();

        entoncesLosRegistrosSeConviertenYLosFaltantesQuedanVacios();
    }

    @Test
    @DisplayName("Convierte los nombres existentes de una pieza; sin lista quedan vacíos")
    void convierteLosNombresExistentesDeUnaPiezaSinListaQuedanVacios() {
        dadoUnaPiezaConNombresExistentesYOtraSinLista();

        cuandoSeConvierteElEstado();

        entoncesLosNombresSeConviertenYLosFaltantesQuedanVacios();
    }

    @Test
    @DisplayName("Convierte el historial de mensajes")
    void convierteElHistorialDeMensajes() {
        dadoUnPedidoConUnMensajeDeSamiEnElHistorial();

        cuandoSeConvierteElHistorial();

        entoncesElHistorialTieneElMensajeDeSami();
    }

    @Test
    @DisplayName("Arma la respuesta con el texto, las acciones y la botonera")
    void armaLaRespuestaConElTextoLasAccionesYLaBotonera() {
        cuandoSeArmaLaRespuestaConUnaAccionYUnaBotonera();

        entoncesLaRespuestaTraeElTextoLasAccionesYLaBotonera();
    }

    @Test
    @DisplayName("Sin botonera la respuesta no lleva opciones")
    void sinBotoneraLaRespuestaNoLlevaOpciones() {
        cuandoSeArmaLaRespuestaSinAccionesNiBotonera();

        entoncesLaRespuestaNoLlevaOpcionesNiAcciones();
    }

    @Test
    @DisplayName("Convierte la foto adjunta con su tipo y sus bytes")
    void convierteLaFotoAdjuntaConSuTipoYSusBytes() {
        dadoUnPedidoConUnaFotoWebp();

        cuandoSeConvierteLaFoto();

        entoncesLaFotoTraeSuTipoYSusBytes();
    }

    @Test
    @DisplayName("Sin foto adjunta no hay imagen")
    void sinFotoAdjuntaNoHayImagen() {
        dadoUnPedidoSinFoto();

        cuandoSeConvierteLaFoto();

        entoncesNoHayImagen();
    }

    // --- dado ---

    private void dadoUnEstadoConUnaPiezaDeInventarioConUnCampo() {
        estadoItem = new EstadoTallerItem("Mi almacén", "mosaic", List.of(new PiezaTallerItem(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTallerItem("Producto", "text")))));
    }

    private void dadoUnaPiezaConDosRegistrosYOtraSinLista() {
        estadoItem = new EstadoTallerItem("S", "mosaic", List.of(
                new PiezaTallerItem("e1", "sales", "Ventas", null, List.of(), List.of(
                        new RegistroTallerItem("r1", Map.of("Producto", "Vela")),
                        new RegistroTallerItem("r2", null))),
                new PiezaTallerItem("e2", "clients", "Clientes", null, List.of(), null)));
    }

    private void dadoUnaPiezaConNombresExistentesYOtraSinLista() {
        estadoItem = new EstadoTallerItem("S", "mosaic", List.of(
                new PiezaTallerItem("e1", "clients", "Clientes", null, List.of(), List.of(), List.of("Ana", "Vero")),
                new PiezaTallerItem("e2", "inventory", "Stock", null, List.of(), List.of(), null)));
    }

    private void dadoUnPedidoConUnMensajeDeSamiEnElHistorial() {
        pedido = new ConversarEnTallerRequest(null, null, null, List.of(new MensajeTallerItem("sami", "Hola")), null);
    }

    private void dadoUnPedidoConUnaFotoWebp() {
        pedido = conImagen(new ImagenTallerItem("image/webp", new byte[]{7, 8}));
    }

    private void dadoUnPedidoSinFoto() {
        pedido = conImagen(null);
    }

    // --- cuando ---

    private void cuandoSeConvierteElEstado() {
        estado = estadoItem.aEstadoTaller();
    }

    private void cuandoSeConvierteElHistorial() {
        historial = pedido.historialTaller();
    }

    private void cuandoSeConvierteLaFoto() {
        imagen = pedido.imagenTaller();
    }

    private void cuandoSeArmaLaRespuestaConUnaAccionYUnaBotonera() {
        respuesta = ConversarEnTallerResponse.de(new RespuestaTaller(
                "Listo",
                List.of(new AccionTaller("agregar_pieza", Map.of("tipo", "expenses"))),
                List.of(new OpcionesTaller("Seguir", "¿Seguimos?", List.of("Sí", "No")))));
    }

    private void cuandoSeArmaLaRespuestaSinAccionesNiBotonera() {
        respuesta = ConversarEnTallerResponse.de(new RespuestaTaller("Hola", List.of(), List.of()));
    }

    // --- entonces ---

    private void entoncesElEstadoDelDominioTieneLaPiezaConSuCampo() {
        assertThat(estado).isEqualTo(new EstadoTaller("Mi almacén", "mosaic", List.of(new PiezaTaller(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTaller("Producto", "text"))))));
    }

    private void entoncesLosRegistrosSeConviertenYLosFaltantesQuedanVacios() {
        assertThat(estado.piezas().getFirst().registros()).containsExactly(
                new RegistroTaller("r1", Map.of("Producto", "Vela")), new RegistroTaller("r2", Map.of()));
        assertThat(estado.piezas().getLast().registros()).isEmpty();
    }

    private void entoncesLosNombresSeConviertenYLosFaltantesQuedanVacios() {
        assertThat(estado.piezas().getFirst().nombres()).containsExactly("Ana", "Vero");
        assertThat(estado.piezas().getLast().nombres()).isEmpty();
    }

    private void entoncesElHistorialTieneElMensajeDeSami() {
        assertThat(historial).containsExactly(new MensajeTaller("sami", "Hola"));
    }

    private void entoncesLaRespuestaTraeElTextoLasAccionesYLaBotonera() {
        assertThat(respuesta.respuesta()).isEqualTo("Listo");
        assertThat(respuesta.acciones()).extracting("herramienta").containsExactly("agregar_pieza");
        assertThat(respuesta.acciones().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(respuesta.preguntas()).hasSize(1);
        assertThat(respuesta.preguntas().getFirst().tema()).isEqualTo("Seguir");
        assertThat(respuesta.preguntas().getFirst().pregunta()).isEqualTo("¿Seguimos?");
        assertThat(respuesta.preguntas().getFirst().opciones()).containsExactly("Sí", "No");
    }

    private void entoncesLaRespuestaNoLlevaOpcionesNiAcciones() {
        assertThat(respuesta.preguntas()).isEmpty();
        assertThat(respuesta.acciones()).isEmpty();
    }

    private void entoncesLaFotoTraeSuTipoYSusBytes() {
        assertThat(imagen.tipo()).isEqualTo("image/webp");
        assertThat(imagen.datos()).containsExactly(7, 8);
    }

    private void entoncesNoHayImagen() {
        assertThat(imagen).isNull();
    }

    // --- datos ---

    private static ConversarEnTallerRequest conImagen(ImagenTallerItem imagen) {
        return new ConversarEnTallerRequest(null, null, null, List.of(), imagen);
    }
}
