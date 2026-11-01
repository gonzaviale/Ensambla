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
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterpreteRegistrosIaAdapterTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRODUCTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId DETALLE_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");

    private final PromptWhatsappRegistros prompt = new PromptWhatsappRegistros(
            new ClassPathResource("ia/prompt-whatsapp-registros.md"),
            new ClassPathResource("ia/esquema-whatsapp-registros.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();
    private final ConsultaInterpretacion consulta = new ConsultaInterpretacion("gasté 5000 en harina", List.of(
            tabla(GASTOS_ID, "Gastos", new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                    new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()),
                    new CampoConocido(DETALLE_ID, "Total", TipoCampo.FORMULA, List.of())),
            tabla(VENTAS_ID, "Ventas", new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.OPCION,
                    List.of("Torta", "Pan")))), LocalDate.parse("2026-10-27"), null);

    @Test
    @DisplayName("El pedido le habla con códigos cortos, sin ids ni columnas calculadas, e indica la fecha de hoy")
    void pedidoShouldUseShortCodes() {
        PromptWhatsappRegistros.Pedido pedido = prompt.pedido(consulta);

        assertThat(pedido.peticion().contexto()).contains("Hoy es 2026-10-27.", "t1: Mi panadería", "c1 Concepto (TEXTO)",
                "c2 Monto (MONEDA)", "t2: Mi panadería", "c3 Producto (OPCION) opciones: Torta, Pan",
                "gasté 5000 en harina").doesNotContain(GASTOS_ID.toString(), "Total");
        assertThat(pedido.tablas()).containsEntry("t1", GASTOS_ID);
        assertThat(pedido.campos()).hasSize(3);
    }


    @Test
    @DisplayName("Las columnas conectadas a otra tabla se muestran con la tabla a la que apuntan")
    void pedidoShouldShowRelationColumnsWithTheirTable() {
        ConsultaInterpretacion conRelacion = new ConsultaInterpretacion("x", List.of(
                tabla(GASTOS_ID, "Productos", new CampoConocido(CONCEPTO_ID, "Nombre", TipoCampo.TEXTO, List.of())),
                tabla(VENTAS_ID, "Ventas",
                        new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.RELACION, List.of(), GASTOS_ID),
                        new CampoConocido(MONTO_ID, "Cliente", TipoCampo.RELACION, List.of()))),
                LocalDate.parse("2026-10-27"), null);

        assertThat(prompt.pedido(conRelacion).peticion().contexto())
                .contains("c2 Producto (RELACION a Productos)", "c3 Cliente (RELACION)");
    }

    @Test
    @DisplayName("Debajo de cada tabla van los nombres que ya tiene, para que la IA los copie tal cual")
    void pedidoShouldListTheExistingNames() {
        ConsultaInterpretacion conNombres = new ConsultaInterpretacion("x", consulta.tablas(), consulta.hoy(), null,
                Map.of(GASTOS_ID, List.of("Leche La Serenísima 1 L", "Yerba 1 kg")));

        assertThat(prompt.pedido(conNombres).peticion().contexto())
                .contains("ya cargados: Leche La Serenísima 1 L; Yerba 1 kg")
                .containsOnlyOnce("ya cargados");
    }
    @Test
    @DisplayName("Con una tabla ya elegida el pedido lo dice")
    void pedidoShouldMentionTheChosenTable() {
        ConsultaInterpretacion elegida = new ConsultaInterpretacion(consulta.texto(), consulta.tablas(),
                consulta.hoy(), VENTAS_ID);

        assertThat(prompt.pedido(elegida).peticion().contexto()).contains("Tabla elegida: t2");
    }

    @Test
    @DisplayName("Traduce los códigos de la respuesta a los ids de la tabla y las columnas")
    void interpretarShouldTranslateCodes() {
        InterpretacionRegistros result = adapter(respuesta(
                "{\"tabla\":\"t1\",\"confianza\":0.9,\"filas\":[{\"c1\":\"harina\",\"c2\":\"5000\"}]}")).interpretar(consulta);

        assertThat(result.bloqueId()).isEqualTo(GASTOS_ID);
        assertThat(result.filas()).containsExactly(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000"));
        assertThat(result.confianza()).isEqualTo(0.9);
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
        assertThat(pedidos).hasSize(1);
    }

    @Test
    @DisplayName("Descarta las columnas que no son de la tabla elegida y las que la IA inventó")
    void interpretarShouldDropForeignColumns() {
        InterpretacionRegistros result = adapter(respuesta(
                "{\"tabla\":\"t1\",\"confianza\":0.8,\"filas\":[{\"c1\":\"harina\",\"c3\":\"Torta\",\"c9\":\"x\"}]}"))
                .interpretar(consulta);

        assertThat(result.filas()).containsExactly(Map.of(CONCEPTO_ID, "harina"));
    }

    @Test
    @DisplayName("Sin confianza informada se toma como 0")
    void interpretarShouldDefaultConfidenceToZero() {
        InterpretacionRegistros result = adapter(respuesta(
                "{\"tabla\":\"t2\",\"filas\":[{\"c3\":\"Pan\"}]}")).interpretar(consulta);

        assertThat(result.confianza()).isZero();
        assertThat(result.bloqueId()).isEqualTo(VENTAS_ID);
    }

    @Test
    @DisplayName("Una tabla inexistente, nula o una respuesta ilegible vuelven vacías pero con su consumo")
    void interpretarShouldReturnEmptyWithConsumption() {
        for (String json : List.of("{\"tabla\":\"t9\",\"confianza\":0.9,\"filas\":[{\"c1\":\"x\"}]}",
                "{\"tabla\":null,\"confianza\":0.0,\"filas\":[]}", "{\"tabla\":\"t1\",\"confianza\":0.9}",
                "esto no es json", "{\"tabla\":\"t1\",\"filas\":[{\"c1\":3}],\"confianza\":\"alta\"}")) {
            InterpretacionRegistros result = adapter(respuesta(json)).interpretar(consulta);

            assertThat(result.bloqueId()).isNull();
            assertThat(result.filas()).isEmpty();
            assertThat(result.consumo()).isNotNull();
        }
    }

    @Test
    @DisplayName("Si el modelo no responde propaga el error del cliente")
    void interpretarShouldPropagateClientFailure() {
        InterpreteRegistrosIaAdapter adapter = new InterpreteRegistrosIaAdapter(pedido -> {
            throw new IllegalStateException("sin conexión");
        }, prompt, JsonMapper.builder().build());

        assertThatThrownBy(() -> adapter.interpretar(consulta)).hasMessage("sin conexión");
    }

    private InterpreteRegistrosIaAdapter adapter(RespuestaModeloTexto respuesta) {
        return new InterpreteRegistrosIaAdapter(pedido -> {
            pedidos.add(pedido);
            return respuesta;
        }, prompt, JsonMapper.builder().build());
    }

    private static RespuestaModeloTexto respuesta(String json) {
        return new RespuestaModeloTexto(json, "bedrock", "haiku", 100, 20);
    }

    private static TablaDisponible tabla(BloqueId id, String nombre, CampoConocido... campos) {
        return new TablaDisponible(new BloqueLectura(id, nombre, List.of(campos)), "Mi panadería");
    }
}
