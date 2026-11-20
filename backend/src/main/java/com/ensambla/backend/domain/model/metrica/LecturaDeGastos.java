package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.util.List;
import java.util.stream.Stream;

/** Lee las tablas de gastos que tienen importe: cada gasto con su categoría, su fecha y su total. */
final class LecturaDeGastos {

    private LecturaDeGastos() {
    }

    static boolean hayGastosConImporte(DatosDeProyecto datos) {
        return datos.gastos().stream().anyMatch(BloqueGastos::tieneImporte);
    }

    static List<LineaMetrica> lineas(DatosDeProyecto datos) {
        return datos.gastos().stream()
                .filter(BloqueGastos::tieneImporte)
                .flatMap(LecturaDeGastos::lineasDelBloque)
                .toList();
    }

    private static Stream<LineaMetrica> lineasDelBloque(BloqueGastos bloque) {
        return bloque.registros().stream().map(registro -> linea(bloque, registro));
    }

    private static LineaMetrica linea(BloqueGastos bloque, Registro registro) {
        return new LineaMetrica(
                LineaMetrica.SIN_NOMBRE,
                LineaMetrica.SIN_CLIENTE,
                dato(bloque, registro, "category").comoTextoO(LineaMetrica.SIN_CATEGORIA),
                dato(bloque, registro, "date").comoFecha(),
                dato(bloque, registro, "total").comoNumero(),
                null,
                false);
    }

    private static ValorDeRegistro dato(BloqueGastos bloque, Registro registro, String clave) {
        return ValorDeRegistro.de(bloque.valorDe(registro, clave));
    }
}
