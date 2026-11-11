package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * La conversación de un cliente con Sami: primero elige a qué comercio pedirle (por su nombre; si hay varios parecidos,
 * con un número) y después le pide. Hay una por cliente; si pasa un rato sin hablar, empieza de nuevo.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionCliente {

    /** Cuánto se espera una respuesta antes de volver a saludar y preguntar el comercio. */
    public static final Duration VIGENCIA = Duration.ofMinutes(30);

    private static final Set<String> CAMBIOS_DE_COMERCIO = Set.of("cambiar", "cambiar comercio", "cambiar de comercio",
            "otro comercio", "otro");

    private ConversacionClienteId id;

    private ClienteId clienteId;

    @Builder.Default
    private EstadoConversacionCliente estado = EstadoConversacionCliente.ELIGIENDO_COMERCIO;

    /** Eligiendo: el comercio que se le ofrece seguir; pidiendo: el comercio elegido. */
    private NegocioId negocioId;

    /** Los comercios entre los que tiene que elegir con un número, en el orden en que se le mostraron. */
    @Builder.Default
    private List<NegocioId> opciones = List.of();

    /** La lista de compra armada con el stock del comercio, todavía sin confirmar. */
    @Builder.Default
    private List<LineaDeCompra> lista = List.of();

    /** El renglón de la lista (desde 1) para el que se le ofrecieron otras marcas y espera su elección; null si ninguno. */
    private Integer renglonACambiar;

    private OffsetDateTime actualizadoEn;

    public static ConversacionCliente nueva(ClienteId clienteId, OffsetDateTime ahora) {
        return ConversacionCliente.builder().clienteId(clienteId).actualizadoEn(ahora).build();
    }

    /** Si el cliente escribió «cambiar» u «otro comercio»: quiere elegir un comercio distinto. */
    public static boolean esCambioDeComercio(String texto) {
        return CAMBIOS_DE_COMERCIO.contains(TextoNormalizado.de(texto).strip());
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return ahora.isBefore(actualizadoEn.plus(VIGENCIA));
    }

    /** Vuelve a preguntar el comercio, y le ofrece seguir con {@code ofrecido} si lo hay (el último al que le pidió). */
    public void reiniciar(NegocioId ofrecido, OffsetDateTime ahora) {
        estado = EstadoConversacionCliente.ELIGIENDO_COMERCIO;
        negocioId = ofrecido;
        opciones = List.of();
        lista = List.of();
        renglonACambiar = null;
        actualizadoEn = ahora;
    }

    /** Hay varios comercios parecidos: se le muestran numerados y tiene que elegir uno. */
    public void ofrecerOpciones(List<NegocioId> comercios, OffsetDateTime ahora) {
        estado = EstadoConversacionCliente.ELIGIENDO_COMERCIO;
        negocioId = null;
        opciones = List.copyOf(comercios);
        lista = List.of();
        renglonACambiar = null;
        actualizadoEn = ahora;
    }

    public void elegir(NegocioId comercio, OffsetDateTime ahora) {
        estado = EstadoConversacionCliente.PIDIENDO;
        negocioId = comercio;
        opciones = List.of();
        lista = List.of();
        renglonACambiar = null;
        actualizadoEn = ahora;
    }

    /** Sami armó la lista con lo que pidió el cliente; reemplaza a la anterior. */
    public void armarLista(List<LineaDeCompra> renglones, OffsetDateTime ahora) {
        lista = List.copyOf(renglones);
        renglonACambiar = null;
        actualizadoEn = ahora;
    }

    /** El cliente descartó la lista o ya la confirmó: no queda nada armado. */
    public void vaciarLista(OffsetDateTime ahora) {
        armarLista(List.of(), ahora);
    }

    public boolean tieneLista() {
        return !lista.isEmpty();
    }

    /** Se le ofrecen otras marcas del renglón (desde 1) y se espera que elija una. */
    public void ofrecerCambioDeMarca(int renglon, OffsetDateTime ahora) {
        renglonACambiar = renglon;
        actualizadoEn = ahora;
    }

    /** Deja de esperar la elección de otra marca y la lista queda como estaba. */
    public void dejarMarca(OffsetDateTime ahora) {
        renglonACambiar = null;
        actualizadoEn = ahora;
    }

    public boolean esperaCambioDeMarca() {
        return renglonACambiar != null;
    }

    /** El renglón que se está cambiando. */
    public LineaDeCompra renglonEnCambio() {
        return lista.get(renglonACambiar - 1);
    }

    /** Cambia el renglón que se estaba cambiando por la otra marca elegida. */
    public void cambiarMarca(ProductoEnStock nueva, OffsetDateTime ahora) {
        List<LineaDeCompra> cambiada = new ArrayList<>(lista);
        cambiada.set(renglonACambiar - 1, renglonEnCambio().cambiarPor(nueva));
        armarLista(cambiada, ahora);
    }

    /** El cliente siguió hablando sin cambiar de paso: la conversación sigue vigente. */
    public void tocar(OffsetDateTime ahora) {
        actualizadoEn = ahora;
    }

    /** Se le ofrece seguir con el último comercio y todavía no respondió. */
    public boolean ofreceElUltimo() {
        return estado == EstadoConversacionCliente.ELIGIENDO_COMERCIO && negocioId != null;
    }

    public boolean ofreceOpciones() {
        return !opciones.isEmpty();
    }

    /** El comercio que corresponde al número que escribió (empieza en 1); vacío si no hay una opción con ese número. */
    public Optional<NegocioId> opcion(int numero) {
        return numero >= 1 && numero <= opciones.size() ? Optional.of(opciones.get(numero - 1)) : Optional.empty();
    }
}
