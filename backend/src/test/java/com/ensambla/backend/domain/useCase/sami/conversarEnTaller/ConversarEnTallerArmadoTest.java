package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

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
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Las herramientas para ensamblar: agregar_pieza completa, usar_kit, cargar_ejemplos, los registros y conectar. */
class ConversarEnTallerArmadoTest {

    private static final EstadoTaller VACIO = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi negocio", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Stock", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"))),
            new PiezaTaller("e2", "clients", "Clientes", null, List.of(new CampoTaller("Nombre", "text")))));
    private static final EstadoTaller CON_REGISTROS = new EstadoTaller("Mi negocio", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Stock", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number")), List.of(
                    new RegistroTaller("r1", Map.of("Producto", "Yerba", "Cantidad", 3)),
                    new RegistroTaller("r2", Map.of("Producto", "Fideos", "Cantidad", 1))))));

    private TallerDePrueba taller;
    private EstadoTaller estado = VACIO;
    private List<LlamadaHerramienta> llamadas = List.of();
    private ValidacionTaller validacion;

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba();
    }

    @Test
    @DisplayName("Suma una pieza con nombre, datos extra y sección, e ignora las filas de ejemplo que inventa el modelo")
    void sumaUnaPiezaConNombreDatosExtraYSeccionEIgnoraLasFilasDeEjemploQueInventaElModelo() {
        dadoQueSePideUnaPiezaConNombreDatosExtraSeccionYEjemplosInventados();

        cuandoSeValidanLasLlamadas();

        entoncesLaUnicaAccionEs(accion("agregar_pieza", Map.of(
                "tipo", "inventory",
                "nombre", "Velas",
                "campos", List.of(Map.of("etiqueta", "Aroma", "tipo", "text"),
                        Map.of("etiqueta", "Cera", "tipo", "select", "opciones", List.of("Soja", "Coco"))),
                "seccion", "Taller")));
    }

    @Test
    @DisplayName("Suma componentes sin datos, como el resumen, sin datos extra ni ejemplos")
    void sumaComponentesSinDatosComoElResumenSinDatosExtraNiEjemplos() {
        dadoLasLlamadas(llamada("agregar_pieza", Map.of(
                "tipo", "summary",
                "campos", List.of(Map.of("etiqueta", "Total", "tipo", "number")),
                "ejemplos", List.of(Map.of("Total", 3)))));

        cuandoSeValidanLasLlamadas();

        entoncesLaUnicaAccionEs(accion("agregar_pieza", Map.of("tipo", "summary")));
    }

    @Test
    @DisplayName("No suma una pieza cuyo nombre de catálogo ya usa otra pieza")
    void noSumaUnaPiezaCuyoNombreDeCatalogoYaUsaOtraPieza() {
        dadoUnaTablaLlamadaVentasYDosPedidosDePiezaDeVentas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptan(accion("agregar_pieza", Map.of("tipo", "sales", "nombre", "Caja")));
    }

    @Test
    @DisplayName("La tabla a medida exige nombre, se puede repetir y no repite un nombre que ya existe")
    void laTablaAMedidaExigeNombreSePuedeRepetirYNoRepiteUnNombreQueYaExiste() {
        dadoQueSePidenTablasAMedidaConYSinNombreYConNombresRepetidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptan(
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Trabajos")));
    }

    @Test
    @DisplayName("Descarta los datos extra inválidos y se queda con los válidos")
    void descartaLosDatosExtraInvalidosYSeQuedaConLosValidos() {
        dadoUnaPiezaConDatosExtraValidosEInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSoloQuedaElDatoExtraMarca();
    }

    @Test
    @DisplayName("No pasa de ocho datos extra ni de doce datos por pieza")
    void noPasaDeOchoDatosExtraNiDeDoceDatosPorPieza() {
        dadoDosPiezasConNueveDatosExtraCadaUna();

        cuandoSeValidanLasLlamadas();

        entoncesLaTablaAMedidaTieneOchoDatosYElInventarioSiete();
    }

    @Test
    @DisplayName("Las filas que inventa el modelo nunca pasan: ni en agregar_pieza ni con cargar_ejemplos")
    void lasFilasQueInventaElModeloNuncaPasanNiEnAgregarPiezaNiConCargarEjemplos() {
        dadoQueElModeloInventaFilasDeEjemplo();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptan(accion("agregar_pieza", Map.of("tipo", "expenses")));
    }

    @Test
    @DisplayName("Los ejemplos del kit del catálogo sí se suman al armar con usar_kit")
    void losEjemplosDelKitDelCatalogoSiSeSumanAlArmarConUsarKit() {
        dadoLasLlamadas(llamada("usar_kit", Map.of("rubro", "velas")));

        cuandoSeValidanLasLlamadas();

        entoncesAlgunaAccionTraeEjemplos();
    }

    @Test
    @DisplayName("Carga registros reales en una pieza existente o recién sumada, con las etiquetas de la pieza")
    void cargaRegistrosRealesEnUnaPiezaExistenteORecienSumadaConLasEtiquetasDeLaPieza() {
        dadoUnaPiezaNuevaDeVentasDeVelasYSeLeCarganFilas();

        cuandoSeValidanLasLlamadas();

        entoncesSeSumaLaPiezaYSeCarganLasFilasConLasEtiquetasDeLaPieza();
    }

    @Test
    @DisplayName("Los registros sin el dato principal se caen y no se cargan más de 30 por llamada")
    void losRegistrosSinElDatoPrincipalSeCaenYNoSeCarganMasDe30PorLlamada() {
        dadoTreintaYCincoFilasMasUnaSinElProducto();

        cuandoSeValidanLasLlamadas();

        entoncesSeCarganTreintaFilasYSeDescartanSeis();
    }

    @Test
    @DisplayName("No carga registros sin filas válidas o en una pieza que no existe")
    void noCargaRegistrosSinFilasValidasOEnUnaPiezaQueNoExiste() {
        dadoQueSeCarganFilasInvalidasOEnUnaPiezaInexistente();

        cuandoSeValidanLasLlamadas();

        entoncesNoSeCargaNadaYSeDescartanDosFilas();
    }

    @Test
    @DisplayName("Corrige registros cargados por su id, sólo con los datos que cambian y las etiquetas de la pieza")
    void corrigeRegistrosCargadosPorSuIdSoloConLosDatosQueCambianYLasEtiquetasDeLaPieza() {
        dadoCorreccionesDeRegistrosConocidosYErroneos();

        cuandoSeValidanLasLlamadas();

        entoncesSoloSeCorrigenLosCambiosReales();
    }

    @Test
    @DisplayName("Sin cambios válidos no corrige nada, y una corrección ya hecha en el turno no se repite")
    void sinCambiosValidosNoCorrigeNadaYUnaCorreccionYaHechaEnElTurnoNoSeRepite() {
        dadoCorreccionesSinCambiosYUnaCorreccionRepetida();

        cuandoSeValidanLasLlamadas();

        entoncesLaUnicaAccionEs(accion("editar_registros", correccionDeCantidadACinco()));
    }

    @Test
    @DisplayName("Borra registros por su id: sólo los que la pieza tiene, sin repetir, y junto con un criterio")
    void borraRegistrosPorSuIdSoloLosQueLaPiezaTieneSinRepetirYJuntoConUnCriterio() {
        dadoQueSeBorranRegistrosPorIdConIdsConocidosYDesconocidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeBorranSoloLosRegistrosConocidos();
    }

    @Test
    @DisplayName("Los registros de una pieza siguen ahí después de renombrarla, sumarle un dato o pasarla de sección")
    void losRegistrosDeUnaPiezaSiguenAhiDespuesDeRenombrarlaSumarleUnDatoOPasarlaDeSeccion() {
        dadoQueSeRenombraSeAmpliaYSeSeparaUnaPiezaConRegistrosYLuegoSeCorrigeUno();

        cuandoSeValidanLasLlamadas();

        entoncesLaCorreccionDelRegistroSigueValiendo();
    }

    @Test
    @DisplayName("Borra registros por los datos que coinciden: un criterio o una lista, con las etiquetas de la pieza")
    void borraRegistrosPorLosDatosQueCoincidenUnCriterioOUnaListaConLasEtiquetasDeLaPieza() {
        dadoQueSeBorranRegistrosPorUnCriterioYPorUnaLista();

        cuandoSeValidanLasLlamadas();

        entoncesSeBorranPorCriterioConLasEtiquetasDeLaPieza();
    }

    @Test
    @DisplayName("Nunca borra de más: sin criterio, con un dato que la pieza no tiene o con un valor no simple se descarta")
    void nuncaBorraDeMasSinCriterioConUnDatoQueLaPiezaNoTieneOConUnValorNoSimpleSeDescarta() {
        dadoQueSePideBorrarConCriteriosVaciosOInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Si un criterio no vale pero otro sí, sólo se borra con el válido; y no más de diez criterios")
    void siUnCriterioNoValePeroOtroSiSoloSeBorraConElValidoYNoMasDeDiezCriterios() {
        dadoUnCriterioInvalidoSeguidoDeDoceValidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeUsanSoloDiezCriteriosValidos();
    }

    @Test
    @DisplayName("Varias dudas juntas: llegan en el orden pedido con su tema, hasta tres, sin repetir y sumando llamadas")
    void variasDudasJuntasLleganEnElOrdenPedidoConSuTemaHastaTresSinRepetirYSumandoLlamadas() {
        dadoVariasDudasEnUnaLlamadaYEnVariasLlamadas();

        cuandoSeValidanLasLlamadas();

        entoncesLlegaUnaBotoneraDeTresDudasEnOrdenConSuTema();
    }

    @Test
    @DisplayName("Conecta dos piezas con la etiqueta pedida o, si no hay, con el singular del componente")
    void conectaDosPiezasConLaEtiquetaPedidaOSiNoHayConElSingularDelComponente() {
        dadoQueSeConectaStockConVentasYClientesConStock();

        cuandoSeValidanLasLlamadas();

        entoncesLosVinculosUsanLaEtiquetaPedidaOElSingularDelComponente();
    }

    @Test
    @DisplayName("Sin etiqueta, el vínculo a una tabla a medida se llama como la tabla")
    void sinEtiquetaElVinculoAUnaTablaAMedidaSeLlamaComoLaTabla() {
        dadoQueSeConectaConUnaTablaAMedidaLlamadaTurnos();

        cuandoSeValidanLasLlamadas();

        entoncesElVinculoSeLlamaTurnos();
    }

    @Test
    @DisplayName("Sin etiqueta y con un tipo que no está en el catálogo, usa el nombre de la pieza")
    void sinEtiquetaYConUnTipoQueNoEstaEnElCatalogoUsaElNombreDeLaPieza() {
        dadoQueSeConectaConUnaPiezaDeTipoDesconocidoLlamadaAgenda();

        cuandoSeValidanLasLlamadas();

        entoncesElVinculoSeLlamaAgenda();
    }

    @Test
    @DisplayName("No conecta piezas inexistentes, una pieza consigo misma ni repite una etiqueta")
    void noConectaPiezasInexistentesUnaPiezaConsigoMismaNiRepiteUnaEtiqueta() {
        dadoQueSePidenVinculosInvalidosYUnoValidoRepetido();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptaUnaSolaAccion();
    }

    @Test
    @DisplayName("Si la pieza ya tiene un dato común con esa etiqueta, ese dato pasa a ser el vínculo")
    void siLaPiezaYaTieneUnDatoComunConEsaEtiquetaEseDatoPasaASerElVinculo() {
        dadoUnaTablaDeTurnosConUnDatoClienteYSePideConectarlaConClientes();

        cuandoSeValidanLasLlamadas();

        entoncesElDatoClienteSeConvierteEnElVinculo();
    }

    @Test
    @DisplayName("No conecta lo que el taller ya vincula solo: ventas y pedidos con clientes, movimientos con inventario")
    void noConectaLoQueElTallerYaVinculaSoloVentasYPedidosConClientesMovimientosConInventario() {
        dadoQueSePidenVinculosQueElTallerYaHaceSoloYUnoDePedidosConInventario();

        cuandoSeValidanLasLlamadas();

        entoncesSoloSeConectanPedidosConInventario();
    }

    @Test
    @DisplayName("No conecta piezas sin datos, como el resumen")
    void noConectaPiezasSinDatosComoElResumen() {
        dadoQueSeSumaUnResumenYSePideConectarloEnAmbosSentidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptaUnaSolaAccion();
    }

    @Test
    @DisplayName("No conecta desde una pieza que ya tiene doce datos")
    void noConectaDesdeUnaPiezaQueYaTieneDoceDatos() {
        dadoUnaPiezaConDoceDatosYSePideConectarla();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("usar_kit arma las piezas del kit con sus datos y ejemplos y después las conecta")
    void usarKitArmaLasPiezasDelKitConSusDatosYEjemplosYDespuesLasConecta() {
        dadoLasLlamadas(llamada("usar_kit", Map.of("rubro", "Velas")));

        cuandoSeValidanLasLlamadas();

        entoncesElKitArmaCuatroPiezasUnVinculoYSuPosicion();
    }

    @Test
    @DisplayName("usar_kit saltea los tipos que el sistema ya tiene y conecta con las piezas existentes")
    void usarKitSalteaLosTiposQueElSistemaYaTieneYConectaConLasPiezasExistentes() {
        dadoUnSistemaConStockYClientesYSePideElKitDePeluqueria();

        cuandoSeValidanLasLlamadas();

        entoncesElKitSoloSumaTurnosYServiciosYLosConectaConLasPiezasExistentes();
    }

    @Test
    @DisplayName("Un kit desconocido o que no suma nada se descarta")
    void unKitDesconocidoOQueNoSumaNadaSeDescarta() {
        dadoQueSePidenKitsDesconocidosYUnKitDeVelasDosVeces();

        cuandoSeValidanLasLlamadas();

        entoncesSoloSeAplicaUnaVezElKitDeVelas();
    }

    @Test
    @DisplayName("Si arma con un kit y el modelo no preguntó nada, propone seguir con la pregunta del kit")
    void siArmaConUnKitYElModeloNoPreguntoNadaProponeSeguirConLaPreguntaDelKit() {
        dadoLasLlamadas(llamada("usar_kit", Map.of("rubro", "velas")));

        cuandoSeValidanLasLlamadas();

        entoncesSeProponeLaPreguntaDelKitDeVelas();
    }

    @Test
    @DisplayName("Si el modelo preguntó, vale su pregunta y no la del kit")
    void siElModeloPreguntoValeSuPreguntaYNoLaDelKit() {
        dadoUnKitDeVelasYUnaPreguntaDelModelo();

        cuandoSeValidanLasLlamadas();

        entoncesSoloValeLaPreguntaDelModelo();
    }

    @Test
    @DisplayName("Un kit desconocido no pregunta")
    void unKitDesconocidoNoPregunta() {
        dadoLasLlamadas(llamada("usar_kit", Map.of("rubro", "astronautas")));

        cuandoSeValidanLasLlamadas();

        entoncesNoHayPreguntas();
    }

    @Test
    @DisplayName("Repetir el kit no borra la pregunta que ya se propuso")
    void repetirElKitNoBorraLaPreguntaQueYaSePropuso() {
        dadoElKitDeVelasPedidoDosVecesSobreUnSistemaConPiezas();

        cuandoSeValidanLasLlamadas();

        entoncesLaPreguntaDelKitSeProponeUnaVez();
    }

    @Test
    @DisplayName("Renombrar una pieza a su mismo nombre no es un cambio y se descarta")
    void renombrarUnaPiezaASuMismoNombreNoEsUnCambioYSeDescarta() {
        dadoQueSeRenombraLaPiezaStockComoStockYComoVelas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptan(accion("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Velas")));
    }

    @Test
    @DisplayName("usar_kit cuenta como una sola acción del modelo")
    void usarKitCuentaComoUnaSolaAccionDelModelo() {
        dadoOnceCambiosDeDistribucionUnKitYUnCambioMas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAplicanDiecisieteAcciones();
    }

    @Test
    @DisplayName("Con kits no pasa de veinticuatro acciones por turno")
    void conKitsNoPasaDeVeinticuatroAccionesPorTurno() {
        dadoUnKitEnormeDeTreintaPiezasYUnCambioDeDistribucion();

        cuandoSeValidanLasLlamadas();

        entoncesSeAplicanVeinticuatroPiezasNadaMas();
    }

    // --- dado ---

    private void dadoQueSePideUnaPiezaConNombreDatosExtraSeccionYEjemplosInventados() {
        dadoLasLlamadas(llamada("agregar_pieza", Map.of(
                "tipo", "inventory",
                "nombre", " Velas ",
                "campos", List.of(Map.of("etiqueta", "Aroma", "tipo", "text"),
                        Map.of("etiqueta", "Cera", "tipo", "SELECT", "opciones", List.of("Soja", "Coco"))),
                "ejemplos", List.of(Map.of("producto", "Vela lavanda", "Cantidad", 8, "Aroma", "Lavanda")),
                "seccion", "Taller")));
    }

    private void dadoUnaTablaLlamadaVentasYDosPedidosDePiezaDeVentas() {
        estado = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "form", "Ventas", null, List.of(new CampoTaller("Nombre", "text")))));
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("agregar_pieza", Map.of("tipo", "sales", "nombre", "Caja")));
    }

    private void dadoQueSePidenTablasAMedidaConYSinNombreYConNombresRepetidos() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "form")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Trabajos")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "turno")),
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Clientes")));
    }

    private void dadoUnaPiezaConDatosExtraValidosEInvalidos() {
        List<Object> campos = new ArrayList<>();
        campos.add("no es un objeto");
        campos.add(Map.of("tipo", "text"));
        campos.add(Map.of("etiqueta", "Color", "tipo", "color"));
        campos.add(Map.of("etiqueta", "Cliente", "tipo", "relation"));
        campos.add(Map.of("etiqueta", "Talle", "tipo", "select", "opciones", List.of("Único")));
        campos.add(Map.of("etiqueta", "precio", "tipo", "currency"));
        campos.add(Map.of("etiqueta", "Marca", "tipo", "text"));
        campos.add(Map.of("etiqueta", "MARCA", "tipo", "text"));
        dadoLasLlamadas(llamada("agregar_pieza", Map.of("tipo", "inventory", "campos", campos)));
    }

    private void dadoDosPiezasConNueveDatosExtraCadaUna() {
        List<Map<String, Object>> nueve = IntStream.rangeClosed(1, 9)
                .mapToObj(numero -> Map.<String, Object>of("etiqueta", "Dato " + numero, "tipo", "text"))
                .toList();
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Recetas", "campos", nueve)),
                llamada("agregar_pieza", Map.of("tipo", "inventory", "campos", nueve)));
    }

    private void dadoQueElModeloInventaFilasDeEjemplo() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "expenses",
                        "ejemplos", List.of(Map.of("Concepto", "Alquiler")))),
                llamada("cargar_ejemplos", Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba")))));
    }

    private void dadoUnaPiezaNuevaDeVentasDeVelasYSeLeCarganFilas() {
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Ventas de velas", "campos", List.of(
                        Map.of("etiqueta", "Clienta", "tipo", "text"), Map.of("etiqueta", "Precio", "tipo", "currency")))),
                llamada("cargar_registros", Map.of("pieza", "ventas de velas", "filas", List.of(
                        Map.of("nombre", "Vela lavanda", "clienta", "Ana", "Precio", 4500),
                        Map.of("Nombre", "Vela coco", "Clienta", "Luz", "Inventada", "x")))));
    }

    private void dadoTreintaYCincoFilasMasUnaSinElProducto() {
        List<Map<String, Object>> filas = new ArrayList<>(IntStream.rangeClosed(1, 35)
                .<Map<String, Object>>mapToObj(numero -> Map.of("Producto", "Producto " + numero, "Cantidad", numero))
                .toList());
        filas.addFirst(Map.of("Cantidad", 9));
        estado = CON_PIEZAS;
        dadoLasLlamadas(llamada("cargar_registros", Map.of("pieza", "e1", "filas", filas)));
    }

    private void dadoQueSeCarganFilasInvalidasOEnUnaPiezaInexistente() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(Map.of("Cantidad", 3)))),
                llamada("cargar_registros", Map.of("pieza", "e1")),
                llamada("cargar_registros", Map.of("pieza", "nada", "filas", List.of(Map.of("Producto", "x")))));
    }

    private void dadoCorreccionesDeRegistrosConocidosYErroneos() {
        estado = CON_REGISTROS;
        dadoLasLlamadas(llamada("editar_registros", Map.of("pieza", "Stock",
                "registros", List.of(
                        Map.of("id", "r1", "datos", Map.of("cantidad", 5, "Producto", "Yerba", "Color", "Rojo")),
                        Map.of("id", "r2", "datos", Map.of("Cantidad", 1.0)),
                        Map.of("id", "r9", "datos", Map.of("Cantidad", 7)),
                        Map.of("datos", Map.of("Cantidad", 7)),
                        Map.of("id", "r1", "datos", Map.of("Producto", "Yerba mate")),
                        Map.of("id", "r2", "datos", Map.of("Producto", "Fideos", "Cantidad", "dos")),
                        "nada"))));
    }

    private void dadoCorreccionesSinCambiosYUnaCorreccionRepetida() {
        estado = CON_REGISTROS;
        dadoLasLlamadas(
                llamada("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                        Map.of("id", "r1", "datos", "Cantidad 5"),
                        Map.of("id", "r1", "datos", Map.of("Cantidad", 3))))),
                llamada("editar_registros", Map.of("pieza", "e1")),
                llamada("editar_registros", Map.of("pieza", "nada", "registros", List.of(
                        Map.of("id", "r1", "datos", Map.of("Cantidad", 9))))),
                llamada("editar_registros", correccionDeCantidadACinco()),
                llamada("editar_registros", correccionDeCantidadACinco()));
    }

    private void dadoQueSeBorranRegistrosPorIdConIdsConocidosYDesconocidos() {
        estado = CON_REGISTROS;
        dadoLasLlamadas(
                llamada("borrar_registros", Map.of("pieza", "e1", "ids", List.of("r2", "r9", "r2", 3))),
                llamada("borrar_registros", Map.of("pieza", "e1", "ids", "r1", "donde", Map.of("Producto", "Té"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "ids", List.of("r9"))),
                llamada("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                        Map.of("id", "r2", "datos", Map.of("Cantidad", 4))))));
    }

    private void dadoQueSeRenombraSeAmpliaYSeSeparaUnaPiezaConRegistrosYLuegoSeCorrigeUno() {
        estado = CON_REGISTROS;
        dadoLasLlamadas(
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Depósito")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Marca", "tipo", "text")),
                llamada("separar_en_seccion", Map.of("pieza", "e1", "seccion", "Stock")),
                llamada("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                        Map.of("id", "r1", "datos", Map.of("Marca", "Playadito"))))));
    }

    private void dadoQueSeBorranRegistrosPorUnCriterioYPorUnaLista() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("borrar_registros", Map.of("pieza", "Stock", "donde", Map.of("producto", " Yerba ", "cantidad", 3))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Fideos"), Map.of("Producto", "Aceite")))));
    }

    private void dadoQueSePideBorrarConCriteriosVaciosOInvalidos() {
        Map<String, Object> vacio = new HashMap<>();
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("borrar_registros", Map.of("pieza", "e1")),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", vacio)),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Color", "Rojo"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba", "Color", "Rojo"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", List.of("x")))),
                llamada("borrar_registros", Map.of("pieza", "e1", "donde", "todo")),
                llamada("borrar_registros", Map.of("pieza", "nada", "donde", Map.of("Producto", "Yerba"))));
    }

    private void dadoUnCriterioInvalidoSeguidoDeDoceValidos() {
        List<Map<String, Object>> criterios = new ArrayList<>(IntStream.rangeClosed(1, 12)
                .<Map<String, Object>>mapToObj(numero -> Map.of("Producto", "P" + numero)).toList());
        criterios.addFirst(Map.of("Color", "Rojo"));
        estado = CON_PIEZAS;
        dadoLasLlamadas(llamada("borrar_registros", Map.of("pieza", "e1", "donde", criterios)));
    }

    private void dadoVariasDudasEnUnaLlamadaYEnVariasLlamadas() {
        dadoLasLlamadas(
                llamada("ofrecer_opciones", Map.of("preguntas", List.of(
                        Map.of("tema", " Tildes ", "pregunta", "¿Qué significan los tildes?",
                                "opciones", List.of("Entregado", "Pagado")),
                        Map.of("pregunta", "¿Costo o deuda?", "opciones", List.of("Costo", "Deuda")),
                        Map.of("pregunta", "¿Sin opciones?", "opciones", List.of("Una sola")),
                        "no es una duda"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Costo o deuda?", "opciones", List.of("Costo", "Deuda"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Cuál total?", "opciones", List.of("37.000", "28.800"))),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Una cuarta?", "opciones", List.of("Sí", "No"))));
    }

    private void dadoQueSeConectaStockConVentasYClientesConStock() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "sales")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "Ventas")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "Stock", "etiqueta", " Producto favorito ")));
    }

    private void dadoQueSeConectaConUnaTablaAMedidaLlamadaTurnos() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "form", "nombre", "Turnos")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "Turnos")));
    }

    private void dadoQueSeConectaConUnaPiezaDeTipoDesconocidoLlamadaAgenda() {
        estado = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Stock", null, List.of(new CampoTaller("Producto", "text"))),
                new PiezaTaller("e9", "calendar", "Agenda", null, List.of(new CampoTaller("Día", "date")))));
        dadoLasLlamadas(llamada("conectar", Map.of("desde", "e1", "hacia", "e9")));
    }

    private void dadoQueSePidenVinculosInvalidosYUnoValidoRepetido() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("conectar", Map.of("desde", "nada", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "nada")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "inventory")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2", "etiqueta", "producto")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e1", "hacia", "e2")));
    }

    private void dadoUnaTablaDeTurnosConUnDatoClienteYSePideConectarlaConClientes() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", turnosConUnDatoCliente()),
                llamada("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "cliente")));
    }

    private void dadoQueSePidenVinculosQueElTallerYaHaceSoloYUnoDePedidosConInventario() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
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
    }

    private void dadoQueSeSumaUnResumenYSePideConectarloEnAmbosSentidos() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "summary")),
                llamada("conectar", Map.of("desde", "summary", "hacia", "e2")),
                llamada("conectar", Map.of("desde", "e2", "hacia", "summary")));
    }

    private void dadoUnaPiezaConDoceDatosYSePideConectarla() {
        List<CampoTaller> doce = IntStream.rangeClosed(1, 12)
                .mapToObj(numero -> new CampoTaller("Dato " + numero, "text")).toList();
        estado = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Stock", null, doce),
                new PiezaTaller("e2", "clients", "Clientes", null, List.of())));
        dadoLasLlamadas(llamada("conectar", Map.of("desde", "e1", "hacia", "e2")));
    }

    private void dadoUnSistemaConStockYClientesYSePideElKitDePeluqueria() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(llamada("usar_kit", Map.of("rubro", "peluqueria")));
    }

    private void dadoQueSePidenKitsDesconocidosYUnKitDeVelasDosVeces() {
        dadoLasLlamadas(
                llamada("usar_kit", Map.of("rubro", "astronautas")),
                llamada("usar_kit", Map.of()),
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("usar_kit", Map.of("rubro", "velas")));
    }

    private void dadoUnKitDeVelasYUnaPreguntaDelModelo() {
        dadoLasLlamadas(
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("ofrecer_opciones", Map.of("pregunta", "¿Vendés por pedido?", "opciones", List.of("Sí", "No"))));
    }

    private void dadoElKitDeVelasPedidoDosVecesSobreUnSistemaConPiezas() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("usar_kit", Map.of("rubro", "velas")),
                llamada("usar_kit", Map.of("rubro", "velas")));
    }

    private void dadoQueSeRenombraLaPiezaStockComoStockYComoVelas() {
        estado = CON_PIEZAS;
        dadoLasLlamadas(
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Stock")),
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Velas")));
    }

    private void dadoOnceCambiosDeDistribucionUnKitYUnCambioMas() {
        List<LlamadaHerramienta> pedidas = new ArrayList<>(
                Collections.nCopies(11, llamada("cambiar_distribucion", Map.of("layout", "two"))));
        pedidas.add(llamada("usar_kit", Map.of("rubro", "velas")));
        pedidas.add(llamada("cambiar_distribucion", Map.of("layout", "single")));
        llamadas = pedidas;
    }

    private void dadoUnKitEnormeDeTreintaPiezasYUnCambioDeDistribucion() {
        List<PiezaKit> treinta = IntStream.rangeClosed(1, 30)
                .mapToObj(numero -> new PiezaKit("form", "Tabla " + numero, List.of(), List.of()))
                .toList();
        KitRubro enorme = new KitRubro("enorme", "Enorme", List.of(), treinta,
                List.of(new ConexionKit("Tabla 1", "Tabla 2", "Vínculo")), new OpcionesTaller("¿Y?", List.of("A", "B")));
        taller = new TallerDePrueba(new KitsRubro(List.of(enorme)));
        dadoLasLlamadas(
                llamada("usar_kit", Map.of("rubro", "enorme")),
                llamada("cambiar_distribucion", Map.of("layout", "two")));
    }

    private void dadoLasLlamadas(LlamadaHerramienta... pedidas) {
        llamadas = List.of(pedidas);
    }

    // --- cuando ---

    private void cuandoSeValidanLasLlamadas() {
        validacion = taller.validar(llamadas, estado);
    }

    // --- entonces ---

    private void entoncesLaUnicaAccionEs(AccionTaller esperada) {
        assertThat(validacion.acciones()).containsExactly(esperada);
    }

    private void entoncesSeAceptan(AccionTaller... esperadas) {
        assertThat(validacion.acciones()).containsExactly(esperadas);
    }

    private void entoncesSeDescartaTodo() {
        assertThat(validacion.acciones()).isEmpty();
    }

    private void entoncesSeAceptaUnaSolaAccion() {
        assertThat(validacion.acciones()).hasSize(1);
    }

    private void entoncesSoloQuedaElDatoExtraMarca() {
        assertThat(validacion.acciones().getFirst().argumentos())
                .containsEntry("campos", List.of(Map.of("etiqueta", "Marca", "tipo", "text")));
    }

    private void entoncesLaTablaAMedidaTieneOchoDatosYElInventarioSiete() {
        assertThat((List<?>) validacion.acciones().get(0).argumentos().get("campos")).hasSize(8);
        assertThat((List<?>) validacion.acciones().get(1).argumentos().get("campos")).hasSize(7);
    }

    private void entoncesAlgunaAccionTraeEjemplos() {
        assertThat(validacion.acciones()).anyMatch(accion -> accion.argumentos().containsKey("ejemplos"));
    }

    private void entoncesSeSumaLaPiezaYSeCarganLasFilasConLasEtiquetasDeLaPieza() {
        entoncesSeAceptan(
                accion("agregar_pieza", Map.of("tipo", "form", "nombre", "Ventas de velas", "campos", List.of(
                        Map.of("etiqueta", "Clienta", "tipo", "text"), Map.of("etiqueta", "Precio", "tipo", "currency")))),
                accion("cargar_registros", Map.of("pieza", "Ventas de velas", "filas", List.of(
                        Map.of("Nombre", "Vela lavanda", "Clienta", "Ana", "Precio", 4500),
                        Map.of("Nombre", "Vela coco", "Clienta", "Luz")))));
    }

    private void entoncesSeCarganTreintaFilasYSeDescartanSeis() {
        List<?> cargadas = (List<?>) validacion.acciones().getFirst().argumentos().get("filas");
        assertThat(cargadas).hasSize(30);
        assertThat(cargadas.getFirst()).isEqualTo(Map.of("Producto", "Producto 1", "Cantidad", 1));
        assertThat(validacion.registrosDescartados()).as("la fila sin producto y las 5 pasadas del tope").isEqualTo(6);
    }

    private void entoncesNoSeCargaNadaYSeDescartanDosFilas() {
        assertThat(validacion.acciones()).isEmpty();
        assertThat(validacion.registrosDescartados()).isEqualTo(2);
    }

    private void entoncesSoloSeCorrigenLosCambiosReales() {
        entoncesLaUnicaAccionEs(accion("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                Map.of("id", "r1", "datos", Map.of("Cantidad", 5, "Producto", "Yerba mate")),
                Map.of("id", "r2", "datos", Map.of("Cantidad", "dos"))))));
    }

    private void entoncesSeBorranSoloLosRegistrosConocidos() {
        entoncesSeAceptan(
                accion("borrar_registros", Map.of("pieza", "e1", "ids", List.of("r2"))),
                accion("borrar_registros", Map.of("pieza", "e1", "donde", List.of(Map.of("Producto", "Té")))));
    }

    private void entoncesLaCorreccionDelRegistroSigueValiendo() {
        assertThat(validacion.acciones().getLast()).isEqualTo(accion("editar_registros", Map.of("pieza", "e1",
                "registros", List.of(Map.of("id", "r1", "datos", Map.of("Marca", "Playadito"))))));
    }

    private void entoncesSeBorranPorCriterioConLasEtiquetasDeLaPieza() {
        entoncesSeAceptan(
                accion("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Yerba", "Cantidad", 3)))),
                accion("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Fideos"), Map.of("Producto", "Aceite")))));
    }

    private void entoncesSeUsanSoloDiezCriteriosValidos() {
        assertThat((List<?>) validacion.acciones().getFirst().argumentos().get("donde")).hasSize(10)
                .first().isEqualTo(Map.of("Producto", "P1"));
    }

    private void entoncesLlegaUnaBotoneraDeTresDudasEnOrdenConSuTema() {
        assertThat(validacion.preguntas()).extracting("pregunta")
                .containsExactly("¿Qué significan los tildes?", "¿Costo o deuda?", "¿Cuál total?");
        assertThat(validacion.preguntas().getFirst().tema()).isEqualTo("Tildes");
        assertThat(validacion.preguntas().get(1).tema()).isNull();
    }

    private void entoncesLosVinculosUsanLaEtiquetaPedidaOElSingularDelComponente() {
        entoncesSeAceptan(
                accion("agregar_pieza", Map.of("tipo", "sales")),
                accion("conectar", Map.of("desde", "e1", "hacia", "sales", "etiqueta", "Venta")),
                accion("conectar", Map.of("desde", "e2", "hacia", "e1", "etiqueta", "Producto favorito")));
    }

    private void entoncesElVinculoSeLlamaTurnos() {
        assertThat(validacion.acciones().get(1))
                .isEqualTo(accion("conectar", Map.of("desde", "e2", "hacia", "Turnos", "etiqueta", "Turnos")));
    }

    private void entoncesElVinculoSeLlamaAgenda() {
        entoncesLaUnicaAccionEs(accion("conectar", Map.of("desde", "e1", "hacia", "e9", "etiqueta", "Agenda")));
    }

    private void entoncesElDatoClienteSeConvierteEnElVinculo() {
        entoncesSeAceptan(
                accion("agregar_pieza", turnosConUnDatoCliente()),
                accion("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "Cliente")));
    }

    private void entoncesSoloSeConectanPedidosConInventario() {
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta).containsExactly(
                "agregar_pieza", "agregar_pieza", "agregar_pieza", "agregar_pieza", "conectar");
        assertThat(validacion.acciones().getLast().argumentos())
                .containsEntry("desde", "orders").containsEntry("hacia", "e1");
    }

    private void entoncesElKitArmaCuatroPiezasUnVinculoYSuPosicion() {
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta).containsExactly(
                "agregar_pieza", "agregar_pieza", "agregar_pieza", "agregar_pieza", "conectar", "editar_campo");
        assertThat(validacion.acciones()).extracting(accion -> accion.argumentos().get("nombre"))
                .containsExactly("Velas", "Materiales", "Ventas", "Quién me debe", null, null);
        assertThat(validacion.acciones().get(4).argumentos()).isEqualTo(
                Map.of("desde", "inventory", "hacia", "Materiales", "etiqueta", "Materiales que lleva"));
        // Con clientas en el kit, el dato Cliente de las ventas va al lugar 2 para que se vea.
        assertThat(validacion.acciones().get(5).argumentos())
                .isEqualTo(Map.of("pieza", "sales", "campo", "Cliente", "posicion", 2));
        assertThat((List<?>) validacion.acciones().getFirst().argumentos().get("ejemplos")).hasSize(3);
    }

    private void entoncesElKitSoloSumaTurnosYServiciosYLosConectaConLasPiezasExistentes() {
        assertThat(validacion.acciones()).extracting(accion -> accion.argumentos().get("nombre"))
                .containsExactly("Turnos", "Servicios", null, null);
        assertThat(validacion.acciones().subList(2, 4)).containsExactly(
                accion("conectar", Map.of("desde", "Turnos", "hacia", "e2", "etiqueta", "Cliente")),
                accion("conectar", Map.of("desde", "Turnos", "hacia", "Servicios", "etiqueta", "Servicio")));
    }

    private void entoncesSoloSeAplicaUnaVezElKitDeVelas() {
        assertThat(validacion.acciones()).hasSize(6);
    }

    private void entoncesSeProponeLaPreguntaDelKitDeVelas() {
        assertThat(validacion.preguntas())
                .containsExactly(SamiPruebas.kitsReales().buscar("velas").orElseThrow().pregunta());
    }

    private void entoncesSoloValeLaPreguntaDelModelo() {
        assertThat(validacion.preguntas())
                .containsExactly(new OpcionesTaller("¿Vendés por pedido?", List.of("Sí", "No")));
    }

    private void entoncesNoHayPreguntas() {
        assertThat(validacion.preguntas()).isEmpty();
    }

    private void entoncesLaPreguntaDelKitSeProponeUnaVez() {
        assertThat(validacion.preguntas()).as("la segunda vez no suma nada, la primera sí").hasSize(1);
    }

    private void entoncesSeAplicanDiecisieteAcciones() {
        assertThat(validacion.acciones()).hasSize(17);
    }

    private void entoncesSeAplicanVeinticuatroPiezasNadaMas() {
        assertThat(validacion.acciones()).hasSize(24);
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta).containsOnly("agregar_pieza");
    }

    // --- datos ---

    private static Map<String, Object> correccionDeCantidadACinco() {
        return Map.of("pieza", "e1", "registros", List.of(Map.of("id", "r1", "datos", Map.of("Cantidad", 5))));
    }

    private static Map<String, Object> turnosConUnDatoCliente() {
        return Map.of("tipo", "form", "nombre", "Turnos",
                "campos", List.of(Map.of("etiqueta", "Cliente", "tipo", "text")));
    }

    private static LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private static AccionTaller accion(String herramienta, Map<String, Object> argumentos) {
        return new AccionTaller(herramienta, argumentos);
    }
}
