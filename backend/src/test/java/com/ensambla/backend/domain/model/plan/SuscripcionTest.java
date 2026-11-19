package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.OffsetDateTime;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;

class SuscripcionTest {

    private Suscripcion suscripcion;
    private boolean vigente;
    private boolean vigenteAlInicio;
    private boolean vigenteJustoAntesDelFin;
    private boolean vigenteEnElFin;
    private boolean vigenteAntesDelInicio;

    @Test
    @DisplayName("El período activo incluye exactamente su inicio")
    void elPeriodoActivoIncluyeExactamenteSuInicio() {
        dadoUnaSuscripcionActiva();

        cuandoSeConsultaLaVigenciaEn(INSTANTE);

        entoncesEstaVigente();
    }

    @Test
    @DisplayName("Un período futuro no habilita premium antes de su inicio")
    void unPeriodoFuturoNoHabilitaPremiumAntesDeSuInicio() {
        dadoUnaSuscripcionActiva();

        cuandoSeConsultaLaVigenciaEn(INSTANTE.minusNanos(1));

        entoncesNoEstaVigente();
    }

    @Test
    @DisplayName("Un período deja de estar vigente en el instante exacto de fin")
    void unPeriodoDejaDeEstarVigenteEnElInstanteExactoDeFin() {
        dadoUnaSuscripcionActiva();

        cuandoSeConsultaLaVigenciaEn(suscripcion.getFinEn());

        entoncesNoEstaVigente();
    }

    @Test
    @DisplayName("El gratuito activo puede tener vigencia sin fecha de fin")
    void elGratuitoActivoPuedeTenerVigenciaSinFechaDeFin() {
        dadoUnaSuscripcionActivaSinFechaDeFin();

        cuandoSeConsultaLaVigenciaEn(INSTANTE.plusYears(1));

        entoncesEstaVigente();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSuscripcion.class, names = {"VENCIDA", "CANCELADA"})
    @DisplayName("Estados sin activación no habilitan premium")
    void estadosSinActivacionNoHabilitanPremium(EstadoSuscripcion estado) {
        dadoUnaSuscripcionEnEstado(estado);

        cuandoSeConsultaLaVigenciaEn(INSTANTE);

        entoncesNoEstaVigente();
    }

    @Test
    @DisplayName("La prueba habilita premium desde su inicio y termina exactamente a los siete días")
    void laPruebaHabilitaPremiumDesdeSuInicioYTerminaExactamenteALosSieteDias() {
        dadoUnaPruebaDeSieteDias();

        cuandoSeConsultaLaVigenciaDeLaPruebaEnSusExtremos();

        entoncesLaPruebaEstaVigenteDesdeSuInicioHastaJustoAntesDeLosSieteDias();
    }

    // --- dado ---
    private void dadoUnaSuscripcionActiva() {
        suscripcion = suscripcion();
    }

    private void dadoUnaSuscripcionActivaSinFechaDeFin() {
        suscripcion = suscripcion();
        suscripcion.setFinEn(null);
    }

    private void dadoUnaSuscripcionEnEstado(EstadoSuscripcion estado) {
        suscripcion = suscripcion();
        suscripcion.setEstado(estado);
    }

    private void dadoUnaPruebaDeSieteDias() {
        suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setFinEn(INSTANTE.plusDays(7));
    }

    // --- cuando ---
    private void cuandoSeConsultaLaVigenciaEn(OffsetDateTime instante) {
        vigente = suscripcion.estaVigente(instante);
    }

    private void cuandoSeConsultaLaVigenciaDeLaPruebaEnSusExtremos() {
        vigenteAlInicio = suscripcion.estaVigente(INSTANTE);
        vigenteJustoAntesDelFin = suscripcion.estaVigente(INSTANTE.plusDays(7).minusNanos(1));
        vigenteEnElFin = suscripcion.estaVigente(INSTANTE.plusDays(7));
        vigenteAntesDelInicio = suscripcion.estaVigente(INSTANTE.minusNanos(1));
    }

    // --- entonces ---
    private void entoncesEstaVigente() {
        assertThat(vigente).isTrue();
    }

    private void entoncesNoEstaVigente() {
        assertThat(vigente).isFalse();
    }

    private void entoncesLaPruebaEstaVigenteDesdeSuInicioHastaJustoAntesDeLosSieteDias() {
        assertThat(vigenteAlInicio).isTrue();
        assertThat(vigenteJustoAntesDelFin).isTrue();
        assertThat(vigenteEnElFin).isFalse();
        assertThat(vigenteAntesDelInicio).isFalse();
    }
}
