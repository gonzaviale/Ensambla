package com.ensambla.backend.domain.model.sami.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelosTallerTest {

    @Test
    @DisplayName("Las listas ausentes quedan vacías en vez de nulas")
    void constructorsShouldReplaceMissingListsWithEmptyOnes() {
        assertThat(new PiezaTaller("e1", "inventory", "Inventario", null, null).campos()).isEmpty();
        assertThat(new EstadoTaller("Sistema", "mosaic", null).piezas()).isEmpty();
        assertThat(new LlamadaHerramienta("agregar_pieza", null).argumentos()).isEmpty();
    }

    @Test
    @DisplayName("Los argumentos de una llamada admiten valores nulos y no se pueden modificar")
    void llamadaShouldAllowNullValuesAndBeImmutable() {
        Map<String, Object> argumentos = new HashMap<>();
        argumentos.put("tipo", null);

        LlamadaHerramienta llamada = new LlamadaHerramienta("agregar_pieza", argumentos);

        assertThat(llamada.argumentos()).containsEntry("tipo", null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> llamada.argumentos().put("otro", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Encuentra una herramienta por su nombre y devuelve vacío si no existe")
    void herramientaShouldBeFoundByName() {
        assertThat(HerramientaTaller.deNombre("ofrecer_opciones")).contains(HerramientaTaller.OFRECER_OPCIONES);
        assertThat(HerramientaTaller.deNombre("agregar_pieza").orElseThrow().nombre()).isEqualTo("agregar_pieza");
        assertThat(HerramientaTaller.deNombre("inventada")).isEmpty();
    }

    @Test
    @DisplayName("El catálogo tiene los siete tipos de pieza del frontend")
    void catalogShouldHaveTheFrontendPieceTypes() {
        assertThat(CatalogoTaller.TIPOS_DE_PIEZA).containsOnlyKeys(
                "inventory", "sales", "clients", "orders", "expenses", "entries", "exits");
    }
}
