package com.ensambla.backend.domain.model.sami.taller;

import java.util.Map;
import java.util.Set;

/**
 * Vocabulario cerrado y topes del taller: lo único que la IA puede pedir. Los tipos de pieza salen del catálogo de
 * componentes ({@link CatalogoComponentes}).
 */
public final class CatalogoTaller {

    /** Tipos de dato que Sami puede sumar; los vínculos ({@code relation}) sólo se arman con {@code conectar}. */
    public static final Set<String> TIPOS_DE_CAMPO = Set.of(
            "text", "textarea", "number", "currency", "date", "time", "select", "email", "tel", "checkbox");

    public static final String TIPO_VINCULO = "relation";
    public static final String TIPO_LISTA = "select";

    public static final Set<String> DISTRIBUCIONES = Set.of("mosaic", "single", "two", "three");

    /**
     * Vínculos que el taller arma solo ({@code wireSystem} en el frontend): ventas y pedidos con clientes, y ventas,
     * entradas y salidas con el inventario. Un {@code conectar} entre esos tipos sumaría un segundo vínculo, así que se
     * descarta.
     */
    public static final Map<String, Set<String>> VINCULOS_AUTOMATICOS = Map.of(
            "sales", Set.of("clients", "inventory"),
            "orders", Set.of("clients"),
            "entries", Set.of("inventory"),
            "exits", Set.of("inventory"));

    public static boolean seVinculaSolo(String desde, String hacia) {
        return VINCULOS_AUTOMATICOS.getOrDefault(desde, Set.of()).contains(hacia);
    }

    /** Acciones que puede pedir el modelo en un turno; un {@code usar_kit} cuenta como una. */
    public static final int MAX_ACCIONES = 12;
    /** Tope de acciones por turno contando las que salen de expandir un kit. */
    public static final int MAX_ACCIONES_TOTALES = 24;
    public static final int MAX_CAMPOS_POR_PIEZA = 12;
    /** Datos que se pueden sumar al crear una pieza, además de los del componente. */
    public static final int MAX_CAMPOS_EXTRA = 8;
    public static final int MAX_FILAS_EJEMPLO = 5;
    public static final int MAX_LARGO_NOMBRE = 60;
    public static final int MAX_LARGO_VALOR = 80;
    public static final int MIN_OPCIONES = 2;
    public static final int MAX_OPCIONES = 6;
    public static final int MAX_LARGO_PREGUNTA = 200;

    private CatalogoTaller() {
    }
}
