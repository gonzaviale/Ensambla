package com.ensambla.backend.domain.useCase.inventario.registrarMovimiento;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        MovimientoStock nuevo = MovimientoStock.nuevo(
                negocioId,
                request.registroId(),
                request.tipo(),
                request.cantidad(),
                request.fecha(),
                request.nota(),
                LocalDate.now(clock),
                OffsetDateTime.now(clock));
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        Registro producto = registroRepository.obtener(request.registroId(), negocioId);
        producto.exigirDelBloque(bloque.getId());
        MovimientoStock guardado = transaccionPort.ejecutar(() -> registrar(nuevo));
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        BigDecimal stock = movimientoStockRepository.stockCompletoDe(List.of(producto.getId()), negocioId)
                .get(producto.getId());
        return RegistrarMovimientoResponse.de(guardado, Referencia.de(producto, campos).etiqueta(), stock);
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
        BigDecimal stock = movimientoStockRepository
                .stockCompletoDe(List.of(salida.getRegistroId()), salida.getNegocioId())
                .get(salida.getRegistroId());
        if (salida.getCantidad().compareTo(stock) > 0) {
            throw new RegistroInvalidoException(Map.of(
                    "cantidad", "No hay stock suficiente: quedan " + stock.stripTrailingZeros().toPlainString()));
        }
    }
}
