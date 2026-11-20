package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.model.bloque.AnchoBloque;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.ColorBloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.DistribucionProyecto;
import com.ensambla.backend.domain.model.proyecto.PlantillaSistema;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SistemaItemTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId SISTEMA_ID = ProyectoId.de("d0000000-0000-4000-8000-000000000001");
    private static final SeccionId SECCION_ID = SeccionId.de("d1000000-0000-4000-8000-000000000001");
    private static final SeccionId OTRA_SECCION_ID = SeccionId.de("d1000000-0000-4000-8000-000000000002");
    private static final BloqueId INVENTARIO_ID = BloqueId.de("d2000000-0000-4000-8000-000000000001");
    private static final BloqueId CLIENTES_ID = BloqueId.de("d2000000-0000-4000-8000-000000000002");
    private static final BloqueId NOTA_ID = BloqueId.de("d2000000-0000-4000-8000-000000000003");
    private static final RegistroId FILA_ID = RegistroId.de("d3000000-0000-4000-8000-000000000001");
    private static final RegistroId CLIENTE_FILA_ID = RegistroId.de("d3000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-01-10T09:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");
    private static final UUID UUID_DE_COLUMNA = UUID.fromString("d4000000-0000-4000-8000-000000000001");
    private static final UUID SIN_CLAVE_ID = UUID.fromString("d4000000-0000-4000-8000-000000000001");
    private static final UUID FORMULA_ID = UUID.fromString("d4000000-0000-4000-8000-000000000002");

    private SistemaItem documento;
    private Sistema sistema;
    private SistemaItem documentoDevuelto;
    private List<CampoItem> camposDelDocumento;
    private Map<String, Object> filaDelDocumento;

    // ===== pantalla -> backend =====

    @Test
    @DisplayName("Mapea el proyecto: nombre, plantilla, distribución, opciones, demo y fechas")
    void mapeaElProyectoConNombrePlantillaDistribucionOpcionesDemoYFechas() {
        dadoUnDocumento("e" + SISTEMA_ID, List.of(unaSeccion(SECCION_ID.toString(), List.of())),
                "shop", new OpcionesItem(true, false), "two", true, "2026-01-10T09:00:00Z");

        cuandoSePasaAlModeloConElIdDePantalla("e" + SISTEMA_ID);

        entoncesElProyectoCopiaTodosLosDatosDelDocumento();
    }

    @Test
    @DisplayName("Sin plantilla, distribución ni opciones usa los valores por defecto")
    void sinPlantillaDistribucionNiOpcionesUsaLosValoresPorDefecto() {
        dadoUnDocumento(SISTEMA_ID.toString(), List.of(), null, null, null, false, null);

        cuandoSePasaAlModelo();

        entoncesElProyectoUsaLosValoresPorDefecto();
    }

    @Test
    @DisplayName("Los gastos se activan solos y una distribución desconocida vuelve al mosaico")
    void losGastosSeActivanSolosYUnaDistribucionDesconocidaVuelveAlMosaico() {
        dadoUnDocumento(SISTEMA_ID.toString(), List.of(), "warehouse", new OpcionesItem(false, true),
                "otra", false, "no es una fecha");

        cuandoSePasaAlModelo();

        entoncesLosGastosEstanActivosYLaDistribucionEsMosaico();
    }

    @Test
    @DisplayName("Mapea secciones y bloques con su posición, su sección y sus datos")
    void mapeaSeccionesYBloquesConSuPosicionSuSeccionYSusDatos() {
        dadoUnDocumentoConUnInventarioYUnaNotaEnDosSecciones();

        cuandoSePasaAlModelo();

        entoncesLasSeccionesQuedanOrdenadasYBienNombradas();
        entoncesLosBloquesQuedanEnSuSeccionYPosicion();
        entoncesElInventarioConservaSusDatos();
        entoncesLaNotaConservaSusDatos();
    }

    @Test
    @DisplayName("Un tipo, color o ancho desconocido usa el valor por defecto y la dirección de stock debe ser 1 o -1")
    void unTipoColorOAnchoDesconocidoUsaElValorPorDefectoYLaDireccionDeStockDebeSerUnoOMenosUno() {
        dadoUnDocumentoConUnBloqueDesconocidoYUnoDeEntradas();

        cuandoSePasaAlModelo();

        entoncesElBloqueDesconocidoUsaLosValoresPorDefecto();
        entoncesElBloqueDeEntradasConservaSusDatos();
    }

    @Test
    @DisplayName("Mapea las columnas con su tipo, clave, opciones, mínimo y conexión")
    void mapeaLasColumnasConSuTipoClaveOpcionesMinimoYConexion() {
        dadoUnInventarioConUnaColumnaDeCadaTipo();

        cuandoSePasaAlModelo();

        entoncesCadaColumnaTieneSuTipo();
        entoncesLasColumnasConservanClavePosicionYDatos();
    }

    @Test
    @DisplayName("Corrige lo que el modelo no admite: opciones vacías, conexión a un bloque ausente, claves inválidas o repetidas")
    void corrigeLoQueElModeloNoAdmiteOpcionesVaciasConexionAUnBloqueAusenteClavesInvalidasORepetidas() {
        dadoUnInventarioConColumnasInvalidas();

        cuandoSePasaAlModelo();

        entoncesLasColumnasInvalidasSeCorrigen();
    }

    @Test
    @DisplayName("Guarda las filas por el id de la columna y respeta el orden")
    void guardaLasFilasPorElIdDeLaColumnaYRespetaElOrden() {
        dadoUnInventarioConTresFilas();

        cuandoSePasaAlModelo();

        entoncesLasFilasQuedanEnOrden();
        entoncesLaPrimeraFilaGuardaSusValoresPorIdDeColumna();
        entoncesLasOtrasFilasConservanSusVacios();
    }

    @Test
    @DisplayName("Si dos columnas comparten clave, la fila usa la primera; un origen largo se recorta")
    void siDosColumnasComparatenClaveLaFilaUsaLaPrimeraYUnOrigenLargoSeRecorta() {
        dadoUnInventarioConColumnasRepetidasYUnOrigenLargo();

        cuandoSePasaAlModelo();

        entoncesLaFilaUsaLaPrimeraColumnaYRecortaElOrigen();
    }

    @Test
    @DisplayName("Los nombres vacíos se reemplazan y los largos se recortan")
    void losNombresVaciosSeReemplazanYLosLargosSeRecortan() {
        dadoUnDocumentoConNombresVaciosYLargos();

        cuandoSePasaAlModelo();

        entoncesLosNombresVaciosSeReemplazanYLosLargosSeRecortan();
    }

    // ===== backend -> pantalla =====

    @Test
    @DisplayName("Devuelve el documento que espera la pantalla, con ids, claves y filas por clave")
    void devuelveElDocumentoQueEsperaLaPantallaConIdsClavesYFilasPorClave() {
        dadoUnSistemaDeAlmacen();

        cuandoSeArmaElDocumentoDeLaPantalla();

        entoncesElDocumentoTieneLosDatosDelProyecto();
        entoncesElDocumentoTieneLasSeccionesYElInventario();
        entoncesElInventarioTieneSusColumnasPorClave();
    }

    @Test
    @DisplayName("Las filas vuelven con su id, las marcas y los valores por la clave de la columna")
    void lasFilasVuelvenConSuIdLasMarcasYLosValoresPorLaClaveDeLaColumna() {
        dadoUnSistemaDeAlmacen();

        cuandoSeArmaElDocumentoDeLaPantalla();

        entoncesLasFilasVuelvenPorClaveDeColumna();
    }

    @Test
    @DisplayName("Una columna sin clave usa su id, y una fórmula se muestra como número")
    void unaColumnaSinClaveUsaSuIdYUnaFormulaSeMuestraComoNumero() {
        dadoUnBloqueDeGastosConUnaColumnaSinClaveYUnaFormula();

        cuandoSeArmaElDocumentoDeLaPantalla();

        entoncesLaColumnaSinClaveUsaSuIdYLaFormulaEsNumero();
    }

    @Test
    @DisplayName("Un proyecto sin secciones se muestra con una sección Vista general que contiene sus bloques")
    void unProyectoSinSeccionesSeMuestraConUnaSeccionVistaGeneralQueContieneSusBloques() {
        dadoUnProyectoConUnBloqueYSinSecciones();

        cuandoSeArmaElDocumentoDeLaPantalla();

        entoncesSeMuestraUnaSeccionVistaGeneralConElBloque();
    }

    @Test
    @DisplayName("Los bloques sin sección van a la primera y los demás a la suya, ordenados por posición")
    void losBloquesSinSeccionVanALaPrimeraYLosDemasALaSuyaOrdenadosPorPosicion() {
        dadoBloquesSueltosYBloquesDeCadaSeccion();

        cuandoSeArmaElDocumentoDeLaPantalla();

        entoncesLosBloquesSueltosVanALaPrimeraSeccionOrdenados();
    }

    @Test
    @DisplayName("Guardar y volver a leer devuelve el mismo documento")
    void guardarYVolverALeerDevuelveElMismoDocumento() {
        dadoUnDocumentoCompletoConUnaFila();

        cuandoSeGuardaYSeVuelveALeer();

        entoncesSeDevuelveElMismoDocumento();
    }

    // --- dado ---

    private void dadoUnDocumento(String id, List<SeccionItem> secciones, String plantilla, OpcionesItem opciones,
                                 String distribucion, boolean demo, String creado) {
        documento = new SistemaItem(id, "Mi almacén", creado, null, secciones, plantilla, opciones, distribucion, demo);
    }

    private void dadoUnDocumentoConSecciones(List<SeccionItem> secciones) {
        dadoUnDocumento(SISTEMA_ID.toString(), secciones, null, null, null, false, null);
    }

    private void dadoUnDocumentoConUnInventarioYUnaNotaEnDosSecciones() {
        BloqueItem inventario = unBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "coral", "wide",
                List.of(), List.of());
        BloqueItem nota = new BloqueItem(NOTA_ID.toString(), "note", "Notas", null, "petrol", "full", List.of(),
                List.of(), "Llamar al proveedor", true, -1);
        dadoUnDocumentoConSecciones(List.of(
                unaSeccion(SECCION_ID.toString(), List.of(inventario)),
                unaSeccion(OTRA_SECCION_ID.toString(), List.of(nota))));
    }

    private void dadoUnDocumentoConUnBloqueDesconocidoYUnoDeEntradas() {
        BloqueItem desconocido = new BloqueItem(INVENTARIO_ID.toString(), "raro", "X", "", "violeta", "enorme",
                List.of(), List.of(), "", null, 2);
        BloqueItem entradas = new BloqueItem(CLIENTES_ID.toString(), "entries", "Entradas", "", "yellow", "third",
                List.of(), List.of(), "", false, 1);
        dadoUnDocumentoConSecciones(List.of(unaSeccion(SECCION_ID.toString(), List.of(desconocido, entradas))));
    }

    private void dadoUnInventarioConUnaColumnaDeCadaTipo() {
        dadoUnInventarioConColumnas(List.of(
                new CampoItem("name", "Producto", "text", true, null, null, null),
                new CampoItem("price", "Precio", "currency", false, null, new BigDecimal("0"), null),
                new CampoItem("category", "Categoría", "select", false, List.of("Insumos", "Terminados"), null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()),
                new CampoItem("notes", "Notas", "textarea", false, null, null, null),
                new CampoItem("ok", "Activo", "checkbox", false, null, null, null),
                new CampoItem("day", "Día", "date", false, null, null, null),
                new CampoItem("hour", "Hora", "time", false, null, null, null),
                new CampoItem("mail", "Correo", "email", false, null, null, null),
                new CampoItem("phone", "Teléfono", "tel", false, null, null, null),
                new CampoItem("qty", "Cantidad", "number", false, null, null, null)));
    }

    private void dadoUnInventarioConColumnasInvalidas() {
        dadoUnInventarioConColumnas(List.of(
                new CampoItem("estado", "Estado", "select", false, List.of(), null, null),
                new CampoItem("sin-opciones", "Sin opciones", "select", false, null, null, null),
                new CampoItem("cliente", "Cliente", "relation", false, null, null, "bloque-que-no-esta"),
                new CampoItem("sin-destino", "Sin destino", "relation", false, null, null, null),
                new CampoItem("repetida", "Una", "text", false, null, null, null),
                new CampoItem("repetida", "Otra", "text", false, null, null, null),
                new CampoItem(UUID_DE_COLUMNA.toString(), "Con UUID", "text", false, null, null, null),
                new CampoItem("no valida!", "Clave inválida", "text", false, null, null, null),
                new CampoItem("rara", "Rara", "tipo-que-no-existe", false, null, null, null)));
    }

    private void dadoUnInventarioConColumnas(List<CampoItem> campos) {
        dadoUnDocumentoConSecciones(List.of(unaSeccion(SECCION_ID.toString(), List.of(
                unBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos, List.of()),
                unBloque(CLIENTES_ID.toString(), "clients", "Clientes", "blue", "half", List.of(), List.of())))));
    }

    private void dadoUnInventarioConTresFilas() {
        List<CampoItem> campos = List.of(
                new CampoItem("name", "Producto", "text", false, null, null, null),
                new CampoItem("price", "Precio", "currency", false, null, null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()));
        dadoUnInventarioConFilas(campos, List.of(unaFilaCompleta(), unaFilaConVacios(), unaFilaSinId()));
    }

    private void dadoUnInventarioConFilas(List<CampoItem> campos, List<Map<String, Object>> filas) {
        dadoUnDocumentoConSecciones(List.of(unaSeccion(SECCION_ID.toString(), List.of(
                unBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos, filas),
                unBloque(CLIENTES_ID.toString(), "clients", "Clientes", "blue", "half", List.of(), List.of())))));
    }

    private void dadoUnInventarioConColumnasRepetidasYUnOrigenLargo() {
        List<CampoItem> campos = List.of(
                new CampoItem("repetida", "Una", "text", false, null, null, null),
                new CampoItem("repetida", "Otra", "text", false, null, null, null));
        Map<String, Object> fila = fila(FILA_ID.toString(), "repetida", "valor");
        fila.put("__source", "x".repeat(300));
        dadoUnDocumentoConSecciones(List.of(unaSeccion(SECCION_ID.toString(), List.of(
                unBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos, List.of(fila))))));
    }

    private void dadoUnDocumentoConNombresVaciosYLargos() {
        BloqueItem bloque = new BloqueItem(INVENTARIO_ID.toString(), "form", "   ", "", "blue", "half",
                List.of(new CampoItem("a", "x".repeat(200), "text", false, null, null, null),
                        new CampoItem("b", null, "text", false, null, null, null)),
                List.of(), "", false, null);
        documento = new SistemaItem(SISTEMA_ID.toString(), "  " + "n".repeat(150) + "  ", null, null,
                List.of(new SeccionItem(SECCION_ID.toString(), null, List.of(bloque))), null, null, null, false);
    }

    private void dadoUnSistemaDeAlmacen() {
        sistema = unSistemaDeAlmacen();
    }

    private void dadoUnBloqueDeGastosConUnaColumnaSinClaveYUnaFormula() {
        Bloque bloque = unBloqueDelModelo(INVENTARIO_ID.valor(), SECCION_ID, 0, TipoBloque.GASTOS, List.of(
                Campo.builder().id(CampoId.de(FORMULA_ID)).bloqueId(INVENTARIO_ID).nombre("Total")
                        .tipo(TipoCampo.FORMULA)
                        .obligatorio(false).posicion((short) 1).build(),
                Campo.builder().id(CampoId.de(SIN_CLAVE_ID))
                        .bloqueId(INVENTARIO_ID).nombre("Monto").tipo(TipoCampo.MONEDA)
                        .obligatorio(true).posicion((short) 0).build()));
        Registro fila = Registro.builder().id(FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(false)
                .valores(Map.of(SIN_CLAVE_ID.toString(), 50)).build();
        sistema = new Sistema(unProyecto(List.of(bloque)), List.of(unaSeccionDelModelo(SECCION_ID, 0)),
                Map.of(INVENTARIO_ID, List.of(fila)));
    }

    private void dadoUnProyectoConUnBloqueYSinSecciones() {
        Bloque bloque = unBloqueDelModelo(INVENTARIO_ID.valor(), null, 0, TipoBloque.FORMULARIO, List.of());
        sistema = new Sistema(unProyecto(List.of(bloque)), List.of(), Map.of());
    }

    private void dadoBloquesSueltosYBloquesDeCadaSeccion() {
        Bloque huerfano = unBloqueDelModelo(NOTA_ID.valor(), null, 5, TipoBloque.NOTA, List.of());
        Bloque ajeno = unBloqueDelModelo(CLIENTES_ID.valor(), SeccionId.nuevo(), 3, TipoBloque.CLIENTES, List.of());
        Bloque propio = unBloqueDelModelo(INVENTARIO_ID.valor(), SECCION_ID, 1, TipoBloque.INVENTARIO, List.of());
        Bloque deLaOtra = unBloqueDelModelo(UUID.fromString("d2000000-0000-4000-8000-000000000009"), OTRA_SECCION_ID,
                0, TipoBloque.VENTAS, List.of());
        sistema = new Sistema(unProyecto(List.of(huerfano, ajeno, propio, deLaOtra)),
                List.of(unaSeccionDelModelo(OTRA_SECCION_ID, 1), unaSeccionDelModelo(SECCION_ID, 0)), Map.of());
    }

    private void dadoUnDocumentoCompletoConUnaFila() {
        camposDelDocumento = List.of(
                new CampoItem("name", "Producto", "text", true, null, null, null, true),
                new CampoItem("category", "Categoría", "select", false, List.of("Insumos"), null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()));
        filaDelDocumento = fila(FILA_ID.toString(), "name", "Harina", "category", "Insumos",
                "client", CLIENTE_FILA_ID.toString());
        documento = new SistemaItem(SISTEMA_ID.toString(), "Mi almacén", "2026-01-10T09:00:00Z",
                "2026-10-21T10:00:00Z", List.of(unaSeccion(SECCION_ID.toString(), List.of(
                unBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "orange", "half", camposDelDocumento,
                        List.of(filaDelDocumento)),
                unBloque(CLIENTES_ID.toString(), "clients", "Clientes", "orange", "half", List.of(), List.of())))),
                "shop", new OpcionesItem(true, true), "mosaic", false);
    }

    // --- cuando ---

    private void cuandoSePasaAlModelo() {
        cuandoSePasaAlModeloConElIdDePantalla(SISTEMA_ID.toString());
    }

    private void cuandoSePasaAlModeloConElIdDePantalla(String idDePantalla) {
        sistema = documento.aSistema(NEGOCIO_ID, idDePantalla, AHORA);
    }

    private void cuandoSeArmaElDocumentoDeLaPantalla() {
        documentoDevuelto = SistemaItem.de(sistema);
    }

    private void cuandoSeGuardaYSeVuelveALeer() {
        documentoDevuelto = SistemaItem.de(documento.aSistema(NEGOCIO_ID, SISTEMA_ID.toString(), AHORA));
    }

    // --- entonces: pantalla -> backend ---

    private void entoncesElProyectoCopiaTodosLosDatosDelDocumento() {
        Proyecto proyecto = sistema.proyecto();
        assertThat(proyecto.getId()).isEqualTo(SISTEMA_ID);
        assertThat(proyecto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(proyecto.getNombre()).isEqualTo("Mi almacén");
        assertThat(proyecto.getPlantillaClave()).isEqualTo(PlantillaSistema.ALMACEN);
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.DOS);
        assertThat(proyecto.getConMovimientos()).isTrue();
        assertThat(proyecto.getConGastos()).isFalse();
        assertThat(proyecto.getEsDemo()).isTrue();
        assertThat(proyecto.getCreadoEn()).isEqualTo(CREADO);
        assertThat(proyecto.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElProyectoUsaLosValoresPorDefecto() {
        Proyecto proyecto = sistema.proyecto();
        assertThat(proyecto.getPlantillaClave()).isNull();
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.MOSAICO);
        assertThat(proyecto.getConMovimientos()).isFalse();
        assertThat(proyecto.getConGastos()).isFalse();
        assertThat(proyecto.getEsDemo()).isFalse();
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLosGastosEstanActivosYLaDistribucionEsMosaico() {
        Proyecto proyecto = sistema.proyecto();
        assertThat(proyecto.getPlantillaClave()).isEqualTo(PlantillaSistema.DEPOSITO);
        assertThat(proyecto.getConMovimientos()).isFalse();
        assertThat(proyecto.getConGastos()).isTrue();
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.MOSAICO);
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLasSeccionesQuedanOrdenadasYBienNombradas() {
        assertThat(sistema.secciones()).extracting(Seccion::getId).containsExactly(SECCION_ID, OTRA_SECCION_ID);
        assertThat(sistema.secciones()).extracting(Seccion::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(sistema.secciones().getFirst().getNombre()).isEqualTo("Vista general");
        assertThat(sistema.secciones().getFirst().getProyectoId()).isEqualTo(SISTEMA_ID);
    }

    private void entoncesLosBloquesQuedanEnSuSeccionYPosicion() {
        List<Bloque> bloques = bloquesDelModelo();
        assertThat(bloques).extracting(Bloque::getId).containsExactly(INVENTARIO_ID, NOTA_ID);
        assertThat(bloques).extracting(Bloque::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(bloques).extracting(Bloque::getSeccionId).containsExactly(SECCION_ID, OTRA_SECCION_ID);
    }

    private void entoncesElInventarioConservaSusDatos() {
        Bloque primero = bloquesDelModelo().getFirst();
        assertThat(primero.getTipo()).isEqualTo(TipoBloque.INVENTARIO);
        assertThat(primero.getColor()).isEqualTo(ColorBloque.CORAL);
        assertThat(primero.getAncho()).isEqualTo(AnchoBloque.ANCHO);
        assertThat(primero.getDescripcion()).isEqualTo("Lo que vendo");
        assertThat(primero.getContenido()).isEmpty();
        assertThat(primero.getEmpiezaFila()).isFalse();
        assertThat(primero.getDireccionStock()).isNull();
    }

    private void entoncesLaNotaConservaSusDatos() {
        Bloque segundo = bloquesDelModelo().getLast();
        assertThat(segundo.getTipo()).isEqualTo(TipoBloque.NOTA);
        assertThat(segundo.getDescripcion()).isEmpty();
        assertThat(segundo.getContenido()).isEqualTo("Llamar al proveedor");
        assertThat(segundo.getEmpiezaFila()).isTrue();
        assertThat(segundo.getDireccionStock()).isEqualTo((short) -1);
    }

    private void entoncesElBloqueDesconocidoUsaLosValoresPorDefecto() {
        Bloque primero = bloquesDelModelo().getFirst();
        assertThat(primero.getTipo()).isEqualTo(TipoBloque.FORMULARIO);
        assertThat(primero.getColor()).isEqualTo(ColorBloque.CELESTE);
        assertThat(primero.getAncho()).isEqualTo(AnchoBloque.MEDIO);
        assertThat(primero.getDireccionStock()).isNull();
    }

    private void entoncesElBloqueDeEntradasConservaSusDatos() {
        Bloque ultimo = bloquesDelModelo().getLast();
        assertThat(ultimo.getTipo()).isEqualTo(TipoBloque.ENTRADAS);
        assertThat(ultimo.getColor()).isEqualTo(ColorBloque.AMARILLO);
        assertThat(ultimo.getAncho()).isEqualTo(AnchoBloque.TERCIO);
        assertThat(ultimo.getDireccionStock()).isEqualTo((short) 1);
    }

    private void entoncesCadaColumnaTieneSuTipo() {
        assertThat(columnasDelInventario()).extracting(Campo::getTipo).containsExactly(
                TipoCampo.TEXTO, TipoCampo.MONEDA, TipoCampo.OPCION, TipoCampo.RELACION, TipoCampo.TEXTO_LARGO,
                TipoCampo.SI_NO, TipoCampo.FECHA, TipoCampo.HORA, TipoCampo.EMAIL, TipoCampo.TELEFONO,
                TipoCampo.NUMERO);
    }

    private void entoncesLasColumnasConservanClavePosicionYDatos() {
        List<Campo> columnas = columnasDelInventario();
        assertThat(columnas).extracting(Campo::getClave).startsWith("name", "price", "category", "client");
        assertThat(columnas).extracting(Campo::getPosicion).startsWith((short) 0, (short) 1, (short) 2);
        assertThat(columnas.getFirst().getNombre()).isEqualTo("Producto");
        assertThat(columnas.getFirst().esObligatorio()).isTrue();
        assertThat(columnas.get(1).getMinimo()).isEqualByComparingTo("0");
        assertThat(columnas.get(2).getOpciones()).containsExactly("Insumos", "Terminados");
        assertThat(columnas.getFirst().getOpciones()).isEmpty();
        assertThat(columnas.get(3).getBloqueRelacionadoId()).isEqualTo(CLIENTES_ID);
        assertThat(columnas.getFirst().getBloqueRelacionadoId()).isNull();
        assertThat(columnas.getFirst().getId()).isEqualTo(IdSistema.aId(INVENTARIO_ID, "name", CampoId::de));
    }

    private void entoncesLasColumnasInvalidasSeCorrigen() {
        List<Campo> columnas = columnasDelInventario();
        assertThat(columnas.get(0).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(columnas.get(1).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(columnas.get(2).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(columnas.get(2).getBloqueRelacionadoId()).isNull();
        assertThat(columnas.get(3).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(columnas.get(4).getClave()).isEqualTo("repetida");
        assertThat(columnas.get(5).getClave()).isNull();
        assertThat(columnas.get(6).getId()).isEqualTo(CampoId.de(UUID_DE_COLUMNA));
        assertThat(columnas.get(7).getClave()).isNull();
        assertThat(columnas.get(8).getTipo()).isEqualTo(TipoCampo.TEXTO);
    }

    private void entoncesLasFilasQuedanEnOrden() {
        assertThat(filasDelInventario()).extracting(Registro::getOrden).containsExactly(0, 1, 2);
    }

    private void entoncesLaPrimeraFilaGuardaSusValoresPorIdDeColumna() {
        Registro harina = filasDelInventario().getFirst();
        assertThat(harina.getId()).isEqualTo(FILA_ID);
        assertThat(harina.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(harina.getBloqueId()).isEqualTo(INVENTARIO_ID);
        assertThat(harina.getValores()).containsOnly(
                Map.entry(idDeColumna("name").toString(), "Harina"),
                Map.entry(idDeColumna("price").toString(), 1200),
                Map.entry(idDeColumna("client").toString(), CLIENTE_FILA_ID.toString()));
        assertThat(harina.getEsEjemplo()).isTrue();
        assertThat(harina.getOrigen()).isEqualTo("Lectura de foto");
    }

    private void entoncesLasOtrasFilasConservanSusVacios() {
        List<Registro> filas = filasDelInventario();
        assertThat(filas.get(1).getValores())
                .containsEntry(idDeColumna("name").toString(), null)
                .containsEntry(idDeColumna("client").toString(), "");
        assertThat(filas.get(1).getEsEjemplo()).isFalse();
        assertThat(filas.get(1).getOrigen()).isNull();
        assertThat(filas.get(2).getId()).isNotNull();
        assertThat(filas.get(2).getOrigen()).isNull();
        assertThat(sistema.registrosPorBloque().get(CLIENTES_ID)).isEmpty();
    }

    private void entoncesLaFilaUsaLaPrimeraColumnaYRecortaElOrigen() {
        Registro registro = filasDelInventario().getFirst();
        assertThat(registro.getValores()).containsOnlyKeys(columnasDelInventario().getFirst().getId().toString());
        assertThat(registro.getOrigen()).hasSize(120);
    }

    private void entoncesLosNombresVaciosSeReemplazanYLosLargosSeRecortan() {
        assertThat(sistema.proyecto().getNombre()).hasSize(120).startsWith("nnn");
        assertThat(sistema.secciones().getFirst().getNombre()).isEqualTo("Sin nombre");
        Bloque bloque = bloquesDelModelo().getFirst();
        assertThat(bloque.getNombre()).isEqualTo("Sin nombre");
        assertThat(bloque.getCampos().getFirst().getNombre()).hasSize(120);
        assertThat(bloque.getCampos().getLast().getNombre()).isEqualTo("Sin nombre");
    }

    // --- entonces: backend -> pantalla ---

    private void entoncesElDocumentoTieneLosDatosDelProyecto() {
        assertThat(documentoDevuelto.id()).isEqualTo(SISTEMA_ID.toString());
        assertThat(documentoDevuelto.name()).isEqualTo("Mi almacén");
        assertThat(documentoDevuelto.createdAt()).isEqualTo("2026-01-10T09:00:00Z");
        assertThat(documentoDevuelto.updatedAt()).isEqualTo("2026-10-21T10:00:00Z");
        assertThat(documentoDevuelto.template()).isEqualTo("shop");
        assertThat(documentoDevuelto.options()).isEqualTo(new OpcionesItem(true, false));
        assertThat(documentoDevuelto.layout()).isEqualTo("three");
        assertThat(documentoDevuelto.demoAlmacen()).isTrue();
    }

    private void entoncesElDocumentoTieneLasSeccionesYElInventario() {
        assertThat(documentoDevuelto.sections()).extracting(SeccionItem::id)
                .containsExactly(SECCION_ID.toString(), OTRA_SECCION_ID.toString());
        SeccionItem general = documentoDevuelto.sections().getFirst();
        assertThat(general.name()).isEqualTo("Vista general");
        assertThat(general.blocks()).extracting(BloqueItem::id)
                .containsExactly(INVENTARIO_ID.toString(), CLIENTES_ID.toString());
        BloqueItem inventario = inventarioDevuelto();
        assertThat(inventario.type()).isEqualTo("inventory");
        assertThat(inventario.color()).isEqualTo("orange");
        assertThat(inventario.width()).isEqualTo("half");
        assertThat(inventario.description()).isEqualTo("Lo que vendo");
        assertThat(inventario.content()).isEmpty();
        assertThat(inventario.startRow()).isFalse();
        assertThat(inventario.stockDirection()).isEqualTo(1);
    }

    private void entoncesElInventarioTieneSusColumnasPorClave() {
        List<CampoItem> columnas = inventarioDevuelto().fields();
        assertThat(columnas).extracting(CampoItem::key).containsExactly("name", "categoria", "cliente");
        assertThat(columnas.getFirst()).isEqualTo(
                new CampoItem("name", "Producto", "text", true, null, new BigDecimal("1"), null, true));
        assertThat(columnas.get(1).type()).isEqualTo("select");
        assertThat(columnas.get(1).options()).containsExactly("Insumos");
        assertThat(columnas.get(2).type()).isEqualTo("relation");
        assertThat(columnas.get(2).target()).isEqualTo(CLIENTES_ID.toString());
    }

    private void entoncesLasFilasVuelvenPorClaveDeColumna() {
        List<Map<String, Object>> filas = inventarioDevuelto().rows();
        assertThat(filas).hasSize(2);
        assertThat(filas.getFirst()).containsEntry("id", FILA_ID.toString())
                .containsEntry("name", "Harina")
                .containsEntry("__example", true)
                .containsEntry("__source", "Aviso de Sami")
                .containsEntry("cliente", CLIENTE_FILA_ID.toString())
                .doesNotContainKey("categoria");
        assertThat(filas.getLast()).containsOnlyKeys("id", "name").containsEntry("name", null);
    }

    private void entoncesLaColumnaSinClaveUsaSuIdYLaFormulaEsNumero() {
        BloqueItem bloque = documentoDevuelto.sections().getFirst().blocks().getFirst();
        assertThat(bloque.type()).isEqualTo("expenses");
        assertThat(bloque.fields()).extracting(CampoItem::key)
                .containsExactly(SIN_CLAVE_ID.toString(), FORMULA_ID.toString());
        assertThat(bloque.fields().getLast().type()).isEqualTo("number");
        assertThat(bloque.rows().getFirst()).containsEntry(SIN_CLAVE_ID.toString(), 50);
    }

    private void entoncesSeMuestraUnaSeccionVistaGeneralConElBloque() {
        assertThat(documentoDevuelto.sections()).singleElement().satisfies(seccion -> {
            assertThat(seccion.name()).isEqualTo("Vista general");
            assertThat(seccion.id()).isEqualTo(IdSistema.aFront(IdSistema.aId(SISTEMA_ID, "general", SeccionId::de)));
            assertThat(seccion.blocks()).extracting(BloqueItem::id).containsExactly(INVENTARIO_ID.toString());
        });
        assertThat(documentoDevuelto.template()).isNull();
    }

    private void entoncesLosBloquesSueltosVanALaPrimeraSeccionOrdenados() {
        List<SeccionItem> secciones = documentoDevuelto.sections();
        assertThat(secciones).extracting(SeccionItem::id)
                .containsExactly(SECCION_ID.toString(), OTRA_SECCION_ID.toString());
        assertThat(secciones.getFirst().blocks()).extracting(BloqueItem::id)
                .containsExactly(INVENTARIO_ID.toString(), CLIENTES_ID.toString(), NOTA_ID.toString());
        assertThat(secciones.getLast().blocks()).hasSize(1);
    }

    private void entoncesSeDevuelveElMismoDocumento() {
        BloqueItem inventario = inventarioDevuelto();
        assertThat(inventario.fields()).usingRecursiveComparison().isEqualTo(camposDelDocumento);
        assertThat(inventario.rows()).containsExactly(filaDelDocumento);
        assertThat(documentoDevuelto.options()).isEqualTo(new OpcionesItem(true, true));
        assertThat(documentoDevuelto.template()).isEqualTo("shop");
        assertThat(documentoDevuelto.layout()).isEqualTo("mosaic");
    }

    // --- lectura del resultado ---

    private List<Bloque> bloquesDelModelo() {
        return sistema.proyecto().getBloques();
    }

    private List<Campo> columnasDelInventario() {
        return bloquesDelModelo().getFirst().getCampos();
    }

    private List<Registro> filasDelInventario() {
        return sistema.registrosPorBloque().get(INVENTARIO_ID);
    }

    private CampoId idDeColumna(String clave) {
        return IdSistema.aId(INVENTARIO_ID, clave, CampoId::de);
    }

    private BloqueItem inventarioDevuelto() {
        return documentoDevuelto.sections().getFirst().blocks().getFirst();
    }

    // --- datos ---

    private SeccionItem unaSeccion(String id, List<BloqueItem> bloques) {
        return new SeccionItem(id, "Vista general", bloques);
    }

    private BloqueItem unBloque(String id, String tipo, String nombre, String color, String ancho,
                                List<CampoItem> campos, List<Map<String, Object>> filas) {
        return new BloqueItem(id, tipo, nombre, "Lo que vendo", color, ancho, campos, filas, null, null, null);
    }

    private Map<String, Object> fila(String id, Object... clavesYValores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", id);
        for (int i = 0; i < clavesYValores.length; i += 2) {
            fila.put((String) clavesYValores[i], clavesYValores[i + 1]);
        }
        return fila;
    }

    private Map<String, Object> unaFilaCompleta() {
        Map<String, Object> primera = fila("e" + FILA_ID, "name", "Harina", "price", 1200, "client", "e" + CLIENTE_FILA_ID);
        primera.put("__example", true);
        primera.put("__source", "Lectura de foto");
        primera.put("desconocida", "se descarta");
        return primera;
    }

    private Map<String, Object> unaFilaConVacios() {
        return fila("d3000000-0000-4000-8000-000000000009", "name", null, "client", "");
    }

    private Map<String, Object> unaFilaSinId() {
        Map<String, Object> tercera = new HashMap<>();
        tercera.put("name", "Sin id");
        tercera.put("client", null);
        tercera.put("__source", 7);
        return tercera;
    }

    private Sistema unSistemaDeAlmacen() {
        CampoId nombreId = CampoId.de("d4000000-0000-4000-8000-000000000001");
        UUID categoriaId = UUID.fromString("d4000000-0000-4000-8000-000000000002");
        CampoId clienteId = CampoId.de("d4000000-0000-4000-8000-000000000003");
        Bloque inventario = unBloqueDelModelo(INVENTARIO_ID.valor(), SECCION_ID, 0, TipoBloque.INVENTARIO, List.of(
                Campo.builder().id(clienteId).bloqueId(INVENTARIO_ID).nombre("Cliente").tipo(TipoCampo.RELACION)
                        .obligatorio(false).clave("cliente").bloqueRelacionadoId(CLIENTES_ID).posicion((short) 2).build(),
                Campo.builder().id(nombreId).bloqueId(INVENTARIO_ID).nombre("Producto").tipo(TipoCampo.TEXTO)
                        .obligatorio(true).clave("name").minimo(new BigDecimal("1")).posicion((short) 0).build(),
                Campo.builder().id(CampoId.de(categoriaId)).bloqueId(INVENTARIO_ID).nombre("Categoría")
                        .tipo(TipoCampo.OPCION)
                        .obligatorio(false).clave("categoria").opciones(List.of("Insumos")).posicion((short) 1).build()));
        inventario.setColor(ColorBloque.NARANJA);
        inventario.setDireccionStock((short) 1);
        Bloque clientes = unBloqueDelModelo(CLIENTES_ID.valor(), SECCION_ID, 1, TipoBloque.CLIENTES, List.of());
        Registro harina = Registro.builder().id(FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(true)
                .origen("Aviso de Sami").orden(0)
                .valores(Map.of(nombreId.toString(), "Harina", clienteId.toString(), CLIENTE_FILA_ID.toString()))
                .build();
        Map<String, Object> vacios = new HashMap<>();
        vacios.put(nombreId.toString(), null);
        Registro sinNombre = Registro.builder().id(CLIENTE_FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(false)
                .orden(1).valores(vacios).build();
        Proyecto proyecto = unProyecto(new ArrayList<>(List.of(inventario, clientes)));
        proyecto.setPlantillaClave(PlantillaSistema.ALMACEN);
        proyecto.setDistribucion(DistribucionProyecto.TRES);
        proyecto.setConMovimientos(true);
        proyecto.setEsDemo(true);
        return new Sistema(proyecto,
                List.of(unaSeccionDelModelo(OTRA_SECCION_ID, 1), unaSeccionDelModelo(SECCION_ID, 0)),
                Map.of(INVENTARIO_ID, List.of(harina, sinNombre)));
    }

    private Proyecto unProyecto(List<Bloque> bloques) {
        return Proyecto.builder().id(SISTEMA_ID).negocioId(NEGOCIO_ID).nombre("Mi almacén")
                .creadoEn(CREADO).actualizadoEn(AHORA).bloques(bloques).build();
    }

    private Seccion unaSeccionDelModelo(SeccionId id, int posicion) {
        return Seccion.builder().id(id).proyectoId(SISTEMA_ID).nombre(id.equals(SECCION_ID) ? "Vista general" : "Ventas")
                .posicion((short) posicion).build();
    }

    private Bloque unBloqueDelModelo(UUID id, SeccionId seccionId, int posicion, TipoBloque tipo, List<Campo> campos) {
        return Bloque.builder().id(BloqueId.de(id)).proyectoId(SISTEMA_ID).seccionId(seccionId).nombre("Bloque")
                .posicion((short) posicion).tipo(tipo).descripcion(tipo == TipoBloque.INVENTARIO ? "Lo que vendo" : "")
                .campos(campos).build();
    }
}
