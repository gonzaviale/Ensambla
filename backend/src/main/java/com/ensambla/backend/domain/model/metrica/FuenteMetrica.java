package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CATEGORIA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CLIENTE;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.DIA_DE_LA_SEMANA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.PRODUCTO;

/** De qué tabla del cuaderno sale una métrica. Cada tabla sabe si está, cómo se lee y cómo se puede repartir. */
public enum FuenteMetrica {

    VENTAS("ventas", "precio", Set.of(NINGUNA, PRODUCTO, CLIENTE, DIA_DE_LA_SEMANA)) {
        @Override
        boolean estaEn(DatosDeProyecto datos) {
            return !datos.ventas().isEmpty();
        }

        @Override
        List<LineaMetrica> lineasDe(DatosDeProyecto datos) {
            return LecturaDeVentas.lineas(datos);
        }

        @Override
        public boolean admiteFiado() {
            return true;
        }

        @Override
        public boolean tieneCostoDeLoVendido() {
            return true;
        }
    },

    GASTOS("gastos", "importe", Set.of(NINGUNA, CATEGORIA, DIA_DE_LA_SEMANA)) {
        @Override
        boolean estaEn(DatosDeProyecto datos) {
            return LecturaDeGastos.hayGastosConImporte(datos);
        }

        @Override
        List<LineaMetrica> lineasDe(DatosDeProyecto datos) {
            return LecturaDeGastos.lineas(datos);
        }

        @Override
        public boolean tieneUnidades() {
            return false;
        }
    },

    INVENTARIO("productos", "costo", Set.of(NINGUNA, PRODUCTO)) {
        @Override
        boolean estaEn(DatosDeProyecto datos) {
            return !datos.inventarios().isEmpty();
        }

        @Override
        List<LineaMetrica> lineasDe(DatosDeProyecto datos) {
            return LecturaDeInventario.lineas(datos);
        }

        @Override
        public boolean tieneFecha() {
            return false;
        }
    };

    private final String filas;

    private final String importe;

    private final Set<AgrupacionMetrica> repartos;

    FuenteMetrica(String filas, String importe, Set<AgrupacionMetrica> repartos) {
        this.filas = filas;
        this.importe = importe;
        this.repartos = repartos;
    }

    /** Si el cuaderno tiene esta tabla con algo con qué calcular. */
    abstract boolean estaEn(DatosDeProyecto datos);

    /** Las filas de esta tabla, ya leídas por su clave semántica. */
    abstract List<LineaMetrica> lineasDe(DatosDeProyecto datos);

    /** Las que el negocio ya cargó, en su orden: sólo de esas se puede armar una métrica. */
    public static List<FuenteMetrica> lasQueEstanEn(DatosDeProyecto datos) {
        return Arrays.stream(values()).filter(fuente -> fuente.estaEn(datos)).toList();
    }

    /** Cómo se nombra a lo que se cuenta: "no hay ventas en este período". */
    public String getFilas() {
        return filas;
    }

    public boolean tieneFecha() {
        return true;
    }

    public boolean tieneUnidades() {
        return true;
    }

    public boolean admiteFiado() {
        return false;
    }

    /** Sólo una venta dice qué producto salió, así que sólo de ellas sale la ganancia o el costo de lo vendido. */
    public boolean tieneCostoDeLoVendido() {
        return false;
    }

    /** Las maneras de repartir que tienen sentido para la tabla: un gasto no tiene producto ni una venta categoría. */
    public boolean permiteRepartirPor(AgrupacionMetrica agrupacion) {
        return repartos.contains(agrupacion);
    }

    String faltaElImporte() {
        return "Cargá el " + importe + " de tus " + filas + " para poder calcularlo.";
    }

    String faltanLasUnidades() {
        return "Cargá la cantidad de tus " + filas + " para poder calcularlo.";
    }
}
