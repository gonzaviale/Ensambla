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
        actualizadoEn = ahora;
    }

    /** Hay varios comercios parecidos: se le muestran numerados y tiene que elegir uno. */
    public void ofrecerOpciones(List<NegocioId> comercios, OffsetDateTime ahora) {
        estado = EstadoConversacionCliente.ELIGIENDO_COMERCIO;
        negocioId = null;
        opciones = List.copyOf(comercios);
        actualizadoEn = ahora;
    }

    public void elegir(NegocioId comercio, OffsetDateTime ahora) {
        estado = EstadoConversacionCliente.PIDIENDO;
        negocioId = comercio;
        opciones = List.of();
        actualizadoEn = ahora;
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
