package com.ensambla.backend.domain.model.bloque;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Los tipos de sección. Cada tipo sabe cómo se llama al crearlo, de qué color nace, si todo negocio necesita al menos
 * una sección de ese tipo y qué columnas fijas lleva (las que el resto de Ensambla lee por su clave).
 */
public enum TipoBloque {
    INVENTARIO("Stock", ColorBloque.NARANJA, true, null, ColumnasFijas::deStock),
    PEDIDOS("Pedidos", ColorBloque.CORAL, false, null, ColumnasFijas::ninguna),
    CLIENTES("Clientes", ColorBloque.CELESTE, true, null, ColumnasFijas::deClientes),
    VENTAS("Ventas", ColorBloque.AMARILLO, true, (short) -1, ColumnasFijas::deVentas),
    GASTOS("Gastos", ColorBloque.AMARILLO, true, null, ColumnasFijas::deGastos),
    RESUMEN("Resumen", ColorBloque.PETROLEO, false, null, ColumnasFijas::ninguna),
    FORMULARIO("Formulario", ColorBloque.CELESTE, false, null, ColumnasFijas::ninguna),
    NOTA("Nota", ColorBloque.AMARILLO, false, null, ColumnasFijas::ninguna),
    ENTRADAS("Entradas", ColorBloque.CELESTE, false, (short) 1, ColumnasFijas::deMovimientos),
    SALIDAS("Salidas", ColorBloque.CORAL, false, (short) -1, ColumnasFijas::deMovimientos);

    private final String nombreInicial;

    private final ColorBloque colorInicial;

    private final boolean obligatorioEnElNegocio;

    private final Short direccionDeStock;

    private final Supplier<List<ColumnaFija>> columnasFijas;

    TipoBloque(String nombreInicial, ColorBloque colorInicial, boolean obligatorioEnElNegocio,
               Short direccionDeStock, Supplier<List<ColumnaFija>> columnasFijas) {
        this.nombreInicial = nombreInicial;
        this.colorInicial = colorInicial;
        this.obligatorioEnElNegocio = obligatorioEnElNegocio;
        this.direccionDeStock = direccionDeStock;
        this.columnasFijas = columnasFijas;
    }

    /** Los tipos que todo negocio necesita tener al menos una vez. */
    public static List<TipoBloque> obligatorios() {
        return Arrays.stream(values()).filter(TipoBloque::esObligatorioEnElNegocio).toList();
    }

    public String nombreInicial() {
        return nombreInicial;
    }

    public ColorBloque colorInicial() {
        return colorInicial;
    }

    public boolean esObligatorioEnElNegocio() {
        return obligatorioEnElNegocio;
    }

    /** +1 si la sección suma stock, -1 si lo descuenta; {@code null} si no lo mueve. */
    public Short direccionDeStock() {
        return direccionDeStock;
    }

    public List<ColumnaFija> columnasFijas() {
        return columnasFijas.get();
    }

    public boolean tieneColumnasFijas() {
        return !columnasFijas().isEmpty();
    }

    public String mensajeAlQuitarLaUltima() {
        return "No se puede quitar la sección «" + nombreInicial + "»: Ensambla la necesita para funcionar.";
    }

    public String mensajeAlCambiarElTipo() {
        return "No se puede cambiar el tipo de una sección «" + nombreInicial
                + "»: Ensambla la necesita para funcionar. Podés cambiarle el nombre.";
    }
}
