package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConexionKit;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Las herramientas para ensamblar: agregar_pieza completa, usar_kit, cargar_ejemplos y conectar. */
class HerramientasTallerArmadoTest {

    private static final EstadoTaller VACIO = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi negocio", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Stock", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"))),
            new PiezaTaller("e2", "clients", "Clientes", null, List.of(new CampoTaller("Nombre", "text")))));

    private HerramientasTallerService herramientasTallerService;

    @BeforeEach
    void setUp() {
        herramientasTallerService = new HerramientasTallerService(SamiPruebas.catalogoReal(), SamiPruebas.kitsReales());
    }

    @Test
    @DisplayName("Suma una pieza con nombre, datos extra, ejemplos y sección")
    void agregarPiezaShouldAcceptNameFieldsExamplesAndSection() {
        ValidacionTaller result = validar(VACIO, llamada("agregar_pieza", Map.of(
                "tipo", "inventory",
                "nombre", " Velas ",
                "campos", List.of(Map.of("etiqueta", "Aroma", "tipo", "text"),
                        Map.of("etiqueta", "Cera", "tipo", "SELECT", "opciones", List.of("Soja", "Coco"))),
                "ejemplos", List.of(Map.of("producto", "Vela lavanda", "Cantidad", 8, "Aroma", "Lavanda")),
                "seccion", "Taller")));

        thenOnlyActionIs(result, accion("agregar_pieza", Map.of(
                "tipo", "inventory",
                "nombre", "Velas",
                "campos", List.of(Map.of("etiqueta", "Aroma", "tipo", "text"),
                        Map.of("etiqueta", "Cera", "tipo", "select", "opciones", List.of("Soja", "Coco"))),
                "ejemplos", List.of(Map.of("Producto", "Vela lavanda", "Cantidad", 8, "Aroma", "Lavanda")),
                "seccion", "Taller")));
    }

    @Test
    @DisplayName("Suma componentes sin datos, como el resumen, sin datos extra ni ejemplos")
    void agregarPiezaShouldAcceptComponentsWithoutFields() {
        ValidacionTaller result = validar(VACIO, llamada("agregar_pieza", Map.of(
                "tipo", "summary",
                "campos", List.of(Map.of("etiqueta", "Total", "tipo", "number")),
                "ejemplos", List.of(Map.of("Total", 3)))));

        thenOnlyActionIs(result, accion("agregar_pieza", Map.of("tipo", "summary")));
    }

    @Test
    @DisplayName("No suma una pieza cuyo nombre de catálogo ya usa otra pieza")
    void agregarPiezaShouldRejectDefaultNameAlreadyInUse() {
        EstadoTaller conTablaVentas = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "form", "Ventas", null, List.of(new CampoTaller("Nombre", "text")))));

        ValidacionTaller result = validar(conTablaVentas,
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("agregar_pieza", Map.of("tipo", "sales", "nombre", "Caja")));

        assertThat(result.acciones()).containsExactly(
                accion("agregar_pieza", Map.of("tipo", "sales", "nombre", "Caja")));
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("La tabla a medida exige nombre, se puede repetir y no repite un nombre que ya existe")
    void agregarPiezaShouldRequireUniqueNameForForms() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "form")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Trabajos")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "turno")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Clientes")));

        assertThat(result.acciones()).containsExactly(
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Trabajos")));
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Descarta los datos extra inválidos y se queda con los válidos")
    void agregarPiezaShouldDropInvalidExtraFields() {
        List<Object> campos = new ArrayList<>();
        campos.add("no es un objeto");
        campos.add(Map.of("tipo", "text"));
        campos.add(Map.of("etiqueta", "Color", "tipo", "color"));
        campos.add(Map.of("etiqueta", "Cliente", "tipo", "relation"));
        campos.add(Map.of("etiqueta", "Talle", "tipo", "select", "opciones", List.of("Único")));
        campos.add(Map.of("etiqueta", "precio", "tipo", "currency"));
        campos.add(Map.of("etiqueta", "Marca", "tipo", "text"));
        campos.add(Map.of("etiqueta", "MARCA", "tipo", "text"));

        ValidacionTaller result = validar(VACIO, llamada("agregar_pieza", Map.of("tipo", "inventory", "campos", campos)));

        assertThat(result.acciones().getFirst().argumentos())
                .containsEntry("campos", List.of(Map.of("etiqueta", "Marca", "tipo", "text")));
    }

    @Test
    @DisplayName("No pasa de ocho datos extra ni de doce datos por pieza")
    void agregarPiezaShouldCapExtraFields() {
        List<Map<String, Object>> nueve = IntStream.rangeClosed(1, 9)
                .mapToObj(numero -> Map.<String, Object>of("etiqueta", "Dato " + numero, "tipo", "text"))
                .toList();

        ValidacionTaller result = validar(VACIO,
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Recetas", "campos", nueve)),
                llamada("agregar_pieza", Map.of("tipo", "inventory", "campos", nueve)));

        assertThat((List<?>) result.acciones().get(0).argumentos().get("campos")).hasSize(8);
        assertThat((List<?>) result.acciones().get(1).argumentos().get("campos")).hasSize(7);
    }

    @Test
    @DisplayName("Los ejemplos usan las etiquetas de la pieza y descartan celdas, filas y valores inválidos")
    void agregarPiezaShouldCleanExamples() {
        Map<String, Object> conNulo = new HashMap<>();
        conNulo.put("Producto", null);
        List<Object> ejemplos = new ArrayList<>();
        ejemplos.add("no es una fila");
        ejemplos.add(Map.of("Inventado", "x"));
        ejemplos.add(conNulo);
        ejemplos.add(Map.of("Producto", "x".repeat(81), "Cantidad", List.of(1)));
        // Sin el dato principal (Producto) la fila se vería vacía en la tabla: se cae.
        ejemplos.add(Map.of("cantidad", 3, "Fiado", true, "Inventado", "y"));
        ejemplos.add(Map.of("producto", "Vela lavanda", "cantidad", 2, "Fiado", true, "Inventado", "y"));

        ValidacionTaller result = validar(VACIO, llamada("agregar_pieza", Map.of("tipo", "sales", "ejemplos", ejemplos)));

        assertThat(result.acciones().getFirst().argumentos()).containsEntry("ejemplos",
                List.of(Map.of("Producto", "Vela lavanda", "Cantidad", 2, "Fiado", true)));
    }

    @Test
    @DisplayName("Se queda con las primeras cinco filas de ejemplo y no manda ejemplos si no es una lista")
    void agregarPiezaShouldCapExamples() {
        List<Map<String, Object>> siete = IntStream.rangeClosed(1, 7)
                .mapToObj(numero -> Map.<String, Object>of("Concepto", "Gasto " + numero))
                .toList();

        ValidacionTaller result = validar(VACIO,
                llamada("agregar_pieza", Map.of("tipo", "expenses", "ejemplos", siete)),
                llamada("agregar_pieza", Map.of("tipo", "clients", "ejemplos", "Juan")));

        assertThat((List<?>) result.acciones().get(0).argumentos().get("ejemplos")).hasSize(5);
        assertThat(result.acciones().get(1).argumentos()).doesNotContainKey("ejemplos");
    }

    @Test
    @DisplayName("Reemplaza los ejemplos de una pieza existente o recién sumada")
    void cargarEjemplosShouldReplaceRows() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("cargar_ejemplos", Map.of("pieza", "Stock", "filas", List.of(Map.of("Producto", "Yerba")))),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                llamada("cargar_ejemplos", Map.of("pieza", "turnos", "filas", List.of(Map.of("nombre", "Paula")))));

        assertThat(result.acciones()).containsExactly(
                accion("cargar_ejemplos", Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba")))),
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                accion("cargar_ejemplos", Map.of("pieza", "Turnos", "filas", List.of(Map.of("Nombre", "Paula")))));
    }

    @Test
    @DisplayName("No carga ejemplos sin filas válidas o en una pieza que no existe")
    void cargarEjemplosShouldRejectInvalidInput() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("cargar_ejemplos", Map.of("pieza", "e1", "filas", List.of(Map.of("Color", "Rojo")))),
                llamada("cargar_ejemplos", Map.of("pieza", "e1")),
                llamada("cargar_ejemplos", Map.of("pieza", "nada", "filas", List.of(Map.of("Producto", "x")))));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Conecta dos piezas con la etiqueta pedida o, si no hay, con el singular del componente")
    void conectarShouldUseGivenLabelOrComponentSingular() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "Ventas")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "Stock", "etiqueta", " Producto favorito ")));

        assertThat(result.acciones()).containsExactly(
                accion("agregar_pieza", Map.of("tipo", "sales")),
                accion("conectar", Map.of("desde", "e1", "hacia", "sales", "etiqueta", "Venta")),
                accion("conectar", Map.of("desde", "e2", "hacia", "e1", "etiqueta", "Producto favorito")));
    }

    @Test
    @DisplayName("Sin etiqueta, el vínculo a una tabla a medida se llama como la tabla")
    void conectarShouldUseFormNameAsDefaultLabel() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "Turnos")));

        assertThat(result.acciones().get(1))
                .isEqualTo(accion("conectar", Map.of("desde", "e2", "hacia", "Turnos", "etiqueta", "Turnos")));
    }

    @Test
    @DisplayName("Sin etiqueta y con un tipo que no está en el catálogo, usa el nombre de la pieza")
    void conectarShouldUsePieceNameForUnknownTypes() {
        EstadoTaller conDesconocida = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Stock", null, List.of(new CampoTaller("Producto", "text"))),
                new PiezaTaller("e9", "calendar", "Agenda", null, List.of(new CampoTaller("Día", "date")))));

        ValidacionTaller result = validar(conDesconocida, llamada("conectar", Map.of("desde", "e1", "hacia", "e9")));

        thenOnlyActionIs(result, accion("conectar", Map.of("desde", "e1", "hacia", "e9", "etiqueta", "Agenda")));
    }

    @Test
    @DisplayName("No conecta piezas inexistentes, una pieza consigo misma ni repite una etiqueta")
    void conectarShouldRejectInvalidLinks() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("conectar", Map.of("desde", "nada", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "nada")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "inventory")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2", "etiqueta", "producto")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2")));

        assertThat(result.acciones()).hasSize(1);
        assertThat(result.descartadas()).isEqualTo(5);
    }

    @Test
    @DisplayName("Si la pieza ya tiene un dato común con esa etiqueta, ese dato pasa a ser el vínculo")
    void conectarShouldTurnAnExistingFieldIntoTheLink() {
        Map<String, Object> turnos = Map.of(
                "tipo", "form", "nombre", "Turnos", "campos", List.of(Map.of("etiqueta", "Cliente", "tipo", "text")));
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", turnos),
                llamada("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "cliente")));

        assertThat(result.acciones()).containsExactly(
                accion("agregar_pieza", turnos),
                accion("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "Cliente")));
    }

    @Test
    @DisplayName("No conecta lo que el taller ya vincula solo: ventas y pedidos con clientes, movimientos con inventario")
    void conectarShouldDiscardLinksTheWorkshopMakesByItself() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("agregar_pieza", Map.of("tipo", "orders")),
                llamada("agregar_pieza", Map.of("tipo", "entries")),
                llamada("agregar_pieza", Map.of("tipo", "exits")),
                llamada("conectar", Map.of("desde", "sales", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "sales", "hacia", "e1")),
                llamada("conectar", Map.of("desde", "orders", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "entries", "hacia", "e1")),
                llamada("conectar", Map.of("desde", "exits", "hacia", "e1")),
                llamada("conectar", Map.of("desde", "orders", "hacia", "e1")));

        assertThat(result.acciones()).extracting(AccionTaller::herramienta).containsExactly(
                "agregar_pieza", "agregar_pieza", "agregar_pieza", "agregar_pieza", "conectar");
        assertThat(result.acciones().getLast().argumentos()).containsEntry("desde", "orders").containsEntry("hacia", "e1");
        assertThat(result.descartadas()).isEqualTo(5);
    }

    @Test
    @DisplayName("No conecta piezas sin datos, como el resumen")
    void conectarShouldRejectPiecesWithoutFields() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "summary")),
                llamada("conectar", Map.of("desde", "summary", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "summary")));

        assertThat(result.acciones()).hasSize(1);
        assertThat(result.descartadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("No conecta desde una pieza que ya tiene doce datos")
    void conectarShouldRespectFieldLimit() {
        List<CampoTaller> doce = IntStream.rangeClosed(1, 12).mapToObj(n -> new CampoTaller("Dato " + n, "text")).toList();
        EstadoTaller lleno = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Stock", null, doce),
                new PiezaTaller("e2", "clients", "Clientes", null, List.of())));

        ValidacionTaller result = validar(lleno, llamada("conectar", Map.of("desde", "e1", "hacia", "e2")));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("usar_kit arma las piezas del kit con sus datos y ejemplos y después las conecta")
    void usarKitShouldExpandIntoPiecesAndLinks() {
        ValidacionTaller result = validar(VACIO, llamada("usar_kit", Map.of("rubro", "Velas")));

        assertThat(result.acciones()).extracting(AccionTaller::herramienta).containsExactly(
                "agregar_pieza", "agregar_pieza", "agregar_pieza", "agregar_pieza", "conectar", "editar_campo");
        assertThat(result.acciones()).extracting(accion -> accion.argumentos().get("nombre"))
                .containsExactly("Velas", "Materiales", "Ventas", "Quién me debe", null, null);
        assertThat(result.acciones().get(4).argumentos()).isEqualTo(
                Map.of("desde", "inventory", "hacia", "Materiales", "etiqueta", "Materiales que lleva"));
        // Con clientas en el kit, el dato Cliente de las ventas va al lugar 2 para que se vea.
        assertThat(result.acciones().get(5).argumentos())
                .isEqualTo(Map.of("pieza", "sales", "campo", "Cliente", "posicion", 2));
        assertThat((List<?>) result.acciones().getFirst().argumentos().get("ejemplos")).hasSize(3);
        assertThat(result.descartadas()).isZero();
    }

    @Test
    @DisplayName("usar_kit saltea los tipos que el sistema ya tiene y conecta con las piezas existentes")
    void usarKitShouldSkipExistingTypesAndLinkToExistingPieces() {
        ValidacionTaller result = validar(CON_PIEZAS, llamada("usar_kit", Map.of("rubro", "peluqueria")));

        assertThat(result.acciones()).extracting(accion -> accion.argumentos().get("nombre"))
                .containsExactly("Turnos", "Servicios", null, null);
        assertThat(result.acciones().subList(2, 4)).containsExactly(
                accion("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "Cliente")),
                accion("conectar", Map.of("desde", "Turnos", "hacia", "Servicios", "etiqueta", "Servicio")));
    }

    @Test
    @DisplayName("Un kit desconocido o que no suma nada se descarta")
    void usarKitShouldDiscardUnknownOrUselessKits() {
        ValidacionTaller result = validar(VACIO,
                llamada("usar_kit", Map.of("rubro", "astronautas")),
                llamada("usar_kit", Map.of()),
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("usar_kit", Map.of("rubro", "velas")));

        assertThat(result.acciones()).hasSize(6);
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Si arma con un kit y el modelo no preguntó nada, propone seguir con la pregunta del kit")
    void usarKitShouldAddTheKitQuestionWhenTheModelDidNotAsk() {
        ValidacionTaller result = validar(VACIO, llamada("usar_kit", Map.of("rubro", "velas")));

        assertThat(result.opciones()).isEqualTo(SamiPruebas.kitsReales().buscar("velas").orElseThrow().pregunta());
    }

    @Test
    @DisplayName("Si el modelo preguntó vale su pregunta; un kit desconocido no pregunta y repetir el kit no la borra")
    void usarKitShouldKeepTheModelQuestion() {
        ValidacionTaller conPregunta = validar(VACIO,
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Vendés por pedido?", "opciones", List.of("Sí", "No"))));
        ValidacionTaller dosVeces = validar(CON_PIEZAS, llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("usar_kit", Map.of("rubro", "velas")));

        assertThat(conPregunta.opciones()).isEqualTo(new OpcionesTaller("¿Vendés por pedido?", List.of("Sí", "No")));
        assertThat(validar(VACIO, llamada("usar_kit", Map.of("rubro", "astronautas"))).opciones()).isNull();
        assertThat(dosVeces.opciones()).as("la segunda vez no suma nada, la primera sí").isNotNull();
    }

    @Test
    @DisplayName("Renombrar una pieza a su mismo nombre no es un cambio y se descarta")
    void renombrarPiezaShouldDiscardTheSameName() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Stock")),
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Velas")));

        assertThat(result.acciones()).containsExactly(
                accion("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Velas")));
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("usar_kit cuenta como una sola acción del modelo")
    void usarKitShouldCountAsOneModelAction() {
        List<LlamadaHerramienta> llamadas = new ArrayList<>();
        IntStream.range(0, 11).forEach(i -> llamadas.add(llamada("cambiar_distribucion", Map.of("layout", "two"))));
        llamadas.add(llamada("usar_kit", Map.of("rubro", "velas")));
        llamadas.add(llamada("cambiar_distribucion", Map.of("layout", "single")));

        ValidacionTaller result = herramientasTallerService.validar(llamadas, VACIO);

        assertThat(result.acciones()).hasSize(17);
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Con kits no pasa de veinticuatro acciones por turno")
    void usarKitShouldRespectTotalActionLimit() {
        List<PiezaKit> treinta = IntStream.rangeClosed(1, 30)
                .mapToObj(numero -> new PiezaKit("form", "Tabla " + numero, List.of(), List.of()))
                .toList();
        KitRubro enorme = new KitRubro("enorme", "Enorme", List.of(), treinta,
                List.of(new ConexionKit("Tabla 1", "Tabla 2", "Vínculo")), new OpcionesTaller("¿Y?", List.of("A", "B")));
        HerramientasTallerService conKitEnorme =
                new HerramientasTallerService(SamiPruebas.catalogoReal(), new KitsRubro(List.of(enorme)));

        ValidacionTaller result = conKitEnorme.validar(List.of(
                llamada("usar_kit", Map.of("rubro", "enorme")),
                llamada("cambiar_distribucion", Map.of("layout", "two"))), VACIO);

        assertThat(result.acciones()).hasSize(24);
        assertThat(result.acciones()).extracting(AccionTaller::herramienta).containsOnly("agregar_pieza");
        assertThat(result.descartadas()).isEqualTo(1);
    }

    // --- act ---
    private ValidacionTaller validar(EstadoTaller estado, LlamadaHerramienta... llamadas) {
        return herramientasTallerService.validar(List.of(llamadas), estado);
    }

    // --- assert ---
    private void thenOnlyActionIs(ValidacionTaller result, AccionTaller esperada) {
        assertThat(result.acciones()).containsExactly(esperada);
        assertThat(result.descartadas()).isZero();
    }

    // --- helpers ---
    private LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private AccionTaller accion(String herramienta, Map<String, Object> argumentos) {
        return new AccionTaller(herramienta, argumentos);
    }
}
