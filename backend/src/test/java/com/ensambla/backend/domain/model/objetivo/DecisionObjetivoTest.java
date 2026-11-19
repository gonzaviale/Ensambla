package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.TITULO_AVISO;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class DecisionObjetivoTest {

    private static final OffsetDateTime DOMINGO_A_LA_NOCHE = OffsetDateTime.parse("2026-11-09T01:30:00Z");
    private static final LocalDate LUNES_ARGENTINO = LocalDate.of(2026, 11, 2);
    private static final LocalDate SEMANA_DE_LA_RESPUESTA = LocalDate.of(2026, 11, 9);

    private DecisionObjetivo decision;

    @Test
    @DisplayName("Una decisión sobre un aviso copia el aviso, el producto y el mini-objetivo vinculado")
    void unaDecisionSobreUnAvisoCopiaElAvisoElProductoYElMiniObjetivoVinculado() {
        cuandoSeDecideSobreUnAviso(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO,
                miniObjetivo(), AHORA);

        entoncesLaDecisionCopiaElAvisoYSeVinculaAlMini();
    }

    @Test
    @DisplayName("Sin mini-objetivo del producto la decisión queda sin vincular")
    void sinMiniObjetivoDelProductoLaDecisionQuedaSinVincular() {
        cuandoSeDecideSobreUnAviso(aviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO), TipoDecision.AVISO_DESCARTADO, null,
                AHORA);

        entoncesLaDecisionNoTieneMini();
        entoncesElTipoEs(TipoDecision.AVISO_DESCARTADO);
    }

    @Test
    @DisplayName("Un aviso que no es de un producto no trae producto")
    void unAvisoQueNoEsDeUnProductoNoTraeProducto() {
        cuandoSeDecideSobreUnAviso(avisoDeLaZona(), TipoDecision.AVISO_APLICADO, null, AHORA);

        entoncesLaDecisionNoTieneProducto();
    }

    @Test
    @DisplayName("La semana es la del lunes en hora argentina, aunque en UTC ya sea otro día")
    void laSemanaEsLaDelLunesEnHoraArgentinaAunqueEnUtcYaSeaOtroDia() {
        cuandoSeDecideSobreUnAviso(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO, null,
                DOMINGO_A_LA_NOCHE);

        entoncesLaSemanaEs(LUNES_ARGENTINO);
    }

    @Test
    @DisplayName("Una decisión sobre un paso copia el mini-objetivo, su producto y su título")
    void unaDecisionSobreUnPasoCopiaElMiniObjetivoSuProductoYSuTitulo() {
        cuandoSeDecideSobreUnPaso(DecisionPaso.ACEPTADO);

        entoncesLaDecisionCopiaElPaso();
    }

    @Test
    @DisplayName("Ignorar el paso es otro tipo de decisión")
    void ignorarElPasoEsOtroTipoDeDecision() {
        cuandoSeDecideSobreUnPaso(DecisionPaso.IGNORADO);

        entoncesElTipoEs(TipoDecision.PASO_IGNORADO);
    }

    @Test
    @DisplayName("La respuesta a un manual deja una decisión con el tipo de lo que contó, vinculada al mini-objetivo")
    void laRespuestaAUnManualDejaUnaDecisionConElTipoDeLoQueContoVinculadaAlMini() {
        cuandoSeDecideSobreUnaRespuesta();

        entoncesLaDecisionCopiaLaRespuesta();
    }

    // --- act ---
    private void cuandoSeDecideSobreUnAviso(Aviso aviso, TipoDecision tipo, MiniObjetivo mini, OffsetDateTime cuando) {
        decision = DecisionObjetivo.deAviso(objetivo(), aviso, tipo, mini, cuando);
    }

    private void cuandoSeDecideSobreUnPaso(DecisionPaso paso) {
        decision = DecisionObjetivo.dePaso(miniObjetivo(), paso, AHORA);
    }

    private void cuandoSeDecideSobreUnaRespuesta() {
        decision = DecisionObjetivo.deRespuesta(miniManual(), respuesta(), AHORA);
    }

    // --- assert ---
    private void entoncesLaDecisionCopiaElAvisoYSeVinculaAlMini() {
        assertThat(decision.id()).isNotNull();
        assertThat(decision.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(decision.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(decision.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(decision.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(decision.avisoId()).isEqualTo(AVISO_ID);
        assertThat(decision.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(decision.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(decision.tomadaEn()).isEqualTo(AHORA);
        assertThat(decision.semanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesLaDecisionNoTieneMini() {
        assertThat(decision.miniObjetivoId()).isNull();
    }

    private void entoncesElTipoEs(TipoDecision esperado) {
        assertThat(decision.tipo()).isEqualTo(esperado);
    }

    private void entoncesLaDecisionNoTieneProducto() {
        assertThat(decision.registroId()).isNull();
    }

    private void entoncesLaSemanaEs(LocalDate esperada) {
        assertThat(decision.semanaInicio()).isEqualTo(esperada);
    }

    private void entoncesLaDecisionCopiaElPaso() {
        assertThat(decision.id()).isNotNull();
        assertThat(decision.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(decision.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(decision.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(decision.tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(decision.avisoId()).isNull();
        assertThat(decision.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(decision.descripcion()).isEqualTo(TITULO);
        assertThat(decision.tomadaEn()).isEqualTo(AHORA);
        assertThat(decision.semanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesLaDecisionCopiaLaRespuesta() {
        assertThat(decision.id()).isNotNull();
        assertThat(decision.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(decision.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(decision.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(decision.tipo()).isEqualTo(TipoDecision.MINI_EN_PARTE);
        assertThat(decision.avisoId()).isNull();
        assertThat(decision.registroId()).isNull();
        assertThat(decision.descripcion()).isEqualTo(TITULO_MANUAL);
        assertThat(decision.tomadaEn()).isEqualTo(AHORA);
        assertThat(decision.semanaInicio()).isEqualTo(SEMANA_DE_LA_RESPUESTA);
    }

    // --- helpers ---
    private static Aviso avisoDeLaZona() {
        Aviso deLaZona = aviso(TipoAviso.CONSUMO_ZONA, EstadoAviso.APLICADO);
        deLaZona.setEntidad("zona");
        return deLaZona;
    }
}
