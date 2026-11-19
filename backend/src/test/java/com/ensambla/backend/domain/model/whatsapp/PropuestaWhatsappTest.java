package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PropuestaWhatsappTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId GASTOS = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final FilaPropuesta FILA = new FilaPropuesta(Map.of("c1", "5000"), Set.of());

    private PropuestaWhatsapp propuesta;
    private BloqueId elegida;
    private Throwable error;

    @Test
    @DisplayName("Pendiente nace abierta y con su tabla")
    void pendienteNaceAbiertaYConSuTabla() {
        dadoUnaCargaPendiente();

        entoncesEstaAbierta(true);
        entoncesEsDeLaTabla(GASTOS);
    }

    @Test
    @DisplayName("Pendiente sigue vigente a los 29 minutos y vence a los 30")
    void pendienteVenceALosTreintaMinutos() {
        dadoUnaCargaPendiente();

        entoncesEstaVigenteA(AHORA.plusMinutes(29), true);
        entoncesEstaVigenteA(AHORA.plusMinutes(30), false);
    }

    @Test
    @DisplayName("Una propuesta ya cerrada no está vigente aunque no haya pasado el tiempo")
    void unaPropuestaCerradaNoEstaVigente() {
        dadoUnaCargaPendiente();
        dadoQueSeDescarto();

        entoncesEstaVigenteA(AHORA.plusMinutes(1), false);
    }

    @Test
    @DisplayName("Elegir 1 devuelve la primera tabla de la lista")
    void elegirUnoDevuelveLaPrimeraTabla() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoSeElige(1);

        entoncesSeEligio(GASTOS);
    }

    @Test
    @DisplayName("Elegir 2 devuelve la segunda tabla de la lista")
    void elegirDosDevuelveLaSegundaTabla() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoSeElige(2);

        entoncesSeEligio(VENTAS);
    }

    @Test
    @DisplayName("Elegir 0 pide elegir un número de la lista")
    void elegirCeroPideElegirUnNumeroDeLaLista() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoSeElige(0);

        entoncesSeRechazaPorque("Elegí un número de la lista");
    }

    @Test
    @DisplayName("Elegir un número más grande que la lista se rechaza")
    void elegirUnNumeroMasGrandeQueLaListaSeRechaza() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoSeElige(3);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Elegir en una carga que no tiene lista se rechaza")
    void elegirEnUnaCargaSinListaSeRechaza() {
        dadoUnaCargaPendiente();

        cuandoSeElige(1);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Pasar a pendiente guarda la tabla elegida y las filas")
    void pasarAPendienteGuardaLaTablaYLasFilas() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoPasaAPendienteEn(VENTAS);

        entoncesQuedaEn(EstadoPropuestaWhatsapp.PENDIENTE);
        entoncesEsDeLaTabla(VENTAS);
        entoncesTieneLasFilas(FILA);
    }

    @Test
    @DisplayName("Confirmar cierra la propuesta con la hora")
    void confirmarCierraLaPropuestaConLaHora() {
        dadoUnaCargaPendiente();

        cuandoSeConfirma();

        entoncesQuedaEn(EstadoPropuestaWhatsapp.CONFIRMADA);
        entoncesSeCerroEl(AHORA);
        entoncesEstaAbierta(false);
    }

    @Test
    @DisplayName("Descartar cierra la propuesta como descartada")
    void descartarCierraLaPropuestaComoDescartada() {
        dadoUnaCargaPendiente();

        dadoQueSeDescarto();

        entoncesQuedaEn(EstadoPropuestaWhatsapp.DESCARTADA);
    }

    @Test
    @DisplayName("Vencer cierra la propuesta como vencida")
    void vencerCierraLaPropuestaComoVencida() {
        dadoUnaCargaPendiente();

        cuandoVence();

        entoncesQuedaEn(EstadoPropuestaWhatsapp.VENCIDA);
    }

    @Test
    @DisplayName("No se puede confirmar una carga que todavía espera que se elija la tabla")
    void noSePuedeConfirmarSinElegirLaTabla() {
        dadoUnaCargaEsperandoLaTabla();

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaPorque("Primero elegí la tabla respondiendo con su número");
    }

    private void dadoUnaCargaPendiente() {
        propuesta = PropuestaWhatsapp.pendiente(NEGOCIO_ID, "5491122334455", OrigenPropuestaWhatsapp.TEXTO,
                "gasté 5000", GASTOS, List.of(FILA), AHORA);
    }

    private void dadoUnaCargaEsperandoLaTabla() {
        propuesta = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, "5491122334455", OrigenPropuestaWhatsapp.TEXTO,
                "gasté 5000", null, List.of(GASTOS, VENTAS), AHORA);
    }

    private void dadoQueSeDescarto() {
        propuesta.descartar(AHORA);
    }

    private void cuandoSeElige(int numero) {
        error = catchThrowable(() -> elegida = propuesta.elegir(numero));
    }

    private void cuandoPasaAPendienteEn(BloqueId tabla) {
        propuesta.aPendiente(tabla, List.of(FILA));
    }

    private void cuandoSeConfirma() {
        propuesta.confirmar(AHORA);
    }

    private void cuandoSeIntentaConfirmar() {
        error = catchThrowable(this::cuandoSeConfirma);
    }

    private void cuandoVence() {
        propuesta.vencer(AHORA);
    }

    private void entoncesEstaAbierta(boolean abierta) {
        assertThat(propuesta.estaAbierta()).isEqualTo(abierta);
    }

    private void entoncesEstaVigenteA(OffsetDateTime momento, boolean vigente) {
        assertThat(propuesta.estaVigente(momento)).isEqualTo(vigente);
    }

    private void entoncesEsDeLaTabla(BloqueId tabla) {
        assertThat(propuesta.getBloqueId()).isEqualTo(tabla);
    }

    private void entoncesSeEligio(BloqueId tabla) {
        assertThat(elegida).isEqualTo(tabla);
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).hasMessage(mensaje);
    }

    private void entoncesQuedaEn(EstadoPropuestaWhatsapp estado) {
        assertThat(propuesta.getEstado()).isEqualTo(estado);
    }

    private void entoncesTieneLasFilas(FilaPropuesta... filas) {
        assertThat(propuesta.getFilas()).containsExactly(filas);
    }

    private void entoncesSeCerroEl(OffsetDateTime momento) {
        assertThat(propuesta.getCerradoEn()).isEqualTo(momento);
    }
}