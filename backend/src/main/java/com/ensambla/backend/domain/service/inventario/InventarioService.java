package com.ensambla.backend.domain.service.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    static final int MAXIMO_MOVIMIENTOS = 100;

    private final MovimientoStockRepository movimientoStockRepository;

    /**
     * Guarda el movimiento. Una salida no puede dejar el stock en negativo.
     *
     * @throws RegistroInvalidoException si la salida es mayor que el stock actual del producto
     */
    @Transactional
    public MovimientoStock registrar(MovimientoStock movimiento) {
        if (movimiento.esSalida()) {
            validarStockSuficiente(movimiento);
        }
        return movimientoStockRepository.save(movimiento);
    }

    /** Stock de cada producto pedido, en el orden pedido; sin movimientos es cero. */
    public Map<UUID, BigDecimal> stockDe(Collection<UUID> registroIds, UUID negocioId) {
        Map<UUID, BigDecimal> existentes = movimientoStockRepository.stockDe(registroIds, negocioId);
        Map<UUID, BigDecimal> stock = new LinkedHashMap<>();
        registroIds.forEach(id -> stock.put(id, existentes.getOrDefault(id, BigDecimal.ZERO)));
        return stock;
    }

    /** Los últimos {@value #MAXIMO_MOVIMIENTOS} movimientos de los productos de un bloque, los más nuevos primero. */
    public List<MovimientoStock> listarRecientes(UUID bloqueId, UUID negocioId, UUID registroId) {
        return movimientoStockRepository.findRecientes(bloqueId, negocioId, registroId, MAXIMO_MOVIMIENTOS);
    }

    private void validarStockSuficiente(MovimientoStock salida) {
        BigDecimal stock = stockDe(List.of(salida.getRegistroId()), salida.getNegocioId()).get(salida.getRegistroId());
        if (salida.getCantidad().compareTo(stock) > 0) {
            throw new RegistroInvalidoException(Map.of(
                    "cantidad", "No hay stock suficiente: quedan " + stock.stripTrailingZeros().toPlainString()));
        }
    }
}
