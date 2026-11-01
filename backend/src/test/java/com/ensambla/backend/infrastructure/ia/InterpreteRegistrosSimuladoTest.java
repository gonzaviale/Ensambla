package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpreteRegistrosSimuladoTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId FECHA_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId OTRO_MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final LocalDate HOY = LocalDate.parse("2026-10-27");

    private final InterpreteRegistrosSimulado interprete = new InterpreteRegistrosSimulado();
    private final TablaDisponible gastos = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(FECHA_ID, "Fecha", TipoCampo.FECHA, List.of()),
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()),
            new CampoConocido(OTRO_MONTO_ID, "Saldo", TipoCampo.NUMERO, List.of()))), "Mi panadería");
    private final TablaDisponible ventas = new TablaDisponible(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
            new CampoConocido(CONCEPTO_ID, "Producto", TipoCampo.TEXTO, List.of()))), "Mi panadería");

    @Test
    @DisplayName("Con una sola tabla la elige, pone el primer número en la primera columna numérica y la fecha de hoy")
    void interpretarShouldFillTheOnlyTable() {
        InterpretacionRegistros result = interprete.interpretar(consulta("gasté 5000,50 en harina", null, gastos));

        assertThat(result.bloqueId()).isEqualTo(GASTOS_ID);
        assertThat(result.confianza()).isEqualTo(0.9);
        assertThat(result.filas()).hasSize(1);
        assertThat(result.filas().getFirst()).containsEntry(MONTO_ID, "5000.50")
                .containsEntry(FECHA_ID, "2026-10-27").containsEntry(CONCEPTO_ID, "gasté  en harina")
                .doesNotContainKey(OTRO_MONTO_ID);
    }

    @Test
    @DisplayName("Con varias tablas elige la que el texto nombra")
    void interpretarShouldPickTheNamedTable() {
        assertThat(interprete.interpretar(consulta("anoté las ventas de hoy", null, gastos, ventas)).bloqueId())
                .isEqualTo(VENTAS_ID);
    }

    @Test
    @DisplayName("Con la tabla ya elegida usa esa aunque el texto nombre otra")
    void interpretarShouldUseTheChosenTable() {
        assertThat(interprete.interpretar(consulta("gastos de hoy", VENTAS_ID, gastos, ventas)).bloqueId())
                .isEqualTo(VENTAS_ID);
        assertThat(interprete.interpretar(consulta("hola", BloqueId.nuevo(), gastos, ventas)).bloqueId()).isNull();
    }

    @Test
    @DisplayName("Con varias tablas y ninguna nombrada no se arriesga: vuelve vacía")
    void interpretarShouldGiveUpWhenAmbiguous() {
        InterpretacionRegistros result = interprete.interpretar(consulta("5000 de harina", null, gastos, ventas));

        assertThat(result.bloqueId()).isNull();
        assertThat(result.filas()).isEmpty();
    }

    @Test
    @DisplayName("Con columnas de texto largo y numéricas completa la primera de cada clase y salta las demás")
    void interpretarShouldHandleOtherColumnTypes() {
        TablaDisponible tabla = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Notas", List.of(
                new CampoConocido(CONCEPTO_ID, "Detalle", TipoCampo.TEXTO_LARGO, List.of()),
                new CampoConocido(MONTO_ID, "Cantidad", TipoCampo.NUMERO, List.of()),
                new CampoConocido(OTRO_MONTO_ID, "Estado", TipoCampo.OPCION, List.of("Ok")))), "Mi panadería");

        InterpretacionRegistros result = interprete.interpretar(consulta("tres cosas", null, tabla));

        assertThat(result.filas().getFirst()).containsOnlyKeys(CONCEPTO_ID);
    }

    @Test
    @DisplayName("Sin números ni texto sobrante deja las columnas sin completar")
    void interpretarShouldLeaveEmptyColumnsOut() {
        InterpretacionRegistros result = interprete.interpretar(consulta("   ", null, ventas));

        assertThat(result.filas().getFirst()).isEmpty();
    }

    private static ConsultaInterpretacion consulta(String texto, BloqueId destino, TablaDisponible... tablas) {
        return new ConsultaInterpretacion(texto, List.of(tablas), HOY, destino);
    }
}
