package com.ensambla.backend.domain.model.metrica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/** Qué se calcula con las filas de la tabla. Siempre lo hace el servidor; cada cálculo sabe qué dato necesita. */
public enum CalculoMetrica {

    /** Suma del importe: ventas = precio × cantidad, gastos = total, inventario = cantidad × costo. */
    SUMA(FormatoMetrica.MONEDA) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return linea.tieneImporte();
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return sumaDe(lineas, LineaMetrica::importe);
        }
    },

    /** Importe promedio por fila. */
    PROMEDIO(FormatoMetrica.MONEDA) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return linea.tieneImporte();
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return sumaDe(lineas, LineaMetrica::importe)
                    .divide(BigDecimal.valueOf(lineas.size()), 2, RoundingMode.HALF_UP);
        }
    },

    /** Cuántas filas hay. */
    CONTEO(FormatoMetrica.NUMERO) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return true;
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return BigDecimal.valueOf(lineas.size());
        }
    },

    /** Suma de las unidades (cantidad); los gastos no tienen. */
    UNIDADES(FormatoMetrica.NUMERO) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return linea.tieneUnidades();
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return sumaDe(lineas, LineaMetrica::unidades);
        }

        @Override
        public boolean sirvePara(FuenteMetrica fuente) {
            return fuente.tieneUnidades();
        }

        @Override
        String datoQueFalta(FuenteMetrica fuente) {
            return fuente.faltanLasUnidades();
        }

        @Override
        String motivoSiNoSirvePara(FuenteMetrica fuente) {
            return "Los gastos no tienen unidades: elegí sumar, promediar o contar";
        }
    },

    /** Lo vendido menos lo que costó, a costo actual: sólo cuentan las ventas con precio y con costo del producto. */
    GANANCIA(FormatoMetrica.MONEDA) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return linea.tieneImporte() && linea.tieneCosto();
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return sumaDe(lineas, linea -> linea.importe().subtract(linea.costo()));
        }

        @Override
        public boolean sirvePara(FuenteMetrica fuente) {
            return fuente == FuenteMetrica.VENTAS;
        }

        @Override
        String datoQueFalta(FuenteMetrica fuente) {
            return "Cargá el precio de tus ventas y el costo de tus productos para poder calcular la ganancia.";
        }

        @Override
        String motivoSiNoSirvePara(FuenteMetrica fuente) {
            return "La ganancia se calcula con tus ventas: elegí ventas";
        }
    },

    /** Lo que costó lo que se vendió, a costo actual: sólo cuentan las ventas de productos con costo cargado. */
    COSTO(FormatoMetrica.MONEDA) {
        @Override
        boolean aporta(LineaMetrica linea) {
            return linea.tieneCosto();
        }

        @Override
        BigDecimal calcular(List<LineaMetrica> lineas) {
            return sumaDe(lineas, LineaMetrica::costo);
        }

        @Override
        public boolean sirvePara(FuenteMetrica fuente) {
            return fuente == FuenteMetrica.VENTAS;
        }

        @Override
        String datoQueFalta(FuenteMetrica fuente) {
            return "Cargá el costo de tus productos para poder calcular lo que te costó lo que vendiste.";
        }

        @Override
        String motivoSiNoSirvePara(FuenteMetrica fuente) {
            return "El costo de lo vendido se calcula con tus ventas: elegí ventas";
        }
    };

    private final FormatoMetrica formato;

    CalculoMetrica(FormatoMetrica formato) {
        this.formato = formato;
    }

    /** Si la fila tiene el dato que este cálculo necesita. */
    abstract boolean aporta(LineaMetrica linea);

    abstract BigDecimal calcular(List<LineaMetrica> lineas);

    public FormatoMetrica formato() {
        return formato;
    }

    /** {@code true} si necesita el importe de cada fila (precio, total o costo). */
    public boolean usaImporte() {
        return formato == FormatoMetrica.MONEDA;
    }

    public boolean sirvePara(FuenteMetrica fuente) {
        return true;
    }

    /** Por qué esta cuenta no se puede hacer con la tabla elegida, en palabras de la persona. */
    String motivoSiNoSirvePara(FuenteMetrica fuente) {
        return "Esa cuenta no se puede hacer con " + fuente.getFilas();
    }

    /** Qué tiene que cargar la persona cuando ninguna fila trae el dato que hace falta. */
    String datoQueFalta(FuenteMetrica fuente) {
        return fuente.faltaElImporte();
    }

    static BigDecimal sumaDe(List<LineaMetrica> lineas, Function<LineaMetrica, BigDecimal> dato) {
        return lineas.stream().map(dato).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
