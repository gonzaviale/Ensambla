package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
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
import java.util.Optional;
import java.util.Set;

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

    private static final String MENSAJE_INSUMO_INEXISTENTE = "Ese insumo ya no existe";

    private RecetaId id;

    private NegocioId negocioId;

    private RegistroId productoId;

    private CampoId campoPrecioId;

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
            NegocioId negocioId,
            RegistroId productoId,
            CampoId campoPrecioId,
            List<LineaReceta> lineas,
            OffsetDateTime ahora) {
        List<LineaReceta> limpias = lineas.stream().map(Receta::limpiar).toList();
        exigirDatosValidos(erroresDe(productoId, campoPrecioId, limpias));
        return armar(negocioId, productoId, campoPrecioId, limpias, ahora);
    }

    /** Receta sin líneas ni guardar: la que tiene un producto al que todavía no se le armó una. */
    public static Receta vacia(NegocioId negocioId, RegistroId productoId) {
        return Receta.builder()
                .negocioId(negocioId)
                .productoId(productoId)
                .lineas(List.of())
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

    public List<RegistroId> insumoIds() {
        return lineas.stream().map(LineaReceta::insumoId).toList();
    }

    /** Un mensaje por cada línea cuyo insumo no está entre los {@code existentes}, indexado por {@code lineas[i]}. */
    public Map<String, String> erroresDeInsumosInexistentes(Set<RegistroId> existentes) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (int indice = 0; indice < lineas.size(); indice++) {
            if (!existentes.contains(lineas.get(indice).insumoId())) {
                errores.put(claveDeLinea(indice), MENSAJE_INSUMO_INEXISTENTE);
            }
        }
        return errores;
    }

    private static Receta armar(
            NegocioId negocioId,
            RegistroId productoId,
            CampoId campoPrecioId,
            List<LineaReceta> lineas,
            OffsetDateTime ahora) {
        return Receta.builder()
                .negocioId(negocioId)
                .productoId(productoId)
                .campoPrecioId(lineas.isEmpty() ? null : campoPrecioId)
                .lineas(lineas)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    private static void exigirDatosValidos(Map<String, String> errores) {
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }

    private static Map<String, String> erroresDe(
            RegistroId productoId, CampoId campoPrecioId, List<LineaReceta> lineas) {
        Map<String, String> errores = new LinkedHashMap<>();
        agregarErrorDeCantidadDeLineas(errores, lineas);
        agregarErrorDeColumnaDelPrecio(errores, campoPrecioId, lineas);
        agregarErroresDeLineas(errores, productoId, lineas);
        return errores;
    }

    private static void agregarErrorDeCantidadDeLineas(Map<String, String> errores, List<LineaReceta> lineas) {
        if (lineas.size() > MAX_LINEAS) {
            errores.put("lineas", "Una receta puede tener hasta " + MAX_LINEAS + " insumos");
        }
    }

    private static void agregarErrorDeColumnaDelPrecio(
            Map<String, String> errores, CampoId campoPrecioId, List<LineaReceta> lineas) {
        if (!lineas.isEmpty() && campoPrecioId == null) {
            errores.put("campoPrecioId", "Elegí la columna con el precio de los insumos");
        }
    }

    private static void agregarErroresDeLineas(
            Map<String, String> errores, RegistroId productoId, List<LineaReceta> lineas) {
        Set<RegistroId> vistos = new HashSet<>();
        for (int indice = 0; indice < lineas.size(); indice++) {
            agregarErrorDeLinea(errores, indice, errorDeLinea(lineas.get(indice), productoId, vistos));
        }
    }

    private static void agregarErrorDeLinea(Map<String, String> errores, int indice, Optional<String> error) {
        error.ifPresent(mensaje -> errores.put(claveDeLinea(indice), mensaje));
    }

    private static String claveDeLinea(int indice) {
        return "lineas[" + indice + "]";
    }

    private static LineaReceta limpiar(LineaReceta linea) {
        return new LineaReceta(linea.insumoId(), linea.cantidad(), unidadSinEspacios(linea.unidad()));
    }

    private static String unidadSinEspacios(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return null;
        }
        return unidad.strip();
    }

    private static Optional<String> errorDeLinea(LineaReceta linea, RegistroId productoId, Set<RegistroId> vistos) {
        return errorDeInsumo(linea.insumoId(), productoId, vistos)
                .or(() -> errorDeUnidad(linea.unidad()))
                .or(() -> errorDeCantidad(linea.cantidad()));
    }

    private static Optional<String> errorDeInsumo(RegistroId insumoId, RegistroId productoId, Set<RegistroId> vistos) {
        if (insumoId == null) {
            return Optional.of("Elegí el insumo");
        }
        if (insumoId.equals(productoId)) {
            return Optional.of("Un producto no puede ser insumo de sí mismo");
        }
        if (!vistos.add(insumoId)) {
            return Optional.of("Ese insumo está repetido");
        }
        return Optional.empty();
    }

    private static Optional<String> errorDeUnidad(String unidad) {
        if (unidad != null && unidad.length() > MAX_CARACTERES_UNIDAD) {
            return Optional.of("La unidad puede tener hasta " + MAX_CARACTERES_UNIDAD + " caracteres");
        }
        return Optional.empty();
    }

    private static Optional<String> errorDeCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            return Optional.of("Escribí la cantidad");
        }
        if (cantidad.signum() <= 0) {
            return Optional.of("La cantidad tiene que ser mayor a cero");
        }
        if (cantidad.stripTrailingZeros().scale() > MAX_DECIMALES) {
            return Optional.of("La cantidad admite hasta " + MAX_DECIMALES + " decimales");
        }
        if (cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
            return Optional.of("La cantidad es demasiado grande");
        }
        return Optional.empty();
    }
}
