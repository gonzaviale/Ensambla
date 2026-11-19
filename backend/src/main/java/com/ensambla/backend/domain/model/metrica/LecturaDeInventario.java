package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Lee las tablas de inventario: cada producto con su nombre, lo que hay y lo que vale al costo. */
final class LecturaDeInventario {

    private LecturaDeInventario() {
    }

    static List<LineaMetrica> lineas(DatosDeProyecto datos) {
        return productos(datos).map(LecturaDeInventario::linea).toList();
    }

    /** El nombre de cada producto por su id, para que una venta diga qué se vendió. */
    static Map<RegistroId, String> nombresDeProductos(DatosDeProyecto datos) {
        Map<RegistroId, String> nombres = new LinkedHashMap<>();
        productos(datos).forEach(producto -> nombres.put(producto.registro().getId(), producto.nombre()));
        return nombres;
    }

    private static Stream<Producto> productos(DatosDeProyecto datos) {
        return datos.inventarios().stream()
                .flatMap(bloque -> bloque.registros().stream().map(registro -> new Producto(bloque, registro)));
    }

    private static LineaMetrica linea(Producto producto) {
        BigDecimal cantidad = producto.dato("quantity").comoNumero();
        return new LineaMetrica(producto.nombre(), LineaMetrica.SIN_CLIENTE, null, null,
                valorAlCosto(cantidad, producto.dato("cost").comoNumero()), cantidad, false);
    }

    private static BigDecimal valorAlCosto(BigDecimal cantidad, BigDecimal costo) {
        return cantidad == null || costo == null ? null : cantidad.multiply(costo);
    }

    private record Producto(BloqueInventario bloque, Registro registro) {

        ValorDeRegistro dato(String clave) {
            return ValorDeRegistro.de(bloque.valorDe(registro, clave));
        }

        String nombre() {
            return dato("name").comoTextoO(LineaMetrica.SIN_NOMBRE);
        }
    }
}
