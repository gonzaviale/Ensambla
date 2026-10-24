package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** editar_campo: cambiar la etiqueta, el tipo, las opciones, si es obligatorio o el lugar de un dato que ya existe. */
class ConversarEnTallerEdicionTest {

    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                    new CampoTaller("Precio", "currency"))),
            new PiezaTaller("e2", "sales", "Ventas", null, List.of(
                    new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number"),
                    new CampoTaller("Precio", "currency"), new CampoTaller("Cliente", "relation"))),
            new PiezaTaller("e3", "clients", "Clientes", null, List.of(
                    new CampoTaller("Nombre", "text"), new CampoTaller("Teléfono", "tel"),
                    new CampoTaller("Rubro", "select")))));

    private TallerDePrueba taller;

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba();
    }

    @Test
    @DisplayName("Renombra un dato buscando la pieza por nombre y el dato sin importar mayúsculas")
    void editarCampoShouldRenameField() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "Ventas", "campo", " cliente ", "etiqueta", " Clienta ")));

        thenActions(result, editada(Map.of("pieza", "e2", "campo", "Cliente", "etiqueta", "Clienta")));
    }

    @Test
    @DisplayName("Puede cambiar sólo las mayúsculas de la etiqueta del mismo dato")
    void editarCampoShouldAcceptCaseChangeOfTheSameField() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "e1", "campo", "Precio", "etiqueta", "PRECIO")));

        thenActions(result, editada(Map.of("pieza", "e1", "campo", "Precio", "etiqueta", "PRECIO")));
    }

    @Test
    @DisplayName("Cambia el tipo de un dato a uno del catálogo sin importar mayúsculas")
    void editarCampoShouldChangeType() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "e1", "campo", "precio", "tipo", " NUMBER ")));

        thenActions(result, editada(Map.of("pieza", "e1", "campo", "Precio", "tipo", "number")));
    }

    @Test
    @DisplayName("Pasa un dato a lista con sus opciones sin repetir y se queda con las primeras seis")
    void editarCampoShouldTurnFieldIntoSelectWithOptions() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "select",
                "opciones", List.of("XS", "S", "XS", "M", "L", "XL", "XXL", "3XL"))));

        thenActions(result, editada(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "select",
                "opciones", List.of("XS", "S", "M", "L", "XL", "XXL"))));
    }

    @Test
    @DisplayName("Descarta pasar un dato a lista sin opciones o con una sola")
    void editarCampoShouldRejectSelectWithoutOptions() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "select")),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "select", "opciones", List.of("Una"))));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("En un dato que ya es lista, las opciones válidas reemplazan a las que tenía y las inválidas descartan")
    void editarCampoShouldReplaceOptionsOfExistingSelect() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e3", "campo", "Rubro", "opciones", List.of("Sola"))),
                editar(Map.of("pieza", "e3", "campo", "Rubro", "tipo", "select", "opciones", List.of("A", "B"))));

        thenActions(result, editada(Map.of("pieza", "e3", "campo", "Rubro", "opciones", List.of("A", "B"))));
    }

    @Test
    @DisplayName("Renombra un dato que ya es lista sin volver a pedir sus opciones")
    void editarCampoShouldRenameSelectWithoutOptions() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "e3", "campo", "Rubro", "etiqueta", "Tipo")));

        thenActions(result, editada(Map.of("pieza", "e3", "campo", "Rubro", "etiqueta", "Tipo")));
    }

    @Test
    @DisplayName("Ignora las opciones en un dato que no es lista y marca si es obligatorio")
    void editarCampoShouldIgnoreOptionsOutsideSelectAndSetRequired() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Precio", "opciones", List.of("A", "B"), "obligatorio", true)),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "obligatorio", false)));

        thenActions(result,
                editada(Map.of("pieza", "e1", "campo", "Precio", "obligatorio", true)),
                editada(Map.of("pieza", "e1", "campo", "Cantidad", "obligatorio", false)));
    }

    @Test
    @DisplayName("Mueve un dato a la posición pedida, que empieza en 1 y llega como número del modelo")
    void editarCampoShouldMoveField() {
        ValidacionTaller result = validar(editar(Map.of("pieza", "sales", "campo", "Cliente", "posicion", 2.0)));

        thenActions(result, editada(Map.of("pieza", "e2", "campo", "Cliente", "posicion", 2)));
    }

    @Test
    @DisplayName("Acota la posición a la cantidad de datos de la pieza")
    void editarCampoShouldClampPosition() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Precio", "posicion", 0)),
                editar(Map.of("pieza", "e1", "campo", "Precio", "posicion", 99)));

        thenActions(result,
                editada(Map.of("pieza", "e1", "campo", "Precio", "posicion", 1)),
                editada(Map.of("pieza", "e1", "campo", "Precio", "posicion", 3)));
    }

    @Test
    @DisplayName("Descarta una posición que no es un entero o que deja el dato donde estaba")
    void editarCampoShouldRejectInvalidOrSamePosition() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Precio", "posicion", 1.5)),
                editar(Map.of("pieza", "e1", "campo", "Precio", "posicion", "2")),
                editar(Map.of("pieza", "e1", "campo", "Precio", "posicion", 3)));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("No cambia el tipo de un vínculo ni del primer dato: esa parte se ignora y, si no queda nada, se descarta")
    void editarCampoShouldKeepTypeOfLinksAndFirstField() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e2", "campo", "Cliente", "tipo", "text")),
                editar(Map.of("pieza", "e2", "campo", "Producto", "tipo", "relation")),
                editar(Map.of("pieza", "e1", "campo", "Producto", "tipo", "select", "opciones", List.of("A", "B"),
                        "etiqueta", "Artículo")));

        thenActions(result, editada(Map.of("pieza", "e1", "campo", "Producto", "etiqueta", "Artículo")));
    }

    @Test
    @DisplayName("Descarta una pieza o un dato inexistente, o una llamada sin dato")
    void editarCampoShouldRejectUnknownPieceOrField() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "fantasma", "campo", "Precio", "etiqueta", "Costo")),
                editar(Map.of("pieza", "e1", "campo", "Color", "etiqueta", "Costo")),
                editar(Map.of("pieza", "e1", "etiqueta", "Costo")));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("Descarta una etiqueta repetida, muy larga o que no es texto")
    void editarCampoShouldRejectInvalidLabels() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "etiqueta", "precio")),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "etiqueta", "x".repeat(61))),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "etiqueta", 5)));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("Descarta un tipo que no es del catálogo, un vínculo o un obligatorio que no es sí o no")
    void editarCampoShouldRejectInvalidTypeOrRequired() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "color")),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "tipo", "relation")),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "obligatorio", "sí")));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("Descarta la llamada si no cambia nada: sin partes, en blanco o con los mismos valores")
    void editarCampoShouldRejectWhenNothingChanges() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Cantidad")),
                editar(Map.of("pieza", "e3", "campo", "Rubro", "etiqueta", " ", "opciones", List.of())),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "etiqueta", "Cantidad", "tipo", "NUMBER")),
                editar(Map.of("pieza", "e3", "campo", "Rubro", "tipo", "select")));

        thenDiscarded(result);
    }

    @Test
    @DisplayName("Las llamadas siguientes del turno ven la etiqueta, el tipo y el lugar nuevos del dato")
    void editarCampoShouldTrackChangesWithinTheTurn() {
        ValidacionTaller result = validar(
                editar(Map.of("pieza", "e1", "campo", "Precio", "etiqueta", "Costo", "posicion", 2)),
                editar(Map.of("pieza", "e1", "campo", "Precio", "obligatorio", true)),
                editar(Map.of("pieza", "e1", "campo", "costo", "tipo", "number")),
                editar(Map.of("pieza", "e1", "campo", "Costo", "tipo", "number")),
                editar(Map.of("pieza", "e1", "campo", "Cantidad", "posicion", 3)),
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "costo")));

        thenActions(result,
                editada(Map.of("pieza", "e1", "campo", "Precio", "etiqueta", "Costo", "posicion", 2)),
                editada(Map.of("pieza", "e1", "campo", "Costo", "tipo", "number")),
                new AccionTaller("quitar_campo", Map.of("pieza", "e1", "campo", "Costo")));
    }

    @Test
    @DisplayName("Edita un dato de una pieza que se sumó en el mismo turno y la nombra por su tipo")
    void editarCampoShouldEditPieceAddedInTheSameTurn() {
        ValidacionTaller result = validar(
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                editar(Map.of("pieza", "Gastos", "campo", "concepto", "etiqueta", "Detalle")));

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "editar_campo");
        assertThat(result.acciones().get(1))
                .isEqualTo(editada(Map.of("pieza", "expenses", "campo", "Concepto", "etiqueta", "Detalle")));
    }

    private ValidacionTaller validar(LlamadaHerramienta... llamadas) {
        return taller.validar(List.of(llamadas), ESTADO);
    }

    private LlamadaHerramienta editar(Map<String, Object> argumentos) {
        return llamada("editar_campo", argumentos);
    }

    private LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private AccionTaller editada(Map<String, Object> argumentos) {
        return new AccionTaller("editar_campo", argumentos);
    }

    private void thenActions(ValidacionTaller result, AccionTaller... esperadas) {
        assertThat(result.acciones()).containsExactly(esperadas);
    }

    private void thenDiscarded(ValidacionTaller result) {
        assertThat(result.acciones()).isEmpty();
    }
}
