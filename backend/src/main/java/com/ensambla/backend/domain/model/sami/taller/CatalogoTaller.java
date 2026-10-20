package com.ensambla.backend.domain.model.sami.taller;

import java.util.Map;
import java.util.Set;

/** Vocabulario cerrado del taller: lo único que la IA puede pedir. Debe coincidir con el catálogo del frontend. */
public final class CatalogoTaller {

    /** Tipos de pieza y cómo se llaman para la persona. */
    public static final Map<String, String> TIPOS_DE_PIEZA = Map.of(
            "inventory", "Inventario",
            "sales", "Ventas",
            "clients", "Clientes",
            "orders", "Pedidos",
            "expenses", "Gastos",
            "entries", "Entradas",
            "exits", "Salidas");

    public static final Set<String> TIPOS_DE_CAMPO =
            Set.of("text", "number", "currency", "date", "select", "email", "tel", "checkbox");

    public static final Set<String> DISTRIBUCIONES = Set.of("mosaic", "single", "two", "three");

    public static final int MAX_ACCIONES = 12;
    public static final int MAX_CAMPOS_POR_PIEZA = 12;
    public static final int MAX_LARGO_NOMBRE = 60;
    public static final int MIN_OPCIONES = 2;
    public static final int MAX_OPCIONES = 6;
    public static final int MAX_LARGO_PREGUNTA = 200;

    private CatalogoTaller() {
    }
}
