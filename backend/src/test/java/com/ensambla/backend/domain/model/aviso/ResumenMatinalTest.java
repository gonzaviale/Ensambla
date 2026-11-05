package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResumenMatinalTest {

    private static final String NEGOCIO = "Almacén de Ana";

    @Test
    @DisplayName("Arma el mensaje con el saludo, una línea por novedad en el orden de las secciones y el cierre")
    void armarShouldWriteOneLinePerNewsInOrder() {
        ResumenMatinal resumen = new ResumenMatinal(List.of(
                new SeccionAgotados(), new SeccionBajoMinimo(), new SeccionAvisosDePrecio()));

        Optional<String> result = resumen.armar("Ana", NEGOCIO, List.of(
                aviso(TipoAviso.PRECIO_ZONA, 0), aviso(TipoAviso.STOCK, 3), aviso(TipoAviso.STOCK, 0)));

        assertThat(result).contains(
                "Buen día, Ana. Sami revisó Almacén de Ana y encontró:\n"
                        + "- 1 producto agotado\n"
                        + "- 1 producto por debajo del mínimo\n"
                        + "- 1 aviso de precio\n"
                        + "Abrí Ensambla para ver los detalles.");
    }

    @Test
    @DisplayName("Sin nombre saluda sin nombre")
    void armarShouldGreetWithoutNameWhenThereIsNone() {
        ResumenMatinal resumen = new ResumenMatinal(List.of(new SeccionAgotados()));

        Optional<String> result = resumen.armar(null, NEGOCIO, List.of(aviso(TipoAviso.STOCK, 0)));

        assertThat(result).hasValueSatisfying(texto -> assertThat(texto).startsWith("Buen día. Sami revisó"));
    }

    @Test
    @DisplayName("Las secciones sin novedades no aparecen")
    void armarShouldSkipSectionsWithoutNews() {
        ResumenMatinal resumen = new ResumenMatinal(List.of(new SeccionAgotados(), new SeccionAvisosDePrecio()));

        Optional<String> result = resumen.armar("Ana", NEGOCIO, List.of(aviso(TipoAviso.PRECIO_ZONA, 0)));

        assertThat(result).hasValueSatisfying(texto -> assertThat(texto)
                .contains("- 1 aviso de precio").doesNotContain("agotado"));
    }

    @Test
    @DisplayName("Si ninguna sección tiene algo que contar no hay mensaje")
    void armarShouldBeEmptyWhenThereIsNothingToTell() {
        ResumenMatinal resumen = new ResumenMatinal(List.of(new SeccionAgotados(), new SeccionAvisosDePrecio()));

        Optional<String> result = resumen.armar("Ana", NEGOCIO, List.of(aviso(TipoAviso.COMBO, 0)));

        assertThat(result).isEmpty();
    }

    // --- helpers ---
    private static Aviso aviso(TipoAviso tipo, Number cantidad) {
        return Aviso.builder().tipo(tipo).grupo(tipo.grupo()).estado(EstadoAviso.PENDIENTE)
                .datos(Map.of("cantidad", cantidad)).build();
    }
}
