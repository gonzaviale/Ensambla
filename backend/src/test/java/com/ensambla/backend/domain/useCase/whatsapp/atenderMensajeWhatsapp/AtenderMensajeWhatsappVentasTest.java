package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lo que Sami hace por WhatsApp con el stock, los nombres que ya existen y las ventas vinculadas. */
class AtenderMensajeWhatsappVentasTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-28T15:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId PRODUCTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000003");
    private static final BloqueId MEDIDAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId STOCK_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId RUBRO_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final CampoId CLIENTE_NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000005");
    private static final CampoId PRODUCTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000006");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000007");
    private static final CampoId CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000008");
    private static final CampoId MEDIDA_ID = CampoId.de("c0000000-0000-4000-8000-000000000009");
    private static final CampoId LITROS_ID = CampoId.de("c0000000-0000-4000-8000-000000000010");
    private static final CampoId SUELTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000011");
    private static final BloqueId PEDIDOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000005");
    private static final CampoId PEDIDO_ID = CampoId.de("c0000000-0000-4000-8000-000000000012");
    private static final CampoId NOTA_ID = CampoId.de("c0000000-0000-4000-8000-000000000013");
    private static final CampoId PEDIDO_NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000014");
    private static final RegistroId LECHE_ID = RegistroId.de("e0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491122334455";

    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class);
    private final PropuestaWhatsappRepository propuestaRepository = mock(PropuestaWhatsappRepository.class);
    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final InterpreteRegistrosIaPort interpreteIa = mock(InterpreteRegistrosIaPort.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeWhatsapp atender = new AtenderMensajeWhatsapp(mensajeRepository, vinculoRepository,
            propuestaRepository, procesamientoRepository, campoRepository, bloqueRepository, registroRepository,
            controlPlanService, consumoSamiRepository, interpreteIa, mock(LectorCuadernoPort.class), mensajeria,
            new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC),
            new PoliticaAtencionWhatsapp(60, 100, 0.6, Duration.ofMinutes(2)));

    private final TablaDisponible productos = new TablaDisponible(new BloqueLectura(PRODUCTOS_ID, "Productos", List.of(
            new CampoConocido(NOMBRE_ID, "Producto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(STOCK_ID, "Cantidad", TipoCampo.NUMERO, List.of()))), "Almacén", TipoBloque.INVENTARIO);
    private final TablaDisponible clientes = new TablaDisponible(new BloqueLectura(CLIENTES_ID, "Clientes", List.of(
            new CampoConocido(CLIENTE_NOMBRE_ID, "Nombre", TipoCampo.TEXTO, List.of()))), "Almacén",
            TipoBloque.CLIENTES);
    private final TablaDisponible ventas = new TablaDisponible(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
            new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.RELACION, List.of(), PRODUCTOS_ID),
            new CampoConocido(CANTIDAD_ID, "Cantidad", TipoCampo.NUMERO, List.of()),
            new CampoConocido(CLIENTE_ID, "Cliente", TipoCampo.RELACION, List.of(), CLIENTES_ID),
            new CampoConocido(MEDIDA_ID, "Medida", TipoCampo.RELACION, List.of(), MEDIDAS_ID),
            new CampoConocido(PEDIDO_ID, "Pedido", TipoCampo.RELACION, List.of(), PEDIDOS_ID),
            new CampoConocido(NOTA_ID, "Nota", TipoCampo.TEXTO, List.of()))), "Almacén", TipoBloque.VENTAS);
    private final TablaDisponible pedidos = new TablaDisponible(new BloqueLectura(PEDIDOS_ID, "Pedidos", List.of(
            new CampoConocido(PEDIDO_NOMBRE_ID, "Pedido", TipoCampo.TEXTO, List.of()))), "Almacén",
            TipoBloque.PEDIDOS);
    private final TablaDisponible medidas = new TablaDisponible(new BloqueLectura(MEDIDAS_ID, "Medidas", List.of(
            new CampoConocido(LITROS_ID, "Litros", TipoCampo.NUMERO, List.of()),
            new CampoConocido(SUELTO_ID, "Suelto", TipoCampo.RELACION, List.of()))), "Almacén");
    private final Registro leche = registro(LECHE_ID, PRODUCTOS_ID, Map.of(NOMBRE_ID.toString(),
            "Leche La Serenísima 1 L", STOCK_ID.toString(), new BigDecimal("5")));
    private final List<Registro> guardados = new ArrayList<>();

    @BeforeEach
    void givenNegocioConProductosClientesYVentas() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA)));
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(productos, clientes, ventas, medidas, pedidos));
        when(consumoSamiRepository.reservar(eq(NEGOCIO_ID), any(), anyInt(), any())).thenReturn(true);
        when(propuestaRepository.save(any(PropuestaWhatsapp.class))).thenAnswer(i -> i.getArgument(0));
        when(mensajeRepository.vencidos(any())).thenReturn(List.of());
        when(registroRepository.findAllByBloqueIdAndNegocioId(PRODUCTOS_ID, NEGOCIO_ID)).thenReturn(List.of(leche));
        when(registroRepository.findByIdAndNegocioId(LECHE_ID, NEGOCIO_ID)).thenReturn(Optional.of(leche));
        when(registroRepository.save(any(Registro.class))).thenAnswer(i -> {
            Registro registro = i.getArgument(0);
            registro.setId(RegistroId.nuevo());
            guardados.add(registro);
            return registro;
        });
    }

    // --- sumar al stock ---

    @Test
    @DisplayName("«Agregá 10» a un producto escrito distinto muestra cómo queda el stock y no crea otro producto")
    void executeShouldProposeToAddToTheStockOfTheExistingProduct() {
        givenTexto("agregá 10 unidades de leche serenisima 1l");
        givenInterpretacion(PRODUCTOS_ID, Map.of(NOMBRE_ID, "leche serenisima 1l", STOCK_ID, "+10"));

        atender.execute();

        FilaPropuesta fila = thenPropuestaGuardada().getFilas().getFirst();
        assertThat(fila.registroId()).isEqualTo(LECHE_ID.toString());
        assertThat(thenRespuesta()).contains("Actualizo *Leche La Serenísima 1 L*: Cantidad: 5 → 15 (+10)");
    }

    @Test
    @DisplayName("Al confirmar, lo que se agrega se suma al stock que había en vez de pisarlo")
    void executeShouldAddToTheStockOnConfirm() {
        givenTexto("sí");
        givenPendiente(PRODUCTOS_ID, List.of(new FilaPropuesta(Map.of(NOMBRE_ID.toString(), "leche",
                STOCK_ID.toString(), "+10"), Set.of()).actualizando(LECHE_ID.toString(), "Leche La Serenísima 1 L",
                NOMBRE_ID.toString(), leche.getValores())));
        givenTablaGuardable(PRODUCTOS_ID, "Productos", null, camposDeProductos());

        atender.execute();

        assertThat(leche.getValores()).containsEntry(STOCK_ID.toString(), new BigDecimal("15"));
        assertThat(thenRespuesta()).contains("actualicé 1 fila en *Productos*");
    }

    @Test
    @DisplayName("La IA recibe los nombres de las tablas de cosas y de las que se vinculan, para copiarlos tal cual")
    void executeShouldGiveTheExistingNamesToTheIa() {
        givenTexto("agregá 10 de leche");
        givenInterpretacion(PRODUCTOS_ID, Map.of(NOMBRE_ID, "leche", STOCK_ID, "+10"));

        atender.execute();

        ArgumentCaptor<ConsultaInterpretacion> consulta = ArgumentCaptor.forClass(ConsultaInterpretacion.class);
        verify(interpreteIa).interpretar(consulta.capture());
        assertThat(consulta.getValue().nombres())
                .containsExactly(Map.entry(PRODUCTOS_ID, List.of("Leche La Serenísima 1 L")));
    }

    // --- ventas vinculadas ---

    @Test
    @DisplayName("Una venta se vincula al producto aunque esté mal escrito y propone sumar el cliente nuevo")
    void executeShouldLinkTheSaleToItsProductAndCustomer() {
        givenTexto("vendí 2 lechee serenisima a Marta y 1 sin producto");
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(VENTAS_ID, List.of(
                Map.of(PRODUCTO_ID, "lechee serenisima", CANTIDAD_ID, "2", CLIENTE_ID, "Marta"),
                Map.of(CANTIDAD_ID, "1")), 0.9, consumo()));

        atender.execute();

        FilaPropuesta venta = thenPropuestaGuardada().getFilas().getFirst();
        assertThat(venta.vinculos().get(PRODUCTO_ID.toString()).registroId()).isEqualTo(LECHE_ID.toString());
        assertThat(venta.vinculos().get(CLIENTE_ID.toString()).esNuevo()).isTrue();
        assertThat(thenRespuesta()).contains("Producto: Leche La Serenísima 1 L",
                "Cliente: Marta (nuevo, lo sumo a Clientes)");
    }

    @Test
    @DisplayName("Un vínculo a una tabla sin nombre para sus filas, o que no está, no se carga")
    void executeShouldDropLinksThatCannotBeResolved() {
        givenTexto("vendí 2 leches en botella");
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(VENTAS_ID, List.of(
                Map.of(PRODUCTO_ID, "leche", CANTIDAD_ID, "2", MEDIDA_ID, "botella")), 0.9, consumo()));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().valores()).doesNotContainKey(MEDIDA_ID.toString())
                .containsKey(PRODUCTO_ID.toString());
    }

    @Test
    @DisplayName("Al confirmar una venta se suma el cliente nuevo una sola vez y se descuenta el stock hasta 0")
    void executeShouldCreateTheNewCustomerAndMoveTheStockOnConfirm() {
        givenTexto("sí");
        VinculoPropuesto lecheVinculada = new VinculoPropuesto("Leche La Serenísima 1 L", LECHE_ID.toString(),
                PRODUCTOS_ID.toString(), "Productos");
        givenPendiente(VENTAS_ID, List.of(
                venta("2", lecheVinculada, new VinculoPropuesto("Marta", null, CLIENTES_ID.toString(), "Clientes")),
                venta("4", lecheVinculada, new VinculoPropuesto("marta", null, CLIENTES_ID.toString(), "Clientes"))));
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) -1, camposDeVentas(true));
        when(campoRepository.findByBloqueIdAndNegocioId(CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of(
                campo(CLIENTE_NOMBRE_ID, TipoCampo.TEXTO, null)));
        when(campoRepository.findByBloqueIdAndNegocioId(PRODUCTOS_ID, NEGOCIO_ID)).thenReturn(camposDeProductos());

        atender.execute();

        assertThat(guardados).singleElement().satisfies(cliente -> {
            assertThat(cliente.getBloqueId()).isEqualTo(CLIENTES_ID);
            assertThat(cliente.getValores()).containsEntry(CLIENTE_NOMBRE_ID.toString(), "Marta");
        });
        assertThat(leche.getValores()).containsEntry(STOCK_ID.toString(), BigDecimal.ZERO);
        verify(registroRepository).saveAll(List.of(leche));
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 2, AHORA);
        assertThat(thenRespuesta()).contains("guardé 2 filas en *Ventas*", "También sumé Marta en Clientes.",
                "Stock de Leche La Serenísima 1 L: 5 → 3.",
                "Stock de Leche La Serenísima 1 L: 3 → 0. No alcanzaba para 4: lo dejé en 0.");
    }

    @Test
    @DisplayName("Si el producto vinculado ya no está en su tabla, se suma de nuevo con sus valores iniciales")
    void executeShouldRecreateAProductThatIsNoLongerThere() {
        givenTexto("sí");
        Registro deOtraTabla = registro(LECHE_ID, CLIENTES_ID, Map.of());
        when(registroRepository.findByIdAndNegocioId(LECHE_ID, NEGOCIO_ID)).thenReturn(Optional.of(deOtraTabla));
        givenPendiente(VENTAS_ID, List.of(venta("1", new VinculoPropuesto("Leche", LECHE_ID.toString(),
                PRODUCTOS_ID.toString(), "Productos"), null)));
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) -1, camposDeVentas(true));
        when(campoRepository.findByBloqueIdAndNegocioId(PRODUCTOS_ID, NEGOCIO_ID)).thenReturn(camposDeProductos());

        atender.execute();

        assertThat(guardados).singleElement().satisfies(producto -> assertThat(producto.getValores())
                .containsEntry(NOMBRE_ID.toString(), "Leche")
                .containsEntry(STOCK_ID.toString(), BigDecimal.ZERO)
                .containsEntry(PRECIO_ID.toString(), new BigDecimal("100"))
                .doesNotContainKey(RUBRO_ID.toString()));
        verify(registroRepository, never()).saveAll(List.of(deOtraTabla));
        assertThat(thenRespuesta()).contains("También sumé Leche en Productos.").doesNotContain("Stock de");
    }

    @Test
    @DisplayName("Un vínculo nuevo a una tabla sin columna de texto no se puede sumar y no guarda nada")
    void executeShouldRejectANewLinkToATableWithoutName() {
        givenTexto("sí");
        givenPendiente(VENTAS_ID, List.of(venta("1", new VinculoPropuesto("botella", null, MEDIDAS_ID.toString(),
                "Medidas"), null)));
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) -1, camposDeVentas(false));
        when(campoRepository.findByBloqueIdAndNegocioId(MEDIDAS_ID, NEGOCIO_ID)).thenReturn(List.of(
                campo(LITROS_ID, TipoCampo.NUMERO, null)));

        atender.execute();

        assertThat(guardados).isEmpty();
        verify(registroRepository, never()).saveAll(anyList());
        assertThat(thenRespuesta()).contains(TextosWhatsapp.TABLA_INEXISTENTE);
    }

    // --- cuándo no se mueve el stock ---

    @Test
    @DisplayName("Una tabla de movimientos sin producto vinculado o sin cantidad no mueve el stock")
    void executeShouldNotMoveTheStockWithoutProductOrQuantity() {
        givenTexto("sí");
        givenPendiente(VENTAS_ID, List.of(new FilaPropuesta(Map.of(CANTIDAD_ID.toString(), "2"), Set.of())));
        Campo productoComoTexto = campo(PRODUCTO_ID, TipoCampo.TEXTO, "product");
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) -1, List.of(productoComoTexto,
                campo(CANTIDAD_ID, TipoCampo.NUMERO, "quantity")));

        atender.execute();

        verify(registroRepository, never()).findByIdAndNegocioId(any(), any());
        assertThat(thenRespuesta()).doesNotContain("Stock de");
    }

    @Test
    @DisplayName("Sin la columna de cantidad tampoco se mueve el stock")
    void executeShouldNotMoveTheStockWithoutAQuantityColumn() {
        givenTexto("sí");
        givenPendiente(VENTAS_ID, List.of(new FilaPropuesta(Map.of(), Set.of())));
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) -1, List.of(campo(PRODUCTO_ID, TipoCampo.RELACION,
                "product")));

        atender.execute();

        assertThat(thenRespuesta()).contains("guardé 1 fila en *Ventas*").doesNotContain("Stock de");
    }

    @Test
    @DisplayName("Una venta sin producto o sin cantidad, o de un producto sin stock ni nombre, no mueve nada")
    void executeShouldSkipRowsThatCannotMoveTheStock() {
        givenTexto("sí");
        VinculoPropuesto lecheVinculada = new VinculoPropuesto("Leche", LECHE_ID.toString(), PRODUCTOS_ID.toString(),
                "Productos");
        givenPendiente(VENTAS_ID, List.of(
                new FilaPropuesta(Map.of(CANTIDAD_ID.toString(), "2"), Set.of()),
                new FilaPropuesta(Map.of(PRODUCTO_ID.toString(), "leche"), Set.of())
                        .vinculando(PRODUCTO_ID.toString(), lecheVinculada),
                venta("1", lecheVinculada, null)));
        givenTablaGuardable(VENTAS_ID, "Ventas", (short) 1, camposDeVentas(false));
        when(campoRepository.findByBloqueIdAndNegocioId(PRODUCTOS_ID, NEGOCIO_ID)).thenReturn(List.of(
                campo(NOMBRE_ID, TipoCampo.TEXTO, null)));

        atender.execute();

        assertThat(leche.getValores()).containsEntry(STOCK_ID.toString(), new BigDecimal("5"));
        verify(registroRepository, times(1)).saveAll(anyList());
        assertThat(thenRespuesta()).contains("guardé 3 filas en *Ventas*").doesNotContain("Stock de");
    }

    @Test
    @DisplayName("Una entrada suma al stock y nombra al producto aunque su tabla no tenga columna de texto")
    void executeShouldAddEntriesToTheStock() {
        givenTexto("sí");
        givenPendiente(VENTAS_ID, List.of(venta("3", new VinculoPropuesto("Leche", LECHE_ID.toString(),
                PRODUCTOS_ID.toString(), "Productos"), null)));
        givenTablaGuardable(VENTAS_ID, "Entradas", (short) 1, camposDeVentas(false));
        when(campoRepository.findByBloqueIdAndNegocioId(PRODUCTOS_ID, NEGOCIO_ID)).thenReturn(List.of(
                campo(STOCK_ID, TipoCampo.NUMERO, "quantity")));

        atender.execute();

        assertThat(leche.getValores()).containsEntry(STOCK_ID.toString(), new BigDecimal("8"));
        assertThat(thenRespuesta()).contains("Stock de el producto: 5 → 8.");
    }

    // --- arrange / assert ---

    private void givenTexto(String texto) {
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(MensajeWhatsapp.builder()
                .negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("T1").tipo(TipoMensajeWhatsapp.TEXTO)
                .texto(texto).recibidoEn(AHORA).build()));
    }

    private void givenInterpretacion(BloqueId bloqueId, Map<CampoId, String> fila) {
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(bloqueId, List.of(fila), 0.9,
                consumo()));
    }

    private void givenPendiente(BloqueId bloqueId, List<FilaPropuesta> filas) {
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(PropuestaWhatsapp.pendiente(
                NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO, "x", bloqueId, filas, AHORA)));
    }

    private void givenTablaGuardable(BloqueId bloqueId, String nombre, Short direccionStock, List<Campo> campos) {
        when(bloqueRepository.findByIdAndNegocioId(bloqueId, NEGOCIO_ID)).thenReturn(Optional.of(Bloque.builder()
                .id(bloqueId).negocioId(NEGOCIO_ID).nombre(nombre).direccionStock(direccionStock).build()));
        when(campoRepository.findByBloqueIdAndNegocioId(bloqueId, NEGOCIO_ID)).thenReturn(campos);
        when(registroRepository.siguienteOrden(any(), eq(NEGOCIO_ID))).thenReturn(0);
    }

    private static FilaPropuesta venta(String cantidad, VinculoPropuesto producto, VinculoPropuesto cliente) {
        Map<String, String> valores = new LinkedHashMap<>(Map.of(PRODUCTO_ID.toString(), producto.nombre(),
                CANTIDAD_ID.toString(), cantidad));
        FilaPropuesta fila = new FilaPropuesta(valores, Set.of());
        if (cliente != null) {
            valores.put(CLIENTE_ID.toString(), cliente.nombre());
            fila = new FilaPropuesta(valores, Set.of()).vinculando(CLIENTE_ID.toString(), cliente);
        }
        return fila.vinculando(PRODUCTO_ID.toString(), producto);
    }

    private static List<Campo> camposDeProductos() {
        Campo stock = campo(STOCK_ID, TipoCampo.NUMERO, "quantity");
        stock.setObligatorio(true);
        Campo precio = campo(PRECIO_ID, TipoCampo.MONEDA, "price");
        precio.setValorInicial("100");
        Campo rubro = campo(RUBRO_ID, TipoCampo.TEXTO, null);
        rubro.setValorInicial(" ");
        Campo nombre = campo(NOMBRE_ID, TipoCampo.TEXTO, "name");
        nombre.setObligatorio(true);
        return List.of(nombre, stock, precio, rubro);
    }

    private static List<Campo> camposDeVentas(boolean conCliente) {
        List<Campo> campos = new ArrayList<>(List.of(campo(PRODUCTO_ID, TipoCampo.RELACION, "product"),
                campo(CANTIDAD_ID, TipoCampo.NUMERO, "quantity")));
        if (conCliente) {
            campos.add(campo(CLIENTE_ID, TipoCampo.RELACION, "customer"));
        }
        return campos;
    }

    private static Campo campo(CampoId id, TipoCampo tipo, String clave) {
        return Campo.builder().id(id).nombre("Campo").tipo(tipo).clave(clave).opciones(List.of())
                .posicion((short) 0).build();
    }

    private static Registro registro(RegistroId id, BloqueId bloqueId, Map<String, Object> valores) {
        Registro registro = Registro.nuevo(NEGOCIO_ID, bloqueId, new HashMap<>(valores), AHORA);
        registro.setId(id);
        return registro;
    }

    private static ConsumoIaSami consumo() {
        return new ConsumoIaSami("simulado", "demo", 10, 20);
    }

    private PropuestaWhatsapp thenPropuestaGuardada() {
        ArgumentCaptor<PropuestaWhatsapp> captor = ArgumentCaptor.forClass(PropuestaWhatsapp.class);
        verify(propuestaRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private String thenRespuesta() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mensajeria, atLeastOnce()).enviarTexto(eq(TELEFONO), captor.capture());
        return String.join("\n", captor.getAllValues());
    }
}
