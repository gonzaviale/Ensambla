package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeWhatsappColaTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final BloqueId TABLA = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId SIN_TABLA = null;
    private static final List<Map<CampoId, String>> UNA_FILA = List.of(Map.of(CampoId.nuevo(), "x"));
    private static final double CONFIANZA_MINIMA = 0.6;

    private MensajeWhatsapp mensaje;

    @Test
    @DisplayName("Reclamar un mensaje lo deja atendiéndose, cuenta el intento y fija hasta cuándo es de quien lo tomó")
    void reclamarUnMensajeLoDejaAtendiendose() {
        dadoUnMensajeRecibido();

        cuandoSeReclamaPor(Duration.ofMinutes(2));

        entoncesQuedaEn(EstadoMensajeWhatsapp.ATENDIENDO);
        entoncesLlevaIntentos(1);
        entoncesEsDeQuienLoTomoHasta(AHORA.plusMinutes(2));
    }

    @Test
    @DisplayName("Atender lo cierra con la hora y limpia un error anterior")
    void atenderLoCierraYLimpiaElError() {
        dadoUnMensajeRecibido();
        dadoQueFallo("antes falló");

        cuandoSeAtiende(AHORA.plusMinutes(1));

        entoncesQuedaEn(EstadoMensajeWhatsapp.ATENDIDO);
        entoncesSeCerroEl(AHORA.plusMinutes(1));
        entoncesNoTieneError();
    }

    @Test
    @DisplayName("Fallar lo deja fallido con el motivo y la hora")
    void fallarLoDejaFallidoConElMotivo() {
        dadoUnMensajeRecibido();

        cuandoFalla("se cayó el proveedor");

        entoncesQuedaEn(EstadoMensajeWhatsapp.FALLIDO);
        entoncesElErrorEs("se cayó el proveedor");
        entoncesSeCerroEl(AHORA);
    }

    @Test
    @DisplayName("La interpretación con tabla, filas y la confianza justa está entendida")
    void laInterpretacionConLaConfianzaJustaEstaEntendida() {
        entoncesEstaEntendida(new InterpretacionRegistros(TABLA, UNA_FILA, 0.6, null), true);
    }

    @Test
    @DisplayName("La interpretación con menos confianza de la necesaria no está entendida")
    void laInterpretacionConPocaConfianzaNoEstaEntendida() {
        entoncesEstaEntendida(new InterpretacionRegistros(TABLA, UNA_FILA, 0.59, null), false);
    }

    @Test
    @DisplayName("La interpretación sin tabla no está entendida")
    void laInterpretacionSinTablaNoEstaEntendida() {
        entoncesEstaEntendida(new InterpretacionRegistros(SIN_TABLA, UNA_FILA, 0.9, null), false);
    }

    @Test
    @DisplayName("La interpretación sin filas no está entendida")
    void laInterpretacionSinFilasNoEstaEntendida() {
        entoncesEstaEntendida(new InterpretacionRegistros(TABLA, List.of(), 0.9, null), false);
    }

    @Test
    @DisplayName("Una interpretación sin resultado no está entendida ni aunque no se pida confianza")
    void unaInterpretacionSinResultadoNuncaEstaEntendida() {
        entoncesNoEstaEntendidaNiSinPedirConfianza(InterpretacionRegistros.sinResultado(null));
    }

    private void dadoUnMensajeRecibido() {
        mensaje = MensajeWhatsapp.recibido(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
                new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.TEXTO, "hola", null, null, AHORA));
    }

    private void dadoQueFallo(String motivo) {
        mensaje.fallar(motivo, AHORA);
    }

    private void cuandoSeReclamaPor(Duration plazo) {
        mensaje.reclamar(AHORA, plazo);
    }

    private void cuandoSeAtiende(OffsetDateTime momento) {
        mensaje.atender(momento);
    }

    private void cuandoFalla(String motivo) {
        mensaje.fallar(motivo, AHORA);
    }

    private void entoncesQuedaEn(EstadoMensajeWhatsapp estado) {
        assertThat(mensaje.getEstado()).isEqualTo(estado);
    }

    private void entoncesLlevaIntentos(int intentos) {
        assertThat(mensaje.getIntentos()).isEqualTo(intentos);
    }

    private void entoncesEsDeQuienLoTomoHasta(OffsetDateTime momento) {
        assertThat(mensaje.getReclamadoHasta()).isEqualTo(momento);
    }

    private void entoncesSeCerroEl(OffsetDateTime momento) {
        assertThat(mensaje.getAtendidoEn()).isEqualTo(momento);
    }

    private void entoncesNoTieneError() {
        assertThat(mensaje.getError()).isNull();
    }

    private void entoncesElErrorEs(String motivo) {
        assertThat(mensaje.getError()).isEqualTo(motivo);
    }

    private static void entoncesEstaEntendida(InterpretacionRegistros interpretacion, boolean entendida) {
        assertThat(interpretacion.entendida(CONFIANZA_MINIMA)).isEqualTo(entendida);
    }

    private static void entoncesNoEstaEntendidaNiSinPedirConfianza(InterpretacionRegistros interpretacion) {
        assertThat(interpretacion.entendida(0.0)).isFalse();
    }
}
