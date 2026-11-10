package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;

import java.util.List;
import java.util.Map;

/**
 * Las tablas de inventario, de ventas, de gastos y de entradas de un proyecto, con sus registros reales (sin los de
 * ejemplo), y el nombre de cada fila de sus tablas de clientes (por id de la fila): una venta puede guardar el cliente
 * como una conexión a esa tabla y el resumen tiene que mostrar el nombre, nunca el id.
 */
public record DatosDeProyecto(
        String nombre,
        List<BloqueInventario> inventarios,
        List<BloqueVentas> ventas,
        List<BloqueGastos> gastos,
        Map<String, String> nombresDeClientes,
        List<BloqueEntradas> entradas) {

    /** Datos sin tablas de clientes: el cliente de las ventas se muestra como quedó escrito. */
    public DatosDeProyecto(String nombre, List<BloqueInventario> inventarios, List<BloqueVentas> ventas,
                           List<BloqueGastos> gastos) {
        this(nombre, inventarios, ventas, gastos, Map.of());
    }

    /** Datos sin tablas de entradas: no se conocen las compras de mercadería. */
    public DatosDeProyecto(String nombre, List<BloqueInventario> inventarios, List<BloqueVentas> ventas,
                           List<BloqueGastos> gastos, Map<String, String> nombresDeClientes) {
        this(nombre, inventarios, ventas, gastos, nombresDeClientes, List.of());
    }
}
