package com.ensambla.backend.domain.model.sami.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoYKitsTallerTest {

    private static final ComponenteCatalogo CLIENTES = new ComponenteCatalogo("clients", "Clientes", "cliente",
            "Reuní los datos", List.of(new CampoComponente("name", "Nombre", "text", true, null)));
    private static final ComponenteCatalogo FORMULARIO =
            new ComponenteCatalogo("form", "Formulario", "registro", "A medida", null);
    private static final PiezaKit TURNOS = new PiezaKit("form", "Turnos",
            List.of(new CampoNuevo("Hora", "time", null)), List.of(Map.of("Hora", "10:30")));
    private static final PiezaKit CLIENTES_DEL_KIT = new PiezaKit("clients", "Mis clientas", null, null);
    private static final KitRubro PELUQUERIA = new KitRubro("peluqueria", "Peluquería", null,
            List.of(TURNOS, CLIENTES_DEL_KIT), null, new OpcionesTaller("¿Vendés productos?", List.of("Sí", "No")));

    @Test
    @DisplayName("Busca un componente por su tipo y devuelve vacío si no existe")
    void buscarShouldFindComponentByType() {
        CatalogoComponentes catalogo = new CatalogoComponentes("aviso", List.of(CLIENTES, FORMULARIO));

        Optional<ComponenteCatalogo> encontrado = catalogo.buscar("form");

        assertThat(encontrado).contains(FORMULARIO);
        assertThat(catalogo.buscar("turnos")).isEmpty();
    }

    @Test
    @DisplayName("Un catálogo sin componentes queda vacío en vez de nulo")
    void catalogoShouldReplaceMissingComponentsWithEmptyList() {
        CatalogoComponentes catalogo = new CatalogoComponentes(null, null);

        assertThat(catalogo.componentes()).isEmpty();
    }

    @Test
    @DisplayName("Sólo la tabla a medida se puede repetir")
    void repetibleShouldBeTrueOnlyForForms() {
        boolean formulario = FORMULARIO.repetible();
        boolean clientes = CLIENTES.repetible();

        assertThat(formulario).isTrue();
        assertThat(clientes).isFalse();
    }

    @Test
    @DisplayName("Los datos de fábrica pasan al taller con su etiqueta y su tipo")
    void camposTallerShouldMapLabelAndType() {
        List<CampoTaller> campos = CLIENTES.camposTaller();

        assertThat(campos).containsExactly(new CampoTaller("Nombre", "text"));
        assertThat(CLIENTES.campos().getFirst().opciones()).isEmpty();
        assertThat(FORMULARIO.campos()).isEmpty();
    }

    @Test
    @DisplayName("El dato que apunta a un componente se llama como una fila, con mayúscula")
    void etiquetaDeVinculoShouldCapitalizeTheSingular() {
        String etiqueta = CLIENTES.etiquetaDeVinculo();

        assertThat(etiqueta).isEqualTo("Cliente");
    }

    @Test
    @DisplayName("Un dato nuevo manda sus opciones sólo si tiene")
    void comoArgumentosShouldIncludeOptionsOnlyWhenPresent() {
        Map<String, Object> lista = new CampoNuevo("Cera", "select", List.of("Soja", "Coco")).comoArgumentos();
        Map<String, Object> texto = new CampoNuevo("Aroma", "text", null).comoArgumentos();

        assertThat(lista).containsExactly(
                Map.entry("etiqueta", "Cera"), Map.entry("tipo", "select"), Map.entry("opciones", List.of("Soja", "Coco")));
        assertThat(texto).containsExactly(Map.entry("etiqueta", "Aroma"), Map.entry("tipo", "text"));
        assertThat(new CampoNuevo("Aroma", "text", null).comoCampo()).isEqualTo(new CampoTaller("Aroma", "text"));
    }

    @Test
    @DisplayName("Una pieza de kit se convierte en los argumentos de agregar_pieza")
    void piezaKitShouldBecomeAddPieceArguments() {
        Map<String, Object> argumentos = TURNOS.comoArgumentos();

        assertThat(argumentos).containsExactly(
                Map.entry("tipo", "form"),
                Map.entry("nombre", "Turnos"),
                Map.entry("campos", List.of(Map.of("etiqueta", "Hora", "tipo", "time"))),
                Map.entry("ejemplos", List.of(Map.of("Hora", "10:30"))));
        assertThat(CLIENTES_DEL_KIT.campos()).isEmpty();
        assertThat(CLIENTES_DEL_KIT.ejemplos()).isEmpty();
    }

    @Test
    @DisplayName("En un kit, la tabla a medida se nombra por su nombre y el resto por su tipo")
    void referenciaDeShouldUseNameForFormsAndTypeForTheRest() {
        assertThat(PELUQUERIA.referenciaDe("Turnos")).isEqualTo("Turnos");
        assertThat(PELUQUERIA.referenciaDe("Mis clientas")).isEqualTo("clients");
        assertThat(PELUQUERIA.referenciaDe("Inexistente")).isEqualTo("Inexistente");
    }

    @Test
    @DisplayName("Un kit sin términos ni conexiones los deja vacíos")
    void kitShouldReplaceMissingListsWithEmptyOnes() {
        assertThat(PELUQUERIA.terminos()).isEmpty();
        assertThat(PELUQUERIA.conexiones()).isEmpty();
        assertThat(new KitRubro("x", "X", null, null, null, null).piezas()).isEmpty();
        assertThat(new KitsRubro(null).kits()).isEmpty();
    }

    @Test
    @DisplayName("Encuentra un kit por id o por nombre sin importar mayúsculas")
    void buscarShouldFindKitByIdOrName() {
        KitsRubro kits = new KitsRubro(List.of(PELUQUERIA));

        assertThat(kits.buscar("PELUQUERIA")).contains(PELUQUERIA);
        assertThat(kits.buscar("peluquería")).contains(PELUQUERIA);
        assertThat(kits.buscar("velas")).isEmpty();
    }
}
