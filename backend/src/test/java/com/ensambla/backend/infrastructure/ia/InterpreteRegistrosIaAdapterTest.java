package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InterpreteRegistrosIaAdapterTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRODUCTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId DETALLE_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final LocalDate HOY = LocalDate.parse("2026-10-27");

    private final PromptWhatsappRegistros prompt = new PromptWhatsappRegistros(
            new ClassPathResource("ia/prompt-whatsapp-registros.md"),
            new ClassPathResource("ia/esquema-whatsapp-registros.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();
    private final ConsultaInterpretacion consulta = new ConsultaInterpretacion("gasté 5000 en harina", List.of(
            tabla(GASTOS_ID, "Gastos", new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                    new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()),
                    new CampoConocido(DETALLE_ID, "Total", TipoCampo.FORMULA, List.of())),
            tabla(VENTAS_ID, "Ventas", new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.OPCION,
                    List.of("Torta", "Pan")))), HOY, null);

    private ConsultaInterpretacion consultaAArmar = consulta;
    private RespuestaModeloTexto respuestaDelModelo;
    private PromptWhatsappRegistros.Pedido pedido;
    private InterpretacionRegistros interpretacion;
    private Throwable rechazo;

    @Test
    @DisplayName("El pedido le habla con códigos cortos, sin ids ni columnas calculadas, e indica la fecha de hoy")
    void elPedidoLeHablaConCodigosCortosSinIdsNiColumnasCalculadasEIndicaLaFechaDeHoy() {
        cuandoSeArmaElPedido();

        entoncesElPedidoUsaCodigosCortosSinIdsNiColumnasCalculadas();
    }

    @Test
    @DisplayName("Las columnas conectadas a otra tabla se muestran con la tabla a la que apuntan")
    void lasColumnasConectadasAOtraTablaSeMuestranConLaTablaALaQueApuntan() {
        dadoUnaConsultaConColumnasConectadasAOtraTabla();

        cuandoSeArmaElPedido();

        entoncesLasRelacionesSeMuestranConSuTabla();
    }

    @Test
    @DisplayName("Debajo de cada tabla van los nombres que ya tiene, para que la IA los copie tal cual")
    void debajoDeCadaTablaVanLosNombresQueYaTieneParaQueLaIaLosCopieTalCual() {
        dadoUnaConsultaConNombresYaCargadosEnGastos();

        cuandoSeArmaElPedido();

        entoncesElPedidoListaLosNombresExistentesUnaSolaVez();
    }

    @Test
    @DisplayName("Con una tabla ya elegida el pedido lo dice")
    void conUnaTablaYaElegidaElPedidoLoDice() {
        dadoUnaConsultaConLaTablaDeVentasElegida();

        cuandoSeArmaElPedido();

        entoncesElPedidoMencionaLaTablaElegida();
    }

    @Test
    @DisplayName("Traduce los códigos de la respuesta a los ids de la tabla y las columnas")
    void traduceLosCodigosDeLaRespuestaALosIdsDeLaTablaYLasColumnas() {
        dadoQueElModeloResponde("{\"tabla\":\"t1\",\"confianza\":0.9,\"filas\":[{\"c1\":\"harina\",\"c2\":\"5000\"}]}");

        cuandoSeInterpreta();

        entoncesSeTraducenLosCodigosALosIds();
    }

    @Test
    @DisplayName("Descarta las columnas que no son de la tabla elegida y las que la IA inventó")
    void descartaLasColumnasQueNoSonDeLaTablaElegidaYLasQueLaIaInvento() {
        dadoQueElModeloResponde(
                "{\"tabla\":\"t1\",\"confianza\":0.8,\"filas\":[{\"c1\":\"harina\",\"c3\":\"Torta\",\"c9\":\"x\"}]}");

        cuandoSeInterpreta();

        entoncesSoloQuedaLaColumnaDeLaTablaElegida();
    }

    @Test
    @DisplayName("Sin confianza informada se toma como 0")
    void sinConfianzaInformadaSeTomaComoCero() {
        dadoQueElModeloResponde("{\"tabla\":\"t2\",\"filas\":[{\"c3\":\"Pan\"}]}");

        cuandoSeInterpreta();

        entoncesLaConfianzaEsCeroYLaTablaEsVentas();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"tabla\":\"t9\",\"confianza\":0.9,\"filas\":[{\"c1\":\"x\"}]}",
            "{\"tabla\":null,\"confianza\":0.0,\"filas\":[]}",
            "{\"tabla\":\"t1\",\"confianza\":0.9}",
            "esto no es json",
            "{\"tabla\":\"t1\",\"filas\":[{\"c1\":3}],\"confianza\":\"alta\"}"})
    @DisplayName("Una tabla inexistente, nula o una respuesta ilegible vuelven vacías pero con su consumo")
    void unaTablaInexistenteNulaOUnaRespuestaIlegibleVuelvenVaciasPeroConSuConsumo(String json) {
        dadoQueElModeloResponde(json);

        cuandoSeInterpreta();

        entoncesVuelveVaciaPeroConSuConsumo();
    }

    @Test
    @DisplayName("Si el modelo no responde propaga el error del cliente")
    void siElModeloNoRespondePropagaElErrorDelCliente() {
        cuandoSeInterpretaConUnModeloSinConexion();

        entoncesSePropagaElErrorDeSinConexion();
    }

    // --- dado ---

    private void dadoUnaConsultaConColumnasConectadasAOtraTabla() {
        consultaAArmar = new ConsultaInterpretacion("x", List.of(
                tabla(GASTOS_ID, "Productos", new CampoConocido(CONCEPTO_ID, "Nombre", TipoCampo.TEXTO, List.of())),
                tabla(VENTAS_ID, "Ventas",
                        new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.RELACION, List.of(), GASTOS_ID),
                        new CampoConocido(MONTO_ID, "Cliente", TipoCampo.RELACION, List.of()))),
                HOY, null);
    }

    private void dadoUnaConsultaConNombresYaCargadosEnGastos() {
        consultaAArmar = new ConsultaInterpretacion("x", consulta.tablas(), consulta.hoy(), null,
                Map.of(GASTOS_ID, List.of("Leche La Serenísima 1 L", "Yerba 1 kg")));
    }

    private void dadoUnaConsultaConLaTablaDeVentasElegida() {
        consultaAArmar = new ConsultaInterpretacion(consulta.texto(), consulta.tablas(), consulta.hoy(), VENTAS_ID);
    }

    private void dadoQueElModeloResponde(String json) {
        respuestaDelModelo = new RespuestaModeloTexto(json, "bedrock", "haiku", 100, 20);
    }

    // --- cuando ---

    private void cuandoSeArmaElPedido() {
        pedido = prompt.pedido(consultaAArmar);
    }

    private void cuandoSeInterpreta() {
        interpretacion = new InterpreteRegistrosIaAdapter(solicitud -> {
            pedidos.add(solicitud);
            return respuestaDelModelo;
        }, prompt, JsonMapper.builder().build()).interpretar(consulta);
    }

    private void cuandoSeInterpretaConUnModeloSinConexion() {
        InterpreteRegistrosIaAdapter adapter = new InterpreteRegistrosIaAdapter(solicitud -> {
            throw new IllegalStateException("sin conexión");
        }, prompt, JsonMapper.builder().build());
        rechazo = catchThrowable(() -> adapter.interpretar(consulta));
    }

    // --- entonces ---

    private void entoncesElPedidoUsaCodigosCortosSinIdsNiColumnasCalculadas() {
        assertThat(pedido.peticion().contexto()).contains("Hoy es 2026-10-27.", "t1: Mi panadería",
                "c1 Concepto (TEXTO)", "c2 Monto (MONEDA)", "t2: Mi panadería",
                "c3 Producto (OPCION) opciones: Torta, Pan", "gasté 5000 en harina")
                .doesNotContain(GASTOS_ID.toString(), "Total");
        assertThat(pedido.tablas()).containsEntry("t1", GASTOS_ID);
        assertThat(pedido.campos()).hasSize(3);
    }

    private void entoncesLasRelacionesSeMuestranConSuTabla() {
        assertThat(pedido.peticion().contexto())
                .contains("c2 Producto (RELACION a Productos)", "c3 Cliente (RELACION)");
    }

    private void entoncesElPedidoListaLosNombresExistentesUnaSolaVez() {
        assertThat(pedido.peticion().contexto())
                .contains("ya cargados: Leche La Serenísima 1 L; Yerba 1 kg")
                .containsOnlyOnce("ya cargados");
    }

    private void entoncesElPedidoMencionaLaTablaElegida() {
        assertThat(pedido.peticion().contexto()).contains("Tabla elegida: t2");
    }

    private void entoncesSeTraducenLosCodigosALosIds() {
        assertThat(interpretacion.bloqueId()).isEqualTo(GASTOS_ID);
        assertThat(interpretacion.filas()).containsExactly(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000"));
        assertThat(interpretacion.confianza()).isEqualTo(0.9);
        assertThat(interpretacion.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
        assertThat(pedidos).hasSize(1);
    }

    private void entoncesSoloQuedaLaColumnaDeLaTablaElegida() {
        assertThat(interpretacion.filas()).containsExactly(Map.of(CONCEPTO_ID, "harina"));
    }

    private void entoncesLaConfianzaEsCeroYLaTablaEsVentas() {
        assertThat(interpretacion.confianza()).isZero();
        assertThat(interpretacion.bloqueId()).isEqualTo(VENTAS_ID);
    }

    private void entoncesVuelveVaciaPeroConSuConsumo() {
        assertThat(interpretacion.bloqueId()).isNull();
        assertThat(interpretacion.filas()).isEmpty();
        assertThat(interpretacion.consumo()).isNotNull();
    }

    private void entoncesSePropagaElErrorDeSinConexion() {
        assertThat(rechazo).hasMessage("sin conexión");
    }

    // --- datos ---

    private static TablaDisponible tabla(BloqueId id, String nombre, CampoConocido... campos) {
        return new TablaDisponible(new BloqueLectura(id, nombre, List.of(campos)), "Mi panadería");
    }
}
