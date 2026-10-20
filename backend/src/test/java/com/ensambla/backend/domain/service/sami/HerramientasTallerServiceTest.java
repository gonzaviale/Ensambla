package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HerramientasTallerServiceTest {

    private HerramientasTallerService service;
    private EstadoTaller estado;

    @BeforeEach
    void setUp() {
        service = new HerramientasTallerService(SamiPruebas.catalogoReal(), SamiPruebas.kitsReales());
        estado = new EstadoTaller("Mi almacén", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                        new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                        new CampoTaller("Precio", "currency"))),
                new PiezaTaller("e2", "clients", "Clientes", "General", List.of(new CampoTaller("Nombre", "text")))));
    }

    @Test
    @DisplayName("Ignora una herramienta que no existe y la cuenta como descartada")
    void validarShouldDiscardUnknownTool() {
        ValidacionTaller result = validar(llamada("borrar_todo", Map.of()));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sin llamadas no hay acciones ni opciones")
    void validarShouldReturnNothingWithoutCalls() {
        ValidacionTaller result = validar();

        assertThat(result.acciones()).isEmpty();
        assertThat(result.preguntas()).isEmpty();
        assertThat(result.descartadas()).isZero();
    }

    @Test
    @DisplayName("Suma una pieza del catálogo sin importar mayúsculas")
    void agregarPiezaShouldAcceptCatalogType() {
        ValidacionTaller result = validar(llamada("agregar_pieza", Map.of("tipo", " EXPENSES ")));

        assertThat(result.acciones()).containsExactly(accion("agregar_pieza", Map.of("tipo", "expenses")));
        assertThat(result.descartadas()).isZero();
    }

    @Test
    @DisplayName("No suma tipos inventados, ya presentes, vacíos ni que no sean texto")
    void agregarPiezaShouldRejectInvalidTypes() {
        ValidacionTaller result = validar(
                llamada("agregar_pieza", Map.of("tipo", "turnos")),
                llamada("agregar_pieza", Map.of("tipo", "inventory")),
                llamada("agregar_pieza", Map.of("tipo", "  ")),
                llamada("agregar_pieza", Map.of("tipo", 5)),
                llamada("agregar_pieza", Map.of()));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(5);
    }

    @Test
    @DisplayName("No suma dos veces el mismo tipo en un turno")
    void agregarPiezaShouldNotAddSameTypeTwiceInOneTurn() {
        ValidacionTaller result = validar(
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                llamada("agregar_pieza", Map.of("tipo", "expenses")));

        assertThat(result.acciones()).hasSize(1);
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Quita una pieza por id, por nombre o por tipo y ya no la encuentra después")
    void quitarPiezaShouldResolveByIdNameOrType() {
        ValidacionTaller result = validar(
                llamada("quitar_pieza", Map.of("pieza", "e1")),
                llamada("quitar_pieza", Map.of("pieza", "CLIENTES")),
                llamada("quitar_pieza", Map.of("pieza", "inventory")));

        assertThat(result.acciones()).containsExactly(
                accion("quitar_pieza", Map.of("pieza", "e1")),
                accion("quitar_pieza", Map.of("pieza", "e2")));
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Encuentra una pieza por su tipo aunque venga en mayúsculas")
    void quitarPiezaShouldResolveByTypeIgnoringCase() {
        ValidacionTaller result = validar(llamada("quitar_pieza", Map.of("pieza", "CLIENTS")));

        assertThat(result.acciones()).containsExactly(accion("quitar_pieza", Map.of("pieza", "e2")));
    }

    @Test
    @DisplayName("Quita por tipo una pieza que se sumó en el mismo turno")
    void quitarPiezaShouldUseTypeForPiecesAddedInTheSameTurn() {
        ValidacionTaller result = validar(
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("quitar_pieza", Map.of("pieza", "Ventas")));

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "quitar_pieza");
        assertThat(result.acciones().get(1).argumentos()).containsEntry("pieza", "sales");
    }

    @Test
    @DisplayName("No quita una pieza inexistente o sin referencia")
    void quitarPiezaShouldRejectUnknownPiece() {
        ValidacionTaller result = validar(
                llamada("quitar_pieza", Map.of("pieza", "fantasma")),
                llamada("quitar_pieza", Map.of()));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Renombra una pieza con un nombre válido")
    void renombrarPiezaShouldAcceptValidName() {
        ValidacionTaller result = validar(llamada("renombrar_pieza", Map.of("pieza", "Clientes", "nombre", " Mis socios ")));

        assertThat(result.acciones())
                .containsExactly(accion("renombrar_pieza", Map.of("pieza", "e2", "nombre", "Mis socios")));
    }

    @Test
    @DisplayName("No renombra sin nombre, con un nombre muy largo o una pieza inexistente")
    void renombrarPiezaShouldRejectInvalidInput() {
        ValidacionTaller result = validar(
                llamada("renombrar_pieza", Map.of("pieza", "e2")),
                llamada("renombrar_pieza", Map.of("pieza", "e2", "nombre", "x".repeat(61))),
                llamada("renombrar_pieza", Map.of("pieza", "nada", "nombre", "Otro")));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Renombra el sistema y rechaza un nombre vacío")
    void renombrarSistemaShouldAcceptOnlyNonBlankNames() {
        ValidacionTaller result = validar(
                llamada("renombrar_sistema", Map.of("nombre", "Velas de Ana")),
                llamada("renombrar_sistema", Map.of("nombre", " ")));

        assertThat(result.acciones()).containsExactly(accion("renombrar_sistema", Map.of("nombre", "Velas de Ana")));
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Suma un dato de texto a una pieza y no manda opciones")
    void agregarCampoShouldAddTextField() {
        ValidacionTaller result = validar(llamada("agregar_campo", Map.of(
                "pieza", "e1", "etiqueta", "Proveedor", "tipo", "TEXT", "opciones", List.of("ignorada"))));

        assertThat(result.acciones()).containsExactly(accion(
                "agregar_campo", Map.of("pieza", "e1", "etiqueta", "Proveedor", "tipo", "text")));
    }

    @Test
    @DisplayName("Suma un dato de lista con sus opciones sin repetir")
    void agregarCampoShouldAddSelectFieldWithOptions() {
        ValidacionTaller result = validar(llamada("agregar_campo", Map.of(
                "pieza", "e1", "etiqueta", "Rubro", "tipo", "select",
                "opciones", List.of("Insumos", "Terminados", "Insumos", " "))));

        assertThat(result.acciones()).hasSize(1);
        assertThat(result.acciones().getFirst().argumentos())
                .containsEntry("opciones", List.of("Insumos", "Terminados"));
    }

    @Test
    @DisplayName("Rechaza datos sin etiqueta, de tipo inválido, repetidos o de lista con menos de dos opciones")
    void agregarCampoShouldRejectInvalidFields() {
        ValidacionTaller result = validar(
                llamada("agregar_campo", Map.of("pieza", "e1", "tipo", "text")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "X")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "X", "tipo", "color")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "producto", "tipo", "text")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Rubro", "tipo", "select",
                        "opciones", List.of("Una sola"))),
                llamada("agregar_campo", Map.of("pieza", "nada", "etiqueta", "Y", "tipo", "text")));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(6);
    }

    @Test
    @DisplayName("No pasa de doce datos por pieza")
    void agregarCampoShouldRespectFieldLimit() {
        List<CampoTaller> campos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            campos.add(new CampoTaller("Dato " + i, "text"));
        }
        EstadoTaller lleno = new EstadoTaller("Lleno", "mosaic", List.of(
                new PiezaTaller("e9", "inventory", "Inventario", null, campos)));

        ValidacionTaller result = service.validar(
                List.of(llamada("agregar_campo", Map.of("pieza", "e9", "etiqueta", "Uno más", "tipo", "text"))), lleno);

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un dato recién sumado cuenta: no se puede repetir ni se puede quitar en el mismo turno")
    void agregarCampoShouldTrackFieldsWithinTheTurn() {
        ValidacionTaller result = validar(
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Proveedor", "tipo", "text")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "PROVEEDOR", "tipo", "text")),
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "proveedor")));

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_campo", "quitar_campo");
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Quita un dato con la etiqueta exacta que tiene la pieza")
    void quitarCampoShouldUseExistingLabel() {
        ValidacionTaller result = validar(llamada("quitar_campo", Map.of("pieza", "e1", "campo", "precio")));

        assertThat(result.acciones())
                .containsExactly(accion("quitar_campo", Map.of("pieza", "e1", "campo", "Precio")));
    }

    @Test
    @DisplayName("No quita un dato obligatorio del componente ni un vínculo")
    void quitarCampoShouldKeepRequiredFieldsAndLinks() {
        EstadoTaller conVinculo = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "sales", "Ventas", null, List.of(
                        new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number"),
                        new CampoTaller("Cliente", "text"))),
                new PiezaTaller("e2", "calendar", "Agenda", null, List.of(new CampoTaller("Cantidad", "number")))));

        ValidacionTaller result = service.validar(List.of(
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "Producto")),
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "Cantidad")),
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "Cliente")),
                llamada("quitar_campo", Map.of("pieza", "e2", "campo", "Cantidad"))), conVinculo);

        assertThat(result.acciones()).containsExactly(
                accion("quitar_campo", Map.of("pieza", "e1", "campo", "Cliente")),
                accion("quitar_campo", Map.of("pieza", "e2", "campo", "Cantidad")));
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("No quita un dato inexistente, sin nombre o de una pieza que no existe")
    void quitarCampoShouldRejectInvalidInput() {
        ValidacionTaller result = validar(
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "Color")),
                llamada("quitar_campo", Map.of("pieza", "e1")),
                llamada("quitar_campo", Map.of("pieza", "nada", "campo", "Producto")));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Separa una pieza en una sección con nombre y rechaza la que no lo tiene")
    void separarEnSeccionShouldRequireSectionName() {
        ValidacionTaller result = validar(
                llamada("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Mis clientes")),
                llamada("separar_en_seccion", Map.of("pieza", "e2")),
                llamada("separar_en_seccion", Map.of("pieza", "nada", "seccion", "X")));

        assertThat(result.acciones())
                .containsExactly(accion("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Mis clientes")));
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cambia la distribución sólo a una del catálogo")
    void cambiarDistribucionShouldAcceptOnlyCatalogLayouts() {
        ValidacionTaller result = validar(
                llamada("cambiar_distribucion", Map.of("layout", "TWO")),
                llamada("cambiar_distribucion", Map.of("layout", "diagonal")),
                llamada("cambiar_distribucion", Map.of()));

        assertThat(result.acciones()).containsExactly(accion("cambiar_distribucion", Map.of("layout", "two")));
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Una pregunta con opciones válidas llega como botonera y no cuenta como acción")
    void ofrecerOpcionesShouldReturnOptions() {
        ValidacionTaller result = validar(llamada("ofrecer_opciones", Map.of(
                "pregunta", " ¿Qué querés sumar? ", "opciones", List.of("Gastos", " Clientes ", "Gastos"))));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.preguntas()).hasSize(1);
        assertThat(result.preguntas().getFirst().pregunta()).isEqualTo("¿Qué querés sumar?");
        assertThat(result.preguntas().getFirst().opciones()).containsExactly("Gastos", "Clientes");
    }

    @Test
    @DisplayName("Se queda con las primeras seis opciones")
    void ofrecerOpcionesShouldKeepAtMostSixOptions() {
        List<String> muchas = List.of("1", "2", "3", "4", "5", "6", "7", "8");

        ValidacionTaller result = validar(llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál?", "opciones", muchas)));

        assertThat(result.preguntas().getFirst().opciones()).containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    @DisplayName("Descarta preguntas sin texto, con menos de dos opciones o con opciones que no son una lista")
    void ofrecerOpcionesShouldRejectInvalidOptions() {
        Map<String, Object> noEsLista = new HashMap<>();
        noEsLista.put("pregunta", "¿Cuál?");
        noEsLista.put("opciones", "Gastos");
        ValidacionTaller result = validar(
                llamada("ofrecer_opciones", Map.of("opciones", List.of("A", "B"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál?", "opciones", List.of("Sola"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál?", "opciones", List.of("A", 5, " "))),
                llamada("ofrecer_opciones", noEsLista));

        assertThat(result.preguntas()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(4);
    }

    @Test
    @DisplayName("Una pregunta inválida posterior no pisa a la anterior")
    void ofrecerOpcionesShouldKeepPreviousValidQuestion() {
        ValidacionTaller result = validar(
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál?", "opciones", List.of("A", "B"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Otra?", "opciones", List.of("A"))));

        assertThat(result.preguntas()).extracting("pregunta").containsExactly("¿Cuál?");
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Aplica como mucho doce acciones por turno y descarta el resto")
    void validarShouldCapActionsPerTurn() {
        List<LlamadaHerramienta> catorce = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            catorce.add(llamada("cambiar_distribucion", Map.of("layout", "single")));
        }

        ValidacionTaller result = service.validar(catorce, estado);

        assertThat(result.acciones()).hasSize(12);
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Busca una pieza primero por id, después por tipo y por último por nombre")
    void validarShouldResolvePiecesByIdThenTypeThenName() {
        EstadoTaller cruzado = new EstadoTaller("Cruzado", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Sales", null, List.of()),
                new PiezaTaller("e2", "sales", "Caja", null, List.of()),
                new PiezaTaller("e2b", "clients", "e2", null, List.of())));

        ValidacionTaller result = service.validar(List.of(
                llamada("separar_en_seccion", Map.of("pieza", "sales", "seccion", "Caja")),
                llamada("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Otra")),
                llamada("separar_en_seccion", Map.of("pieza", "«Sales»", "seccion", "Depósito"))), cruzado);

        assertThat(result.acciones()).containsExactly(
                accion("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Caja")),
                accion("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Otra")),
                accion("separar_en_seccion", Map.of("pieza", "e1", "seccion", "Depósito")));
    }

    @Test
    @DisplayName("Encuentra una pieza por su nombre sin importar tildes, mayúsculas ni plural")
    void validarShouldResolveNamesIgnoringAccentsAndPlural() {
        EstadoTaller conDeudores = new EstadoTaller("Velas", "mosaic", List.of(
                new PiezaTaller("e7", "clients", "Quién me debe", null, List.of()),
                new PiezaTaller("e8", "form", "Material", null, List.of())));

        ValidacionTaller result = service.validar(List.of(
                llamada("quitar_pieza", Map.of("pieza", "QUIEN ME DEBE")),
                llamada("quitar_pieza", Map.of("pieza", "materiales"))), conDeudores);

        assertThat(result.acciones()).containsExactly(
                accion("quitar_pieza", Map.of("pieza", "e7")),
                accion("quitar_pieza", Map.of("pieza", "e8")));
    }

    @Test
    @DisplayName("Un dato de lista se queda con las primeras seis opciones")
    void agregarCampoShouldKeepAtMostSixOptions() {
        ValidacionTaller result = validar(llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Talle",
                "tipo", "select", "opciones", List.of("XS", "S", "M", "L", "XL", "XXL", "3XL"))));

        assertThat(result.acciones().getFirst().argumentos())
                .containsEntry("opciones", List.of("XS", "S", "M", "L", "XL", "XXL"));
    }

    @Test
    @DisplayName("No suma un vínculo como dato común: eso es conectar")
    void agregarCampoShouldRejectRelationType() {
        ValidacionTaller result = validar(
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Cliente", "tipo", "relation")));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(1);
    }

    private ValidacionTaller validar(LlamadaHerramienta... llamadas) {
        return service.validar(List.of(llamadas), estado);
    }

    private LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private AccionTaller accion(String herramienta, Map<String, Object> argumentos) {
        return new AccionTaller(herramienta, argumentos);
    }
}
