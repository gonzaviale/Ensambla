package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.numero;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.texto;

/** Productos agotados o por debajo del mínimo que definió el negocio. */
public class ReglaStockBajo implements ReglaAviso {

    static final String CLAVE_CANTIDAD = "quantity";
    static final String CLAVE_MINIMO = "minimum";

    private static final short PRIORIDAD_AGOTADO = 3;
    private static final short PRIORIDAD_BAJO_MINIMO = 2;
    private static final BigDecimal PEDIDO_SIN_MINIMO = BigDecimal.TEN;
    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    @Override
    public TipoAviso tipo() {
        return TipoAviso.STOCK;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        return contexto.inventarios().stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> evaluarRegistro(inventario, registro))
                        .filter(Objects::nonNull))
                .toList();
    }

    private ResultadoRegla evaluarRegistro(BloqueInventario inventario, Registro registro) {
        Stock stock = Stock.de(inventario, registro);
        if (stock == null || !stock.hayQueReponer()) {
            return null;
        }
        return avisoDeReposicion(inventario, registro, stock);
    }

    private static ResultadoRegla avisoDeReposicion(BloqueInventario inventario, Registro registro, Stock stock) {
        String nombre = nombreDe(inventario, registro);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                stock.prioridad(),
                stock.titulo(nombre),
                stock.detalle(),
                stock.datos(),
                List.of(stock.motivo(), stock.sugerencia()),
                List.of(accion(inventario, registro.getId())));
    }

    /** Registrar la entrada si el proyecto tiene tabla de entradas; si no, abrir el producto. */
    private static AccionDeAviso accion(BloqueInventario inventario, RegistroId registroId) {
        if (inventario.entradasId() != null) {
            return AccionesAviso.registrarEntrada(inventario);
        }
        return AccionesAviso.abrir(inventario, registroId);
    }

    /** Lo que queda de un producto y el mínimo que definió el negocio; {@code minimo} es {@code null} si no lo definió. */
    private record Stock(BigDecimal cantidad, BigDecimal minimo) {

        static Stock de(BloqueInventario inventario, Registro registro) {
            BigDecimal cantidad = numero(inventario.valorDe(registro, CLAVE_CANTIDAD));
            if (cantidad == null) {
                return null;
            }
            return new Stock(cantidad, minimoDefinido(numero(inventario.valorDe(registro, CLAVE_MINIMO))));
        }

        private static BigDecimal minimoDefinido(BigDecimal minimo) {
            return minimo != null && minimo.signum() > 0 ? minimo : null;
        }

        boolean hayQueReponer() {
            return estaAgotado() || estaEnElMinimoOPorDebajo();
        }

        short prioridad() {
            return estaAgotado() ? PRIORIDAD_AGOTADO : PRIORIDAD_BAJO_MINIMO;
        }

        String titulo(String nombre) {
            return estaAgotado() ? "Te quedaste sin " + nombre : nombre + " está por debajo del mínimo";
        }

        String detalle() {
            return situacion() + conMinimo() + ". " + sugerencia();
        }

        String motivo() {
            return tieneMinimo()
                    ? "Definiste un mínimo de " + texto(minimo) + " y quedan " + texto(cantidad) + "."
                    : "No tiene un mínimo definido: te avisamos porque se agotó.";
        }

        String sugerencia() {
            return "Te conviene pedir " + texto(cantidadAPedir()) + " para no quedarte sin stock.";
        }

        Map<String, Object> datos() {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("cantidad", cantidad);
            datos.put("minimo", tieneMinimo() ? minimo : BigDecimal.ZERO);
            return datos;
        }

        private String situacion() {
            return estaAgotado() ? "No te quedan unidades" : "Te quedan " + texto(cantidad);
        }

        private String conMinimo() {
            return tieneMinimo() ? " y tu mínimo es " + texto(minimo) : "";
        }

        private BigDecimal cantidadAPedir() {
            BigDecimal objetivo = tieneMinimo() ? minimo.multiply(DOS) : PEDIDO_SIN_MINIMO;
            return objetivo.subtract(cantidad).max(BigDecimal.ONE);
        }

        private boolean estaAgotado() {
            return cantidad.signum() <= 0;
        }

        private boolean estaEnElMinimoOPorDebajo() {
            return tieneMinimo() && cantidad.compareTo(minimo) <= 0;
        }

        private boolean tieneMinimo() {
            return minimo != null;
        }
    }
}
