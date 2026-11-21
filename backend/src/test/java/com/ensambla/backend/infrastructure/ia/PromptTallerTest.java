package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PromptTallerTest {

    private static final KitsRubro KITS = SamiPruebas.kitsReales();
    private static final ImagenTaller FOTO = new ImagenTaller("image/png", new byte[]{1});
    private static final EstadoTaller VACIO = new EstadoTaller("", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null,
                    List.of(new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"))),
            new PiezaTaller("e2", "clients", "Clientes", "Aparte", List.of()),
            new PiezaTaller("e3", "sales", "Ventas", null,
                    List.of(new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number")))));

    private final PromptTaller prompt = new PromptTaller(
            new ClassPathResource("ia/prompt-taller.md"), SamiPruebas.catalogoReal(), KITS);

    private PeticionConHerramientas peticion;
    private PeticionConHerramientas peticionConKit;
    private PeticionConHerramientas peticionSinKit;
    private Throwable rechazo;

    @Test
    @DisplayName("El contexto lista las piezas con su id, tipo, sección y datos, y termina con el mensaje")
    void elContextoListaLasPiezasConSuIdTipoSeccionYDatosYTerminaConElMensaje() {
        cuandoSePideLaPeticion(new ConsultaTaller("sumá gastos", CON_PIEZAS, List.of()));

        entoncesElContextoListaLasPiezasYTerminaConElMensaje();
    }

    @Test
    @DisplayName("Las instrucciones fijan el vocabulario: negocio, pestañas y tablas, y nunca sistema, sección ni cuaderno")
    void lasInstruccionesFijanElVocabularioNegocioPestaniasYTablasYNuncaSistemaSeccionNiCuaderno() {
        cuandoSePideLaPeticion(consultaSimple());

        entoncesLasInstruccionesFijanElVocabulario();
    }

    @Test
    @DisplayName("Las instrucciones terminan con los componentes del catálogo y sus datos de fábrica")
    void lasInstruccionesTerminanConLosComponentesDelCatalogoYSusDatosDeFabrica() {
        cuandoSePideLaPeticion(consultaSimple());

        entoncesLasInstruccionesTerminanConLosComponentesDelCatalogo();
    }

    @Test
    @DisplayName("Con foto, la manda en el pedido y suma al contexto la línea de la foto antes del mensaje")
    void conFotoLaMandaEnElPedidoYSumaAlContextoLaLineaDeLaFotoAntesDelMensaje() {
        cuandoSePideLaPeticionConFoto();

        entoncesLaFotoViajaYLaLineaDeLaFotoVaAntesDelMensaje();
    }

    @Test
    @DisplayName("Sin foto, el pedido no lleva imágenes ni la línea de la foto")
    void sinFotoElPedidoNoLlevaImagenesNiLaLineaDeLaFoto() {
        cuandoSePideLaPeticion(consultaSimple());

        entoncesNoHayImagenesNiLineaDeFoto();
    }

    @Test
    @DisplayName("Una pieza recién armada en el turno, sin id, se muestra sin id")
    void unaPiezaRecienArmadaEnElTurnoSinIdSeMuestraSinId() {
        cuandoSePideLaPeticionConUnaPiezaSinId();

        entoncesLaPiezaSeMuestraSinId();
    }

    @Test
    @DisplayName("Los nombres existentes de una pieza van bajo ella, antes de sus registros")
    void losNombresExistentesDeUnaPiezaVanBajoEllaAntesDeSusRegistros() {
        cuandoSePideLaPeticionConNombresExistentes();

        entoncesLosNombresExistentesVanBajoLaPiezaAntesDeSusRegistros();
    }

    @Test
    @DisplayName("Con una foto pide usar los nombres existentes y no cargar productos ni clientes")
    void conUnaFotoPideUsarLosNombresExistentesYNoCargarProductosNiClientes() {
        entoncesLaLineaDeLaFotoPideUsarLosNombresExistentes();
    }

    @Test
    @DisplayName("Los registros cargados van bajo su pieza, cada uno con su id y sus datos")
    void losRegistrosCargadosVanBajoSuPiezaCadaUnoConSuIdYSusDatos() {
        cuandoSePideLaPeticionConRegistrosCargados();

        entoncesLosRegistrosVanBajoSuPiezaConSuId();
    }

    @Test
    @DisplayName("Un sistema sin piezas lo dice")
    void unSistemaSinPiezasLoDice() {
        cuandoSePideLaPeticion(consultaSimple());

        entoncesElContextoDiceQueNoHayPiezas();
    }

    @Test
    @DisplayName("Sin rubro reconocido, lista los kits disponibles con su id y su nombre")
    void sinRubroReconocidoListaLosKitsDisponiblesConSuIdYSuNombre() {
        cuandoSePideLaPeticion(consultaSimple());

        entoncesElContextoListaLosKitsDisponibles();
    }

    @Test
    @DisplayName("Con rubro reconocido, resume su kit: piezas, tipo, datos extra y conexiones")
    void conRubroReconocidoResumeSuKitPiezasTipoDatosExtraYConexiones() {
        cuandoSePideLaPeticion(new ConsultaTaller(
                "vendo velas", VACIO, List.of(), KITS.buscar("velas").orElseThrow()));

        entoncesElContextoResumeElKitDeVelas();
    }

    @Test
    @DisplayName("Con piezas ya armadas no nombra el kit ni la lista de kits: un pedido puntual no arma el kit")
    void conPiezasYaArmadasNoNombraElKitNiLaListaDeKitsUnPedidoPuntualNoArmaElKit() {
        cuandoSePideLaPeticionConYSinKitSobreUnSistemaConPiezas();

        entoncesNingunContextoNombraKits();
    }

    @Test
    @DisplayName("Un kit sin conexiones ni datos extra se resume sin esas partes")
    void unKitSinConexionesNiDatosExtraSeResumeSinEsasPartes() {
        cuandoSePideLaPeticion(new ConsultaTaller(
                "dietética", VACIO, List.of(), KITS.buscar("dietetica").orElseThrow()));

        entoncesElKitSeResumeSinConexiones();
    }

    @Test
    @DisplayName("La charla reciente va con quién dijo cada cosa")
    void laCharlaRecienteVaConQuienDijoCadaCosa() {
        cuandoSePideLaPeticionConCharla();

        entoncesLaCharlaIncluyeQuienDijoCadaCosa();
    }

    @Test
    @DisplayName("Si falta el recurso del prompt la aplicación no levanta")
    void siFaltaElRecursoDelPromptLaAplicacionNoLevanta() {
        cuandoSeIntentaArmarElPromptConUnRecursoInexistente();

        entoncesSeRechazaElArranque();
    }

    @Test
    @DisplayName("Con un recurso mínimo las instrucciones son el texto más el catálogo")
    void conUnRecursoMinimoLasInstruccionesSonElTextoMasElCatalogo() {
        cuandoSePideLaPeticionConUnPromptMinimo();

        entoncesLasInstruccionesEmpiezanConElTextoYSigueElCatalogo();
    }

    @Test
    @DisplayName("La versión del prompt es la que muestra los nombres existentes y no carga catálogos desde la foto")
    void laVersionDelPromptEsLaQueMuestraLosNombresExistentesYNoCargaCatalogosDesdeLaFoto() {
        entoncesLaVersionEsV6();
    }

    // --- cuando ---

    private void cuandoSePideLaPeticion(ConsultaTaller consulta) {
        peticion = prompt.peticion(consulta);
    }

    private void cuandoSePideLaPeticionConFoto() {
        cuandoSePideLaPeticion(new ConsultaTaller("te paso mi cuaderno", VACIO, List.of(), null, foto()));
    }

    private void cuandoSePideLaPeticionConUnaPiezaSinId() {
        EstadoTaller armado = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller(null, "sales", "Ventas", null, List.of(new CampoTaller("Producto", "relation")))));
        cuandoSePideLaPeticion(new ConsultaTaller("cargá", armado, List.of()));
    }

    private void cuandoSePideLaPeticionConNombresExistentes() {
        EstadoTaller conClientes = new EstadoTaller("S", "mosaic", List.of(new PiezaTaller(
                "e2", "clients", "Clientes", null, List.of(new CampoTaller("Nombre", "text")),
                List.of(new RegistroTaller("r1", Map.of("Nombre", "Marta"))), List.of("Lucía", "Marta"))));
        cuandoSePideLaPeticion(new ConsultaTaller("cargá", conClientes, List.of()));
    }

    private void cuandoSePideLaPeticionConRegistrosCargados() {
        EstadoTaller conRegistros = new EstadoTaller("S", "mosaic", List.of(new PiezaTaller(
                "e1", "sales", "Ventas", null, List.of(new CampoTaller("Producto", "text")),
                List.of(new RegistroTaller("r1", Map.of("Producto", "Vela")),
                        new RegistroTaller("r2", Map.of("Producto", "Jabón"))))));
        cuandoSePideLaPeticion(new ConsultaTaller("cargá", conRegistros, List.of()));
    }

    private void cuandoSePideLaPeticionConYSinKitSobreUnSistemaConPiezas() {
        peticionConKit = prompt.peticion(new ConsultaTaller(
                "llamalo Velas de Ana", CON_PIEZAS, List.of(), KITS.buscar("velas").orElseThrow()));
        peticionSinKit = prompt.peticion(new ConsultaTaller("sumá un dato", CON_PIEZAS, List.of()));
    }

    private void cuandoSePideLaPeticionConCharla() {
        cuandoSePideLaPeticion(new ConsultaTaller("dale", new EstadoTaller("S", "mosaic", List.of()),
                List.of(new MensajeTaller("sami", "¿Sumamos gastos?"), new MensajeTaller("you", "tal vez"))));
    }

    private void cuandoSeIntentaArmarElPromptConUnRecursoInexistente() {
        rechazo = catchThrowable(() -> new PromptTaller(
                new ClassPathResource("ia/no-existe.md"), SamiPruebas.catalogoReal(), KITS));
    }

    private void cuandoSePideLaPeticionConUnPromptMinimo() {
        PromptTaller minimo = new PromptTaller(
                new ByteArrayResource("Sos Sami\n".getBytes()), SamiPruebas.catalogoReal(), KITS);
        peticion = minimo.peticion(consultaSimple());
    }

    // --- entonces ---

    private void entoncesElContextoListaLasPiezasYTerminaConElMensaje() {
        assertThat(peticion.contexto())
                .startsWith("Sistema actual: «Mi almacén» (distribución: mosaic)\n")
                .contains("- id e1, tipo inventory, «Inventario»: Producto, Cantidad\n")
                .contains("- id e2, tipo clients, «Clientes» sección «Aparte»: \n")
                .contains("- id e3, tipo sales, «Ventas»: Producto (vínculo), Cantidad\n")
                .endsWith("Mensaje de la persona: sumá gastos")
                .doesNotContain("Conversación reciente")
                .doesNotContain("Kit");
        assertThat(peticion.instrucciones()).contains("Sos Sami").contains("ofrecer_opciones").contains("usar_kit");
    }

    private void entoncesLasInstruccionesFijanElVocabulario() {
        assertThat(peticion.instrucciones())
                .contains("armás el negocio de la persona")
                .contains("«negocio»", "«pestañas»", "«tablas»")
                .contains("Nunca digas «sistema», «proyecto», «sección» ni «cuaderno»");
    }

    private void entoncesLasInstruccionesTerminanConLosComponentesDelCatalogo() {
        assertThat(peticion.instrucciones())
                .contains("\nComponentes (tipo Nombre): inventory Inventario; orders Pedidos;")
                .contains("; summary Resumen; form Formulario: Nombre, Detalle, Fecha; note Nota; ")
                .endsWith("; exits Salidas.");
    }

    private void entoncesLaFotoViajaYLaLineaDeLaFotoVaAntesDelMensaje() {
        assertThat(peticion.imagenes()).containsExactly(foto());
        assertThat(peticion.contexto())
                .endsWith(PromptTaller.CON_FOTO + "\n" + PromptTaller.MARCA_MENSAJE + " te paso mi cuaderno");
    }

    private void entoncesNoHayImagenesNiLineaDeFoto() {
        assertThat(peticion.imagenes()).isEmpty();
        assertThat(peticion.contexto()).doesNotContain(PromptTaller.CON_FOTO);
    }

    private void entoncesLaPiezaSeMuestraSinId() {
        assertThat(peticion.contexto()).contains("- tipo sales, «Ventas»: Producto (vínculo)\n")
                .doesNotContain("id null");
    }

    private void entoncesLosNombresExistentesVanBajoLaPiezaAntesDeSusRegistros() {
        assertThat(peticion.contexto()).contains(
                "- id e2, tipo clients, «Clientes»: Nombre\n  existentes: Lucía, Marta\n  · r1: Nombre=Marta\n");
    }

    private void entoncesLaLineaDeLaFotoPideUsarLosNombresExistentes() {
        assertThat(PromptTaller.CON_FOTO)
                .contains("existentes")
                .contains("NO cargues productos ni clientes")
                .doesNotContain("productos DISTINTOS");
    }

    private void entoncesLosRegistrosVanBajoSuPiezaConSuId() {
        assertThat(peticion.contexto())
                .contains("- id e1, tipo sales, «Ventas»: Producto\n  · r1: Producto=Vela\n  · r2: Producto=Jabón\n");
    }

    private void entoncesElContextoDiceQueNoHayPiezas() {
        assertThat(peticion.contexto()).contains(PromptTaller.SIN_PIEZAS);
    }

    private void entoncesElContextoListaLosKitsDisponibles() {
        assertThat(peticion.contexto())
                .contains("Kits por rubro (usar_kit): almacen, dietetica, velas, panaderia, viandas, ropa, peluqueria, "
                        + "taller-mecanico, ferreteria, libreria.\n")
                .doesNotContain(PromptTaller.MARCA_KIT);
    }

    private void entoncesElContextoResumeElKitDeVelas() {
        assertThat(peticion.contexto())
                .contains("Kit del rubro: velas (Velas y aromas). Piezas: Velas (inventory +Aroma, Cera, Tamaño); "
                        + "Materiales (form +Cantidad, Unidad, Costo); Ventas (sales +Canal); Quién me debe (clients +Debe)."
                        + " Conexiones: Velas→Materiales.\n")
                .doesNotContain("Kits por rubro");
    }

    private void entoncesNingunContextoNombraKits() {
        assertThat(List.of(peticionConKit.contexto(), peticionSinKit.contexto()))
                .allSatisfy(contexto -> assertThat(contexto)
                        .doesNotContain(PromptTaller.MARCA_KIT)
                        .doesNotContain("Kits por rubro"));
    }

    private void entoncesElKitSeResumeSinConexiones() {
        assertThat(peticion.contexto())
                .contains("Gastos (expenses).\n")
                .doesNotContain("Conexiones");
    }

    private void entoncesLaCharlaIncluyeQuienDijoCadaCosa() {
        assertThat(peticion.contexto())
                .contains("Conversación reciente:\nSami: ¿Sumamos gastos?\nPersona: tal vez\n");
    }

    private void entoncesSeRechazaElArranque() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesLasInstruccionesEmpiezanConElTextoYSigueElCatalogo() {
        assertThat(peticion.instrucciones()).startsWith("Sos Sami\nComponentes");
    }

    private void entoncesLaVersionEsV6() {
        assertThat(PromptTaller.VERSION).isEqualTo("v6");
    }

    // --- datos ---

    private static ConsultaTaller consultaSimple() {
        return new ConsultaTaller("hola", VACIO, List.of());
    }

    private static ImagenTaller foto() {
        return FOTO;
    }
}
