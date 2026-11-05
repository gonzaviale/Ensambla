package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Optional;

/** Un producto que hay que reponer, con cuánto pedir y a qué proveedor (nulo si no tiene uno cargado). */
public record ItemReposicion(
        RegistroId productoId,
        String nombre,
        String proveedor,
        BigDecimal cantidadActual,
        BigDecimal cantidadAPedir,
        String situacion,
        int urgencia) {

    public static final String CLAVE_NOMBRE = "name";
    public static final String CLAVE_CANTIDAD = "quantity";
    public static final String CLAVE_MINIMO = "minimum";
    public static final String CLAVE_PROVEEDOR = "supplier";

    private static final String PRODUCTO_SIN_NOMBRE = "Producto sin nombre";

    /** El producto de la fila si hay que reponerlo; vacío si tiene stock suficiente o no tiene cantidad cargada. */
    public static Optional<ItemReposicion> de(BloqueInventario inventario, Registro registro) {
        BigDecimal cantidad = numero(inventario.valorDe(registro, CLAVE_CANTIDAD));
        BigDecimal minimo = numero(inventario.valorDe(registro, CLAVE_MINIMO));
        return Optional.ofNullable(cantidad)
                .flatMap(actual -> SituacionStock.detectar(actual, minimo))
                .map(situacion -> new ItemReposicion(
                        registro.getId(),
                        nombreDe(inventario.valorDe(registro, CLAVE_NOMBRE)),
                        proveedorDe(inventario.valorDe(registro, CLAVE_PROVEEDOR)),
                        cantidad,
                        situacion.cantidadAPedir(),
                        situacion.descripcion(),
                        situacion.urgencia()));
    }

    /** El mismo producto con la cantidad que la persona decidió pedir. */
    public ItemReposicion conCantidad(BigDecimal cantidadElegida) {
        exigirPositiva(cantidadElegida);
        return new ItemReposicion(productoId, nombre, proveedor, cantidadActual, cantidadElegida, situacion, urgencia);
    }

    private void exigirPositiva(BigDecimal cantidadElegida) {
        if (cantidadElegida.signum() <= 0) {
            throw new BusinessRuleException("La cantidad a pedir de «" + nombre + "» tiene que ser mayor a cero");
        }
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static String nombreDe(Object valor) {
        return textoDe(valor).orElse(PRODUCTO_SIN_NOMBRE);
    }

    private static String proveedorDe(Object valor) {
        return textoDe(valor).orElse(null);
    }

    private static Optional<String> textoDe(Object valor) {
        return Optional.ofNullable(valor)
                .filter(String.class::isInstance)
                .map(texto -> ((String) texto).strip())
                .filter(texto -> !texto.isEmpty());
    }
}
