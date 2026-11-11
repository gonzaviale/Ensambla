package com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ArmadoDeLista;
import com.ensambla.backend.domain.model.pedido.BuscadorDeComercios;
import com.ensambla.backend.domain.model.pedido.BuscadorDeProductos;
import com.ensambla.backend.domain.model.pedido.CatalogoDeProductos;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.pedido.TextosPedido;
import com.ensambla.backend.domain.model.whatsapp.RespuestaChat;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.InterpretePedidoIaPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.ComercioRepository;
import com.ensambla.backend.domain.repository.pedido.ConversacionClienteRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Atiende, de a uno y en orden, lo que escribieron los clientes por WhatsApp. Registra al cliente la primera vez que
 * escribe y lo acompaña en la conversación: lo saluda, le pregunta a qué comercio quiere pedirle (por nombre, ofreciendo
 * seguir con el último) y, ya elegido el comercio, le arma la lista de compra con el stock del comercio. Tiene muchas
 * dependencias a propósito: ningún caso de uso llama a otro, así que los pasos viven acá y la lógica pura en el modelo.
 */
@RequiredArgsConstructor
public class AtenderMensajeCliente {

    private static final String MOTIVO_PERDIDO = "Se perdió mientras se atendía";

    private final MensajeClienteRepository mensajeRepository;
    private final ClienteWhatsappRepository clienteRepository;
    private final ConversacionClienteRepository conversacionRepository;
    private final ComercioRepository comercioRepository;
    private final ProyectoRepository proyectoRepository;
    private final DatosDeProyectoService datosDeProyectoService;
    private final ControlPlanService controlPlanService;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final InterpretePedidoIaPort interpreteIa;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;
    private final Duration vigenciaReclamo;
    private final int limiteDiarioSami;

