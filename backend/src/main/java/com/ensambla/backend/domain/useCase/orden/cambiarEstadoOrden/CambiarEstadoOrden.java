package com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.pedido.TextosPedido;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.ItemDeOrden;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El dueño lleva una orden de su negocio a la etapa siguiente: aceptarla, rechazarla, ponerla en preparación o dejarla
 * lista para retirar. Al aceptarla se descuenta del inventario lo que pidió el cliente; si el stock ya no alcanza no
 * se acepta y no se toca nada. Cuando la acepta, la rechaza o la deja lista, Sami le avisa al cliente por WhatsApp.
 */
@Slf4j
@RequiredArgsConstructor
public class CambiarEstadoOrden {

    private final NegocioRepository negocioRepository;
    private final OrdenCompraRepository ordenRepository;
    private final ClienteWhatsappRepository clienteRepository;
    private final RegistroRepository registroRepository;
    private final CampoRepository campoRepository;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public OrdenItem execute(CambiarEstadoOrdenRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        OrdenCompra orden = transaccionPort.ejecutar(() -> cambiar(negocio.getId(), request, ahora));
        String telefono = telefonoDelCliente(orden);
        return OrdenItem.de(orden, telefono, avisarAlCliente(orden, negocio.getNombre(), telefono));
    }

    /**
     * Ya con la orden guardada, Sami le avisa al cliente por WhatsApp. Si no se pudo entregar la orden igual quedó
     * como la dejó el dueño; el aviso que falló se informa para que él lo avise por su cuenta.
     *
     * @return si el aviso salió; null si este paso no se avisa
     */
    private Boolean avisarAlCliente(OrdenCompra orden, String comercio, String telefono) {
        Optional<String> aviso = TextosPedido.avisoAlCliente(orden, comercio);
        if (aviso.isEmpty()) {
            return null;
        }
        if (telefono == null) {
            return false;
        }
        try {
            return mensajeria.enviarTexto(telefono, aviso.get());
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar por WhatsApp al cliente de la orden {}", orden.getId(), e);
            return false;
        }
    }

    private OrdenCompra cambiar(NegocioId negocioId, CambiarEstadoOrdenRequest request, OffsetDateTime ahora) {
        OrdenCompra orden = ordenRepository.obtener(negocioId, request.ordenId());
        orden.pasarA(request.estado(), request.motivo(), ahora);
        if (request.estado() == EstadoOrdenCompra.ACEPTADA) {
            descontarStock(orden, negocioId, ahora);
        }
        return ordenRepository.save(orden);
    }

    // --- descontar el stock al aceptar ---

    private void descontarStock(OrdenCompra orden, NegocioId negocioId, OffsetDateTime ahora) {
        List<Registro> productos = new ArrayList<>();
        for (ItemDeOrden item : orden.getItems()) {
            Registro producto = registroRepository.findByIdAndNegocioId(item.productoId(), negocioId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "%s ya no está en tu inventario".formatted(item.nombre())));
            descontar(producto, item, negocioId, ahora);
            productos.add(producto);
        }
        registroRepository.saveAll(productos);
    }

    private void descontar(Registro producto, ItemDeOrden item, NegocioId negocioId, OffsetDateTime ahora) {
        Campo campoStock = campoRepository.findByBloqueIdAndNegocioId(producto.getBloqueId(), negocioId).stream()
                .filter(campo -> MovimientoDeStock.CLAVE_CANTIDAD.equals(campo.getClave()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "%s no tiene el stock cargado en tu inventario".formatted(item.nombre())));
        String clave = campoStock.getId().toString();
        BigDecimal habia = producto.getValores().get(clave) instanceof Number numero
                ? new BigDecimal(numero.toString()) : BigDecimal.ZERO;
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar(item.nombre(), habia, item.cantidad(), -1);
        if (movimiento.noAlcanzo()) {
            throw new BusinessRuleException("No alcanza el stock de %s: hay %s y la orden pide %s".formatted(
                    item.nombre(), habia.stripTrailingZeros().toPlainString(),
                    item.cantidad().stripTrailingZeros().toPlainString()));
        }
        Map<String, Object> valores = new LinkedHashMap<>(producto.getValores());
        valores.put(clave, movimiento.despues());
        producto.reemplazarValores(valores, ahora);
    }

    private String telefonoDelCliente(OrdenCompra orden) {
        return clienteRepository.findAllByIds(List.of(orden.getClienteId())).stream()
                .findFirst()
                .map(ClienteWhatsapp::getTelefono)
                .orElse(null);
    }
}
