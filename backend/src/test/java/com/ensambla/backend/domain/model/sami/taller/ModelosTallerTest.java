package com.ensambla.backend.domain.model.sami.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelosTallerTest {

    @Test
    @DisplayName("Las listas ausentes quedan vacías en vez de nulas")
    void constructorsShouldReplaceMissingListsWithEmptyOnes() {
        assertThat(new PiezaTaller("e1", "inventory", "Inventario", null, null).campos()).isEmpty();
        assertThat(new PiezaTaller("e1", "inventory", "Inventario", null, null, null).registros()).isEmpty();
        assertThat(new PiezaTaller("e1", "inventory", "Inventario", null, null, null, null).nombres()).isEmpty();
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
    @DisplayName("Los vínculos no se suman como un dato más: sólo con conectar")
    void catalogShouldNotAllowRelationsAsPlainFields() {
        assertThat(CatalogoTaller.TIPOS_DE_CAMPO)
                .contains("textarea", "time")
                .doesNotContain(CatalogoTaller.TIPO_VINCULO);
    }

    @Test
    @DisplayName("Una consulta armada sin kit lo deja vacío")
    void consultaShouldHaveNoKitWhenBuiltWithoutOne() {
        ConsultaTaller consulta = new ConsultaTaller("hola", new EstadoTaller("S", "mosaic", null), List.of());

        assertThat(consulta.kit()).isNull();
    }
}