    /** @return si había un mensaje esperando; quien lo llama insiste mientras haya */
    public boolean execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        recuperarPerdidos(ahora);
        Optional<MensajeCliente> reclamado = mensajeRepository.reclamarSiguiente(ahora, vigenciaReclamo);
        reclamado.ifPresent(this::atender);
        return reclamado.isPresent();
    }

    /** Un mensaje reclamado que nunca terminó (se cayó el proceso): no se reintenta, para no contestar dos veces. */
    private void recuperarPerdidos(OffsetDateTime ahora) {
        for (MensajeCliente perdido : mensajeRepository.vencidos(ahora)) {
            perdido.fallar(MOTIVO_PERDIDO, ahora);
            mensajeRepository.save(perdido);
            mensajeria.enviarTexto(perdido.getTelefono(), TextosWhatsapp.ERROR_TEMPORAL);
        }
    }

    private void atender(MensajeCliente mensaje) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        try {
            String respuesta = transaccionPort.ejecutar(() -> conversar(mensaje, ahora));
            mensajeria.enviarTexto(mensaje.getTelefono(), respuesta);
            mensaje.atender(ahora);
        } catch (BusinessRuleException e) {
            mensajeria.enviarTexto(mensaje.getTelefono(), e.getMessage());
            mensaje.atender(ahora);
        } catch (RuntimeException e) {
            mensajeria.enviarTexto(mensaje.getTelefono(), TextosWhatsapp.ERROR_TEMPORAL);
            mensaje.fallar(e.getClass().getSimpleName() + ": " + e.getMessage(), ahora);
        }
        mensajeRepository.save(mensaje);
    }

    /** Hace avanzar la conversación del cliente con lo que escribió y devuelve lo que Sami le contesta. */
    private String conversar(MensajeCliente mensaje, OffsetDateTime ahora) {
        ClienteWhatsapp cliente = registrarCliente(mensaje.getTelefono(), ahora);
        List<Comercio> comercios = comercioRepository.listarConWhatsapp();
        Optional<ConversacionCliente> existente = conversacionRepository.findByClienteId(cliente.getId());
        ConversacionCliente conversacion = existente
                .orElseGet(() -> ConversacionCliente.nueva(cliente.getId(), ahora));
        boolean retoma = existente.isPresent() && conversacion.estaVigente(ahora);
        String respuesta = retoma
                ? continuar(conversacion, cliente, mensaje.getTexto(), comercios, ahora)
                : saludar(conversacion, cliente, comercios, ahora);
        conversacionRepository.save(conversacion);
        return respuesta;
    }

    private ClienteWhatsapp registrarCliente(String telefono, OffsetDateTime ahora) {
        return clienteRepository.findByTelefono(telefono)
                .orElseGet(() -> clienteRepository.save(ClienteWhatsapp.nuevo(telefono, ahora)));
    }

    // --- empezar ---

    /** Primer mensaje, o vuelve después de un rato: saluda y, si ya pidió antes, le ofrece seguir con el último comercio. */
    private String saludar(ConversacionCliente conversacion, ClienteWhatsapp cliente, List<Comercio> comercios,
                           OffsetDateTime ahora) {
        Optional<Comercio> ultimo = comercioDe(comercios, cliente.getUltimoNegocioId());
        conversacion.reiniciar(ultimo.map(Comercio::id).orElse(null), ahora);
        return ultimo.map(comercio -> TextosPedido.bienvenidaConUltimo(comercio.nombre()))
                .orElse(TextosPedido.BIENVENIDA);
    }

    private String continuar(ConversacionCliente conversacion, ClienteWhatsapp cliente, String texto,
                             List<Comercio> comercios, OffsetDateTime ahora) {
        return switch (conversacion.getEstado()) {
            case ELIGIENDO_COMERCIO -> eligiendoComercio(conversacion, cliente, texto, comercios, ahora);
            case PIDIENDO -> pidiendo(conversacion, texto, comercios, ahora);
        };
    }

    // --- elegir el comercio ---

    private String eligiendoComercio(ConversacionCliente conversacion, ClienteWhatsapp cliente, String texto,
                                     List<Comercio> comercios, OffsetDateTime ahora) {
        RespuestaChat respuesta = RespuestaChat.de(texto);
        boolean estabaEligiendo = conversacion.ofreceElUltimo() || conversacion.ofreceOpciones();
        if (respuesta == RespuestaChat.NO && estabaEligiendo) {
            conversacion.reiniciar(null, ahora);
            return TextosPedido.PREGUNTAR_COMERCIO;
        }
        if (respuesta == RespuestaChat.SI && conversacion.ofreceElUltimo()) {
            return elegir(conversacion, cliente, comercioDe(comercios, conversacion.getNegocioId()), ahora);
        }
        if (respuesta == RespuestaChat.NUMERO && conversacion.ofreceOpciones()) {
            return elegirOpcion(conversacion, cliente, RespuestaChat.numero(texto), comercios, ahora);
        }
        return buscarPorNombre(conversacion, cliente, texto, comercios, ahora);
    }

    private String elegirOpcion(ConversacionCliente conversacion, ClienteWhatsapp cliente, int numero,
                                List<Comercio> comercios, OffsetDateTime ahora) {
        Optional<NegocioId> opcion = conversacion.opcion(numero);
        if (opcion.isEmpty()) {
            conversacion.tocar(ahora);
            return TextosPedido.opcionInvalida(conversacion.getOpciones().size());
        }
        return elegir(conversacion, cliente, comercioDe(comercios, opcion.get()), ahora);
    }

    private String buscarPorNombre(ConversacionCliente conversacion, ClienteWhatsapp cliente, String texto,
                                   List<Comercio> comercios, OffsetDateTime ahora) {
        List<Comercio> encontrados = BuscadorDeComercios.buscar(texto, comercios);
        if (encontrados.isEmpty()) {
            conversacion.tocar(ahora);
            return TextosPedido.comercioNoEncontrado(texto);
        }
        if (encontrados.size() == 1) {
            return elegir(conversacion, cliente, Optional.of(encontrados.getFirst()), ahora);
        }
        conversacion.ofrecerOpciones(encontrados.stream().map(Comercio::id).toList(), ahora);
        return TextosPedido.variosComercios(encontrados);
    }

    /** Deja al cliente pidiéndole a ese comercio y lo recuerda como el último; si ya no está disponible, vuelve a preguntar. */
    private String elegir(ConversacionCliente conversacion, ClienteWhatsapp cliente, Optional<Comercio> comercio,
                          OffsetDateTime ahora) {
        if (comercio.isEmpty()) {
            conversacion.reiniciar(null, ahora);
            return TextosPedido.COMERCIO_NO_DISPONIBLE;
        }
        conversacion.elegir(comercio.get().id(), ahora);
        cliente.setUltimoNegocioId(comercio.get().id());
        clienteRepository.save(cliente);
        return TextosPedido.comercioElegido(comercio.get().nombre());
    }

    // --- pedir ---

    private String pidiendo(ConversacionCliente conversacion, String texto, List<Comercio> comercios,
                            OffsetDateTime ahora) {
        if (ConversacionCliente.esCambioDeComercio(texto)) {
            conversacion.reiniciar(null, ahora);
            return TextosPedido.PREGUNTAR_COMERCIO;
        }
        Optional<Comercio> comercio = comercioDe(comercios, conversacion.getNegocioId());
        if (comercio.isEmpty()) {
            conversacion.reiniciar(null, ahora);
            return TextosPedido.COMERCIO_NO_DISPONIBLE;
        }
        return armarLista(conversacion, comercio.get(), texto, ahora);
    }

    /**
     * Pasa lo que escribió el cliente a una lista con productos reales del comercio: la IA entiende qué pidió y el stock
     * decide qué hay. Nada de la lista sale de otro lado que del inventario del comercio.
     */
    private String armarLista(ConversacionCliente conversacion, Comercio comercio, String texto, OffsetDateTime ahora) {
        List<ProductoEnStock> catalogo = catalogoDe(comercio.id());
        if (catalogo.isEmpty()) {
            conversacion.tocar(ahora);
            return TextosPedido.sinProductos(comercio.nombre());
        }
        PedidoInterpretado pedido = interpretar(comercio.id(), texto, catalogo, ahora);
        if (pedido.lineas().isEmpty()) {
            conversacion.tocar(ahora);
            return TextosPedido.NO_ENTENDI_PEDIDO;
        }
        ArmadoDeLista armado = BuscadorDeProductos.armar(pedido.lineas(), catalogo);
        if (armado.lineas().isEmpty()) {
            conversacion.tocar(ahora);
            return TextosPedido.sinCoincidencias(armado.faltantes());
        }
        conversacion.armarLista(armado.lineas(), ahora);
        return TextosPedido.lista(comercio.nombre(), armado);
    }

    /** Los productos con precio y stock de los inventarios del comercio. */
    private List<ProductoEnStock> catalogoDe(NegocioId negocioId) {
        List<BloqueInventario> inventarios = proyectoRepository.findAllByNegocioId(negocioId).stream()
                .flatMap(proyecto -> datosDeProyectoService.cargar(proyecto, negocioId).inventarios().stream())
                .toList();
        return CatalogoDeProductos.de(inventarios);
    }

    /** Pedirle a la IA cuenta como una consulta de Sami del comercio elegido: mismo plan y mismo límite diario. */
    private PedidoInterpretado interpretar(NegocioId negocioId, String texto, List<ProductoEnStock> catalogo,
                                           OffsetDateTime ahora) {
        controlPlanService.exigirIa(negocioId);
        LocalDate dia = ZonaNegocio.dia(ahora);
        if (!consumoSamiRepository.reservar(negocioId, dia, limiteDiarioSami, ahora)) {
            throw new BusinessRuleException(TextosWhatsapp.LIMITE_SAMI);
        }
        PedidoInterpretado pedido = interpreteIa.interpretar(
                new ConsultaPedido(texto, catalogo.stream().map(ProductoEnStock::nombre).toList()));
        if (pedido.consumo() != null) {
            consumoSamiRepository.registrar(negocioId, dia, pedido.consumo(), ahora);
        }
        return pedido;
    }

    private static Optional<Comercio> comercioDe(List<Comercio> comercios, NegocioId negocioId) {
        return comercios.stream().filter(comercio -> comercio.id().equals(negocioId)).findFirst();
    }
}
