package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Receta de un producto: sus insumos con la cantidad de cada uno y la columna numérica de la que sale el precio de
 * los insumos. Una receta sin líneas es una receta que se quita.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receta {

    static final int MAX_LINEAS = 50;

    static final int MAX_CARACTERES_UNIDAD = 30;

    static final int MAX_DECIMALES = 3;

    static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("99999999999.999");

    private UUID id;

    private UUID negocioId;

    private UUID productoId;

    private UUID campoPrecioId;

    private List<LineaReceta> lineas;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /**
     * Receta nueva (todavía sin guardar). Con líneas hace falta la columna del precio; no puede repetir un insumo ni
     * usar al propio producto; cada cantidad es mayor a cero y tiene hasta tres decimales.
     *
     * @throws RegistroInvalidoException con un mensaje por dato inválido: {@code campoPrecioId} o {@code lineas[i]}
     */
    public static Receta nueva(
            UUID negocioId,
            UUID productoId,
            UUID campoPrecioId,
            List<LineaReceta> lineas,
            OffsetDateTime ahora) {
        Map<String, String> errores = new LinkedHashMap<>();
        List<LineaReceta> limpias = lineas.stream().map(Receta::limpiar).toList();
        if (limpias.size() > MAX_LINEAS) {
            errores.put("lineas", "Una receta puede tener hasta " + MAX_LINEAS + " insumos");
        }
        if (!limpias.isEmpty() && campoPrecioId == null) {
            errores.put("campoPrecioId", "Elegí la columna con el precio de los insumos");
        }
        Set<UUID> vistos = new HashSet<>();
        for (int indice = 0; indice < limpias.size(); indice++) {
            String error = errorDeLinea(limpias.get(indice), productoId, vistos);
            if (error != null) {
                errores.put("lineas[" + indice + "]", error);
            }
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
        return Receta.builder()
                .negocioId(negocioId)
                .productoId(productoId)
                .campoPrecioId(limpias.isEmpty() ? null : campoPrecioId)
                .lineas(limpias)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** La receta que se quita o que nunca se armó. */
    public boolean esVacia() {
        return lineas.isEmpty();
    }

    /** Conserva el id y la fecha de creación de la receta guardada que esta reemplaza. */
    public void reemplazar(Receta guardada) {
        this.id = guardada.getId();
        this.creadoEn = guardada.getCreadoEn();
    }

    public List<UUID> insumoIds() {
        return lineas.stream().map(LineaReceta::insumoId).toList();
    }

    private static LineaReceta limpiar(LineaReceta linea) {
        String unidad = linea.unidad() == null || linea.unidad().isBlank() ? null : linea.unidad().strip();
        return new LineaReceta(linea.insumoId(), linea.cantidad(), unidad);
    }

    private static String errorDeLinea(LineaReceta linea, UUID productoId, Set<UUID> vistos) {
        if (linea.insumoId() == null) {
            return "Elegí el insumo";
        }
        if (linea.insumoId().equals(productoId)) {
            return "Un producto no puede ser insumo de sí mismo";
        }
        if (!vistos.add(linea.insumoId())) {
            return "Ese insumo está repetido";
        }
        if (linea.unidad() != null && linea.unidad().length() > MAX_CARACTERES_UNIDAD) {
            return "La unidad puede tener hasta " + MAX_CARACTERES_UNIDAD + " caracteres";
        }
        return errorDeCantidad(linea.cantidad());
    }

    private static String errorDeCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            return "Escribí la cantidad";
        }
        if (cantidad.signum() <= 0) {
            return "La cantidad tiene que ser mayor a cero";
        }
        if (cantidad.stripTrailingZeros().scale() > MAX_DECIMALES) {
            return "La cantidad admite hasta " + MAX_DECIMALES + " decimales";
        }
        if (cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
            return "La cantidad es demasiado grande";
        }
        return null;
    }
}
