package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FiltroReporteTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId ESTADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId PAGADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");

    private FiltroReporte filtro;
    private FiltroReporte otroFiltro;
    private FiltroReporte tercerFiltro;
    private List<Throwable> rechazos;

    @Test
    @DisplayName("Sin filtros no acota nada")
    void sinFiltrosNoAcotaNada() {
        cuandoSeArma(null, null, null, null, null);

        entoncesNoAcotaNada();
    }

    @Test
    @DisplayName("Las fechas incluyen los dos días enteros en la zona de Argentina")
    void lasFechasIncluyenLosDosDiasEnterosEnLaZonaDeArgentina() {
        cuandoSeArma(" pan ", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), null, null);

        entoncesElRangoCubreLosDosDiasEnterosEnHoraArgentina();
    }

    @Test
    @DisplayName("Un texto en blanco no filtra")
    void unTextoEnBlancoNoFiltra() {
        cuandoSeArma("   ", null, null, null, null);

        entoncesElTextoQuedaSinFiltrar();
    }

    @Test
    @DisplayName("Acepta un solo extremo del rango y el mismo día en los dos")
    void aceptaUnSoloExtremoDelRangoYElMismoDiaEnLosDos() {
        cuandoSeArmanFiltrosConUnSoloExtremoYConElMismoDia(LocalDate.parse("2026-10-13"));

        entoncesAceptaUnSoloExtremoYElMismoDiaEnLosDos();
    }

    @Test
    @DisplayName("Rechaza una fecha desde posterior a la fecha hasta")
    void rechazaUnaFechaDesdePosteriorALaFechaHasta() {
        cuandoSeIntentaArmar(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01"));

        entoncesSeRechazaPorFechasAlReves();
    }

    @Test
    @DisplayName("Filtra por un valor de una columna de opción o de sí/no")
    void filtraPorUnValorDeUnaColumnaDeOpcionODeSiNo() {
        cuandoSeFiltraPorUnaOpcionYPorSiNo();

        entoncesFiltraPorLaColumnaConSuValor();
    }

    @Test
    @DisplayName("Rechaza filtrar por una columna que no es de opción ni de sí/no, o que no existe")
    void rechazaFiltrarPorUnaColumnaQueNoEsDeOpcionNiDeSiNoOQueNoExiste() {
        cuandoSeIntentaFiltrarPorUnaColumnaDeTextoYPorUnaInexistente();

        entoncesSeRechazanPorNoPoderFiltrarPorEsosCampos();
    }

    @Test
    @DisplayName("Pide el valor y rechaza uno que la columna no admite")
    void pideElValorYRechazaUnoQueLaColumnaNoAdmite() {
        cuandoSeIntentaFiltrarSinValorConValorEnBlancoYConValoresQueNoAdmite();

        entoncesSePideElValorYSeRechazanLosQueLaColumnaNoAdmite();
    }

    // --- cuando ---
    private void cuandoSeArma(String texto, LocalDate desde, LocalDate hasta, CampoId campoId, String valor) {
        filtro = armar(texto, desde, hasta, campoId, valor);
    }

    private void cuandoSeArmanFiltrosConUnSoloExtremoYConElMismoDia(LocalDate dia) {
        filtro = armar(null, dia, null, null, null);
        otroFiltro = armar(null, null, dia, null, null);
        tercerFiltro = armar(null, dia, dia, null, null);
    }

    private void cuandoSeIntentaArmar(LocalDate desde, LocalDate hasta) {
        rechazos = List.of(catchThrowable(() -> armar(null, desde, hasta, null, null)));
    }

    private void cuandoSeFiltraPorUnaOpcionYPorSiNo() {
        filtro = armar(null, null, null, ESTADO_ID, "Entregado");
        otroFiltro = armar(null, null, null, PAGADO_ID, "true");
        tercerFiltro = armar(null, null, null, PAGADO_ID, "false");
    }

    private void cuandoSeIntentaFiltrarPorUnaColumnaDeTextoYPorUnaInexistente() {
        rechazos = List.of(
                catchThrowable(() -> armar(null, null, null, NOMBRE_ID, "x")),
                catchThrowable(() -> armar(null, null, null, CampoId.nuevo(), "x")));
    }

    private void cuandoSeIntentaFiltrarSinValorConValorEnBlancoYConValoresQueNoAdmite() {
        rechazos = List.of(
                catchThrowable(() -> armar(null, null, null, ESTADO_ID, null)),
                catchThrowable(() -> armar(null, null, null, ESTADO_ID, " ")),
                catchThrowable(() -> armar(null, null, null, ESTADO_ID, "Perdido")),
                catchThrowable(() -> armar(null, null, null, PAGADO_ID, "quizás")));
    }

    // --- entonces ---
    private void entoncesNoAcotaNada() {
        assertThat(filtro.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(filtro.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(filtro.texto()).isNull();
        assertThat(filtro.desde()).isNull();
        assertThat(filtro.hasta()).isNull();
        assertThat(filtro.valor()).isNull();
        assertThat(filtro.filtraPorColumna()).isFalse();
    }

    private void entoncesElRangoCubreLosDosDiasEnterosEnHoraArgentina() {
        assertThat(filtro.texto()).isEqualTo("pan");
        assertThat(filtro.desde()).isEqualTo(Instant.parse("2026-10-01T03:00:00Z"));
        assertThat(filtro.hasta()).isEqualTo(Instant.parse("2026-11-01T03:00:00Z"));
    }

    private void entoncesElTextoQuedaSinFiltrar() {
        assertThat(filtro.texto()).isNull();
    }

    private void entoncesAceptaUnSoloExtremoYElMismoDiaEnLosDos() {
        assertThat(filtro.hasta()).isNull();
        assertThat(otroFiltro.desde()).isNull();
        assertThat(tercerFiltro.hasta()).isEqualTo(Instant.parse("2026-10-14T03:00:00Z"));
    }

    private void entoncesSeRechazaPorFechasAlReves() {
        assertThat(rechazos.getFirst()).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta");
    }

    private void entoncesFiltraPorLaColumnaConSuValor() {
        assertThat(filtro.filtraPorColumna()).isTrue();
        assertThat(filtro.campoId()).isEqualTo(ESTADO_ID);
        assertThat(filtro.valor()).isEqualTo("Entregado");
        assertThat(otroFiltro.valor()).isEqualTo("true");
        assertThat(tercerFiltro.valor()).isEqualTo("false");
    }

    private void entoncesSeRechazanPorNoPoderFiltrarPorEsosCampos() {
        assertThat(rechazos).allSatisfy(rechazo -> assertThat(rechazo)
                .isInstanceOf(BusinessRuleException.class).hasMessage("No se puede filtrar por ese campo"));
    }

    private void entoncesSePideElValorYSeRechazanLosQueLaColumnaNoAdmite() {
        assertThat(rechazos.get(0)).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí el valor por el que filtrar");
        assertThat(rechazos.get(1)).isInstanceOf(BusinessRuleException.class);
        assertThat(rechazos.get(2)).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí un valor de la columna «Estado»");
        assertThat(rechazos.get(3)).isInstanceOf(BusinessRuleException.class);
    }

    private static FiltroReporte armar(String texto, LocalDate desde, LocalDate hasta, CampoId campoId, String valor) {
        List<Campo> campos = List.of(
                Campo.builder().id(ESTADO_ID).nombre("Estado").tipo(TipoCampo.OPCION)
                        .opciones(List.of("Pendiente", "Entregado")).build(),
                Campo.builder().id(PAGADO_ID).nombre("Pagado").tipo(TipoCampo.SI_NO).build(),
                Campo.builder().id(NOMBRE_ID).nombre("Nombre").tipo(TipoCampo.TEXTO).build());
        return FiltroReporte.armar(NEGOCIO_ID, BLOQUE_ID, campos, texto, desde, hasta, campoId, valor);
    }
}
