package com.ensambla.backend.domain.useCase.inventario.registrarMovimiento;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class RegistrarMovimiento {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final MovimientoStockRepository movimientoStockRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public RegistrarMovimientoResponse execute(RegistrarMovimientoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        MovimientoStock nuevo = movimientoDe(request, negocioId);
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        Registro producto = obtenerProducto(request.registroId(), negocioId);
        producto.exigirDelBloque(bloque.getId());
        MovimientoStock guardado = registrarEnTransaccion(nuevo);
        return respuestaDe(guardado, producto, bloque, negocioId);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private MovimientoStock movimientoDe(RegistrarMovimientoRequest request, NegocioId negocioId) {
        return MovimientoStock.nuevo(
                negocioId,
                request.registroId(),
                request.tipo(),
                request.cantidad(),
                request.fecha(),
                request.nota(),
                LocalDate.now(clock),
                OffsetDateTime.now(clock));
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private Registro obtenerProducto(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private MovimientoStock registrarEnTransaccion(MovimientoStock movimiento) {
        return transaccionPort.ejecutar(() -> registrar(movimiento));
    }

    private RegistrarMovimientoResponse respuestaDe(MovimientoStock guardado, Registro producto, Bloque bloque,
                                                    NegocioId negocioId) {
        List<Campo> campos = obtenerCampos(bloque, negocioId);
        BigDecimal stock = stockDe(producto.getId(), negocioId);
        return RegistrarMovimientoResponse.de(guardado, Referencia.de(producto, campos).etiqueta(), stock);
    }

    private List<Campo> obtenerCampos(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private BigDecimal stockDe(RegistroId productoId, NegocioId negocioId) {
        return movimientoStockRepository.stockCompletoDe(List.of(productoId), negocioId).get(productoId);
    }

    /**
     * Guarda el movimiento. Una salida no puede dejar el stock en negativo.
     *
     * @throws RegistroInvalidoException si la salida es mayor que el stock actual del producto
     */
    private MovimientoStock registrar(MovimientoStock movimiento) {
        if (movimiento.esSalida()) {
            validarStockSuficiente(movimiento);
        }
        return movimientoStockRepository.save(movimiento);
    }

    private void validarStockSuficiente(MovimientoStock salida) {
        BigDecimal stock = stockDe(salida.getRegistroId(), salida.getNegocioId());
        if (salida.getCantidad().compareTo(stock) > 0) {
            throw new RegistroInvalidoException(Map.of(
                    "cantidad", "No hay stock suficiente: quedan " + stock.stripTrailingZeros().toPlainString()));
        }
    }
}
