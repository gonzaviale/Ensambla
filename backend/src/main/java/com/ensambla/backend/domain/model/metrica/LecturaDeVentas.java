package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Lee la tabla de ventas: cada venta con el nombre de su producto, el de su cliente y si quedó fiada. */
final class LecturaDeVentas {

    private final Map<RegistroId, String> nombresDeProductos;

    private final Map<String, String> nombresDeClientes;

    private LecturaDeVentas(DatosDeProyecto datos) {
        this.nombresDeProductos = LecturaDeInventario.nombresDeProductos(datos);
        this.nombresDeClientes = datos.nombresDeClientes();
    }

    static List<LineaMetrica> lineas(DatosDeProyecto datos) {
        LecturaDeVentas lectura = new LecturaDeVentas(datos);
        return datos.ventas().stream().flatMap(lectura::lineasDelBloque).toList();
    }

    private Stream<LineaMetrica> lineasDelBloque(BloqueVentas bloque) {
        return bloque.registros().stream().flatMap(registro -> lineaDeLaVenta(bloque, registro));
    }

    private Stream<LineaMetrica> lineaDeLaVenta(BloqueVentas bloque, Registro registro) {
        boolean fiada = Boolean.TRUE.equals(bloque.valorDe(registro, "credit"));
        return VentaProducto.desde(bloque, registro).map(venta -> linea(venta, fiada)).stream();
    }

    private LineaMetrica linea(VentaProducto venta, boolean fiada) {
        return new LineaMetrica(nombreDelProducto(venta), nombreDelCliente(venta), null, venta.fecha(),
                importe(venta), venta.cantidad(), fiada);
    }

    private String nombreDelProducto(VentaProducto venta) {
        return nombresDeProductos.getOrDefault(venta.productoId(), LineaMetrica.SIN_NOMBRE);
    }

    private String nombreDelCliente(VentaProducto venta) {
        String cliente = venta.cliente();
        return cliente == null ? LineaMetrica.SIN_CLIENTE : nombresDeClientes.getOrDefault(cliente, cliente);
    }

    private static BigDecimal importe(VentaProducto venta) {
        return venta.precio() == null ? null : venta.precio().multiply(venta.cantidad());
    }
}
