package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.model.registro.Registro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Productos agotados o por debajo del mínimo que definió el negocio. */
@Component
public class ReglaStockBajo implements ReglaAviso {

    static final String CLAVE_NOMBRE = "name";
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
                        .filter(java.util.Objects::nonNull))
                .toList();
    }

    private ResultadoRegla evaluarRegistro(BloqueInventario inventario, Registro registro) {
        BigDecimal cantidad = numero(inventario.valorDe(registro, CLAVE_CANTIDAD));
        if (cantidad == null) {
            return null;
        }
        BigDecimal minimo = numero(inventario.valorDe(registro, CLAVE_MINIMO));
        boolean hayMinimo = minimo != null && minimo.signum() > 0;
        boolean agotado = cantidad.signum() <= 0;
        if (!agotado && !(hayMinimo && cantidad.compareTo(minimo) <= 0)) {
            return null;
        }
        String nombre = nombreDe(inventario, registro);
        BigDecimal pedir = cantidadAPedir(cantidad, hayMinimo ? minimo : null);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("cantidad", cantidad);
        datos.put("minimo", hayMinimo ? minimo : BigDecimal.ZERO);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                agotado ? PRIORIDAD_AGOTADO : PRIORIDAD_BAJO_MINIMO,
                agotado ? "Te quedaste sin " + nombre : nombre + " está por debajo del mínimo",
                detalle(agotado, cantidad, hayMinimo ? minimo : null, pedir),
                datos,
                List.of(motivo(cantidad, hayMinimo ? minimo : null), "Para no quedarte corto, pedí " + texto(pedir) + "."),
                List.of(accion(inventario, registro.getId())));
    }

    private static String detalle(boolean agotado, BigDecimal cantidad, BigDecimal minimo, BigDecimal pedir) {
        String situacion = agotado ? "No te quedan unidades" : "Te quedan " + texto(cantidad);
        String conMinimo = minimo == null ? "" : " y tu mínimo es " + texto(minimo);
        return situacion + conMinimo + ". Para no quedarte corto, pedí " + texto(pedir) + ".";
    }

    private static String motivo(BigDecimal cantidad, BigDecimal minimo) {
        return minimo == null
                ? "No tiene un mínimo definido: te avisamos porque se agotó."
                : "Definiste un mínimo de " + texto(minimo) + " y quedan " + texto(cantidad) + ".";
    }

    private static BigDecimal cantidadAPedir(BigDecimal cantidad, BigDecimal minimo) {
        BigDecimal objetivo = minimo == null ? PEDIDO_SIN_MINIMO : minimo.multiply(DOS);
        return objetivo.subtract(cantidad).max(BigDecimal.ONE);
    }

    /** Registrar la entrada si el proyecto tiene tabla de entradas; si no, abrir el producto. */
    private static Map<String, Object> accion(BloqueInventario inventario, UUID registroId) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        if (inventario.entradasId() != null) {
            accion.put("tipo", "REGISTRAR");
            accion.put("texto", "Registrar entrada");
            accion.put("bloqueId", inventario.entradasId().toString());
        } else {
            accion.put("tipo", "ABRIR");
            accion.put("texto", "Ver producto");
            accion.put("bloqueId", inventario.bloque().getId().toString());
            accion.put("registroId", registroId.toString());
        }
        return accion;
    }

    private static String nombreDe(BloqueInventario inventario, Registro registro) {
        Object nombre = inventario.valorDe(registro, CLAVE_NOMBRE);
        return nombre instanceof String texto && !texto.isBlank() ? texto.trim() : "Un producto sin nombre";
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static String texto(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
