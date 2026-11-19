package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una tabla de movimientos (ventas, salidas, entradas) mueve el stock del producto de cada fila nueva, como en el
 * taller. Varias filas del mismo producto se acumulan.
 */
@RequiredArgsConstructor
final class StockDeLaPropuesta {

    private static final String PRODUCTO_SIN_NOMBRE = "el producto";

    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;

    List<MovimientoDeStock> mover(Bloque bloque, List<Campo> campos, List<Map<String, Object>> nuevas,
                                  NegocioId negocioId, OffsetDateTime ahora) {
        Optional<Campo> producto = campoConClave(campos, MovimientoDeStock.CLAVE_PRODUCTO).filter(Campo::esRelacion);
        Optional<Campo> cantidad = campoConClave(campos, MovimientoDeStock.CLAVE_CANTIDAD);
        if (bloque.getDireccionStock() == null || producto.isEmpty() || cantidad.isEmpty()) {
            return List.of();
        }
        MovimientosEnCurso enCurso = new MovimientosEnCurso(bloque.getDireccionStock(), negocioId, ahora);
        nuevas.forEach(fila -> moverElStockDe(fila, producto.get(), cantidad.get(), enCurso));
        guardarProductosMovidos(enCurso);
        return enCurso.movimientos;
    }

    /** Lo que se va moviendo mientras se recorren las filas: los productos tocados (una sola vez cada uno) y los movimientos. */
    private static final class MovimientosEnCurso {
        private final Map<String, Registro> productos = new LinkedHashMap<>();
        private final List<MovimientoDeStock> movimientos = new ArrayList<>();
        private final int direccion;
        private final NegocioId negocioId;
        private final OffsetDateTime ahora;

        private MovimientosEnCurso(int direccion, NegocioId negocioId, OffsetDateTime ahora) {
            this.direccion = direccion;
            this.negocioId = negocioId;
            this.ahora = ahora;
        }
    }

    private void moverElStockDe(Map<String, Object> fila, Campo campoProducto, Campo campoCantidad,
                                MovimientosEnCurso enCurso) {
        Object productoId = fila.get(campoProducto.getId().toString());
        Object pedido = fila.get(campoCantidad.getId().toString());
        if (productoId == null || pedido == null) {
            return;
        }
        productoVinculado(productoId.toString(), enCurso)
                .flatMap(producto -> mover(producto, new BigDecimal(pedido.toString()), enCurso))
                .ifPresent(enCurso.movimientos::add);
    }

    private Optional<Registro> productoVinculado(String productoId, MovimientosEnCurso enCurso) {
        return Optional.ofNullable(enCurso.productos.computeIfAbsent(productoId,
                id -> buscarProducto(id, enCurso.negocioId).orElse(null)));
    }

    private void guardarProductosMovidos(MovimientosEnCurso enCurso) {
        if (!enCurso.movimientos.isEmpty()) {
            registroRepository.saveAll(List.copyOf(enCurso.productos.values()));
        }
    }

    private Optional<MovimientoDeStock> mover(Registro producto, BigDecimal pedido, MovimientosEnCurso enCurso) {
        List<Campo> campos = camposDelProducto(producto, enCurso.negocioId);
        Optional<Campo> stock = campoConClave(campos, MovimientoDeStock.CLAVE_CANTIDAD);
        if (stock.isEmpty()) {
            return Optional.empty();
        }
        String clave = stock.get().getId().toString();
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar(nombreDe(producto, campos),
                FilaPropuesta.sumado(producto.getValores().get(clave), BigDecimal.ZERO), pedido, enCurso.direccion);
        actualizarStock(producto, clave, movimiento, enCurso.ahora);
        return Optional.of(movimiento);
    }

    private static void actualizarStock(Registro producto, String clave, MovimientoDeStock movimiento,
                                        OffsetDateTime ahora) {
        Map<String, Object> valores = new LinkedHashMap<>(producto.getValores());
        valores.put(clave, movimiento.despues());
        producto.reemplazarValores(valores, ahora);
    }

    private static String nombreDe(Registro producto, List<Campo> campos) {
        return campos.stream().filter(Campo::nombraLasFilas).findFirst()
                .map(campo -> producto.getValores().get(campo.getId().toString()))
                .map(Object::toString)
                .orElse(PRODUCTO_SIN_NOMBRE);
    }

    private static Optional<Campo> campoConClave(List<Campo> campos, String clave) {
        return campos.stream().filter(campo -> clave.equals(campo.getClave())).findFirst();
    }

    private Optional<Registro> buscarProducto(String productoId, NegocioId negocioId) {
        return registroRepository.findByIdAndNegocioId(RegistroId.de(productoId), negocioId);
    }

    private List<Campo> camposDelProducto(Registro producto, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(producto.getBloqueId(), negocioId);
    }
}
