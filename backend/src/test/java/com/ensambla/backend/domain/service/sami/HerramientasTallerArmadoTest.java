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

/** Las herramientas para ensamblar: agregar_pieza completa, usar_kit, cargar_ejemplos, cargar_registros y conectar. */
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
    @DisplayName("Suma una pieza con nombre, datos extra y sección, e ignora las filas de ejemplo que inventa el modelo")
    void agregarPiezaShouldAcceptNameFieldsAndSectionAndIgnoreInventedExamples() {
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
    @DisplayName("Las filas que inventa el modelo nunca pasan: ni en agregar_pieza ni con cargar_ejemplos")
    void ejemplosDelModeloShouldNeverPass() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("agregar_pieza", Map.of("tipo", "expenses",
                        "ejemplos", List.of(Map.of("Concepto", "Alquiler")))),
                llamada("cargar_ejemplos", Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba")))));

        assertThat(result.acciones()).containsExactly(accion("agregar_pieza", Map.of("tipo", "expenses")));
        assertThat(result.descartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Los ejemplos del kit del catálogo sí se suman al armar con usar_kit")
    void usarKitShouldKeepTheCatalogExamples() {
        ValidacionTaller result = validar(VACIO, llamada("usar_kit", Map.of("rubro", "velas")));

        assertThat(result.acciones()).anyMatch(accion -> accion.argumentos().containsKey("ejemplos"));
    }

    @Test
    @DisplayName("Carga registros reales en una pieza existente o recién sumada, con las etiquetas de la pieza")
    void cargarRegistrosShouldAcceptRowsForExistingOrNewPiece() {
        ValidacionTaller result = validar(VACIO,
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Ventas de velas", "campos", List.of(
                        Map.of("etiqueta", "Clienta", "tipo", "text"), Map.of("etiqueta", "Precio", "tipo", "currency")))),
                llamada("cargar_registros", Map.of("pieza", "ventas de velas", "filas", List.of(
                        Map.of("nombre", "Vela lavanda", "clienta", "Ana", "Precio", 4500),
                        Map.of("Nombre", "Vela coco", "Clienta", "Luz", "Inventada", "x")))));

        assertThat(result.acciones()).containsExactly(
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Ventas de velas", "campos", List.of(
                        Map.of("etiqueta", "Clienta", "tipo", "text"), Map.of("etiqueta", "Precio", "tipo", "currency")))),
                accion("cargar_registros", Map.of("pieza", "Ventas de velas", "filas", List.of(
                        Map.of("Nombre", "Vela lavanda", "Clienta", "Ana", "Precio", 4500),
                        Map.of("Nombre", "Vela coco", "Clienta", "Luz")))));
    }

    @Test
    @DisplayName("Los registros sin el dato principal se caen y no se cargan más de 30 por llamada")
    void cargarRegistrosShouldDropRowsWithoutMainFieldAndCapAtThirty() {
        List<Map<String, Object>> filas = new ArrayList<>(IntStream.rangeClosed(1, 35)
                .<Map<String, Object>>mapToObj(numero -> Map.of("Producto", "Producto " + numero, "Cantidad", numero))
                .toList());
        filas.addFirst(Map.of("Cantidad", 9));

        ValidacionTaller result = validar(CON_PIEZAS, llamada("cargar_registros", Map.of("pieza", "e1", "filas", filas)));

        List<?> cargadas = (List<?>) result.acciones().getFirst().argumentos().get("filas");
        assertThat(cargadas).hasSize(30);
        assertThat(cargadas.getFirst()).isEqualTo(Map.of("Producto", "Producto 1", "Cantidad", 1));
        assertThat(result.registrosDescartados()).as("la fila sin producto y las 5 pasadas del tope").isEqualTo(6);
    }

    @Test
    @DisplayName("No carga registros sin filas válidas o en una pieza que no existe")
    void cargarRegistrosShouldRejectInvalidInput() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(Map.of("Cantidad", 3)))),
                llamada("cargar_registros", Map.of("pieza", "e1")),
                llamada("cargar_registros", Map.of("pieza", "nada", "filas", List.of(Map.of("Producto", "x")))));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(3);
        assertThat(result.registrosDescartados()).isEqualTo(2);
    }

    @Test
    @DisplayName("Borra registros por los datos que coinciden: un criterio o una lista, con las etiquetas de la pieza")
    void borrarRegistrosShouldAcceptOneOrManyCriteria() {
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("borrar_registros", Map.of("pieza", "Stock", "donde", Map.of("producto", " Yerba ", "cantidad", 3))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Fideos"), Map.of("Producto", "Aceite")))));

        assertThat(result.acciones()).containsExactly(
                accion("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Yerba", "Cantidad", 3)))),
                accion("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Fideos"), Map.of("Producto", "Aceite")))));
    }

    @Test
    @DisplayName("Nunca borra de más: sin criterio, con un dato que la pieza no tiene o con un valor no simple se descarta")
    void borrarRegistrosShouldNeverWidenTheCriterion() {
        Map<String, Object> vacio = new HashMap<>();
        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("borrar_registros", Map.of("pieza", "e1")),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", vacio)),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Color", "Rojo"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba", "Color", "Rojo"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", List.of("x")))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", "todo")),
                llamada("borrar_registros", Map.of("pieza", "nada", "donde", Map.of("Producto", "Yerba"))));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(7);
    }

    @Test
    @DisplayName("Si un criterio no vale pero otro sí, sólo se borra con el válido; y no más de diez criterios")
    void borrarRegistrosShouldKeepOnlyValidCriteriaUpToTen() {
        List<Map<String, Object>> criterios = new ArrayList<>(IntStream.rangeClosed(1, 12)
                .<Map<String, Object>>mapToObj(numero -> Map.of("Producto", "P" + numero)).toList());
        criterios.addFirst(Map.of("Color", "Rojo"));

        ValidacionTaller result = validar(CON_PIEZAS,
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", criterios)));

        assertThat((List<?>) result.acciones().getFirst().argumentos().get("donde")).hasSize(10)
                .first().isEqualTo(Map.of("Producto", "P1"));
    }

    @Test
    @DisplayName("Varias dudas juntas: llegan en el orden pedido con su tema, hasta tres, sin repetir y sumando llamadas")
    void ofrecerOpcionesShouldCollectUpToThreeQuestions() {
        ValidacionTaller result = validar(VACIO,
                llamada("ofrecer_opciones", Map.of("preguntas", List.of(
                        Map.of("tema", " Tildes ", "pregunta", "¿Qué significan los tildes?", "opciones", List.of("Entregado", "Pagado")),
                        Map.of("pregunta", "¿Costo o deuda?", "opciones", List.of("Costo", "Deuda")),
                        Map.of("pregunta", "¿Sin opciones?", "opciones", List.of("Una sola")),
                        "no es una duda"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Costo o deuda?", "opciones", List.of("Costo", "Deuda"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál total?", "opciones", List.of("37.000", "28.800"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Una cuarta?", "opciones", List.of("Sí", "No"))));

        assertThat(result.preguntas()).extracting("pregunta")
                .containsExactly("¿Qué significan los tildes?", "¿Costo o deuda?", "¿Cuál total?");
        assertThat(result.preguntas().getFirst().tema()).isEqualTo("Tildes");
        assertThat(result.preguntas().get(1).tema()).isNull();
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

        assertThat(result.preguntas())
                .containsExactly(SamiPruebas.kitsReales().buscar("velas").orElseThrow().pregunta());
    }

    @Test
    @DisplayName("Si el modelo preguntó vale su pregunta; un kit desconocido no pregunta y repetir el kit no la borra")
    void usarKitShouldKeepTheModelQuestion() {
        ValidacionTaller conPregunta = validar(VACIO,
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Vendés por pedido?", "opciones", List.of("Sí", "No"))));
        ValidacionTaller dosVeces = validar(CON_PIEZAS, llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("usar_kit", Map.of("rubro", "velas")));

        assertThat(conPregunta.preguntas()).containsExactly(new OpcionesTaller("¿Vendés por pedido?", List.of("Sí", "No")));
        assertThat(validar(VACIO, llamada("usar_kit", Map.of("rubro", "astronautas"))).preguntas()).isEmpty();
        assertThat(dosVeces.preguntas()).as("la segunda vez no suma nada, la primera sí").hasSize(1);
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
