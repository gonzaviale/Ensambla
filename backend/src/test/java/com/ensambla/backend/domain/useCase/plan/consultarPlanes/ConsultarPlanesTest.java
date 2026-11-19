package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.ConsultaPlanesTestData.prueba;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarPlanesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("bb000000-0000-4000-8000-000000000009");
    private static final OffsetDateTime PRIMER_MINUTO_DEL_MES_ARGENTINO =
            OffsetDateTime.parse("2026-11-01T00:05:00-03:00");
    private static final OffsetDateTime INICIO_DEL_MES_EN_UTC = OffsetDateTime.parse("2026-11-01T03:00:00Z");
    private static final OffsetDateTime INICIO_DEL_MES_SIGUIENTE_EN_UTC = OffsetDateTime.parse("2026-12-01T03:00:00Z");
    private static final LocalDate INICIO_DEL_MES = LocalDate.parse("2026-11-01");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);
    private ConsultarPlanesResponse respuesta;
    private Throwable error;

    @BeforeEach
    void dadoNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El gratuito muestra catálogo, uso del mes argentino y disponibilidad de prueba bajo el mismo bloqueo")
    void elGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajoEl() {
        dadoUnGratuitoConUsoDelMes();

        cuandoSeConsulta(PRIMER_MINUTO_DEL_MES_ARGENTINO);

        entoncesSeMuestraElGratuitoConSuCatalogoYSuPrueba();
        entoncesElUsoEsElDelMesArgentino();
        entoncesSeBloqueaElNegocioAntesDeLeerLaSuscripcion();
    }

    @Test
    @DisplayName("La prueba usa sus diez páginas totales y fechas originales aunque cruce de mes")
    void laPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes() {
        dadoUnaPruebaActiva();

        cuandoSeConsulta(AHORA);

        entoncesElUsoEsElDeLaPrueba();
        entoncesNoSeConsultaElUsoMensualDeIa();
    }

    @Test
    @DisplayName("Premium vigente sin historial no ofrece una prueba ni altera sus cien páginas mensuales")
    void premiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginasMensuales() {
        dadoUnPremiumPagoVigente();

        cuandoSeConsulta(AHORA);

        entoncesSeMuestraPremiumConSusLimitesMensuales();
    }

    @Test
    @DisplayName("Al vencer la prueba muestra gratuito y conserva el historial sin esperar al job")
    void alVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob() {
        dadoUnaPruebaQueVenceAhora();

        cuandoSeConsulta(AHORA);

        entoncesSeMuestraElGratuitoConElHistorialDeLaPrueba();
    }

    @Test
    @DisplayName("Una suscripción futura usa el gratuito y conserva la disponibilidad de prueba no usada")
    void unaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNoUsada() {
        dadoUnaSuscripcionFutura();

        cuandoSeConsulta(AHORA);

        entoncesSeMuestraElGratuitoConPruebaDisponible();
    }

    @Test
    @DisplayName("No ofrece prueba si las capacidades premium aún no están configuradas")
    void noOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas() {
        dadoUnPremiumSinLimitesConfigurados();

        cuandoSeConsulta(AHORA);

        entoncesNoSeOfreceLaPrueba();
    }

    @Test
    @DisplayName("Un catálogo ausente falla sin consultar datos privados")
    void unCatalogoAusenteFallaSinConsultarDatosPrivados() {
        cuandoSeConsultaConError(AHORA);

        entoncesFallaPorRecursoInexistente("Plan no encontrado");
        entoncesNoSeConsultaLaSuscripcion();
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void respondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion() {
        dadoUnCatalogoSinSuscripcion();

        cuandoSeConsultaConError(AHORA);

        entoncesFallaPorRecursoInexistente("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una referencia a un plan inexistente no se presenta como premium")
    void unaReferenciaAUnPlanInexistenteNoSePresentaComoPremium() {
        dadoUnaSuscripcionConPlanInexistente();

        cuandoSeConsultaConError(AHORA);

        entoncesFallaPorRecursoInexistente("Plan no encontrado");
    }

    @Test
    @DisplayName("Una prueba activa sin historial no devuelve capacidades incompletas")
    void unaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas() {
        dadoUnaPruebaActivaSinHistorial();

        cuandoSeConsultaConError(AHORA);

        entoncesFallaPorRecursoInexistente("Prueba no encontrada");
    }

    @Test
    @DisplayName("Un plan efectivo sin límites configurados falla de forma cerrada")
    void unPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada() {
        dadoUnGratuitoSinLimitesConfigurados();

        cuandoSeConsultaConError(AHORA);

        entoncesSeRechazaPorRestriccionDePlan();
    }

    @Test
    @DisplayName("Un usuario sin negocio no consulta planes de otro usuario")
    void unUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario() {
        dadoUnUsuarioSinNegocio();

        cuandoSeConsultaConError(AHORA);

        entoncesFallaPorRecursoInexistente("Negocio no encontrado");
        entoncesNoSeConsultaNingunDatoDelPlan();
    }

    // --- arrange ---
    private void dadoUnGratuitoConUsoDelMes() {
        Plan gratuito = dadoCatalogo();
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
        when(usoPlanRepository.contarProyectos(NEGOCIO_ID)).thenReturn(2L);
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(1001L);
        when(usoPlanRepository.paginasEnPeriodo(NEGOCIO_ID, INICIO_DEL_MES, INICIO_DEL_MES.plusMonths(1)))
                .thenReturn(7L);
        when(usoPlanRepository.objetivosIaEnPeriodo(
                NEGOCIO_ID, INICIO_DEL_MES_EN_UTC, INICIO_DEL_MES_SIGUIENTE_EN_UTC)).thenReturn(3L);
    }

    private void dadoUnaPruebaActiva() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void dadoUnPremiumPagoVigente() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoUnaPruebaQueVenceAhora() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA);
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void dadoUnaSuscripcionFutura() {
        dadoCatalogo();
        Suscripcion suscripcion = dadoSuscripcion(premium(), EstadoSuscripcion.CANCELADA, AHORA.plusDays(30));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioEn(AHORA.plusDays(1));
    }

    private void dadoUnPremiumSinLimitesConfigurados() {
        Plan gratuito = plan();
        Plan premium = premium();
        premium.setLimitesConfigurados(false);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoUnCatalogoSinSuscripcion() {
        dadoCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaSuscripcionConPlanInexistente() {
        dadoCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
    }

    private void dadoUnaPruebaActivaSinHistorial() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
    }

    private void dadoUnGratuitoSinLimitesConfigurados() {
        Plan gratuito = dadoCatalogo();
        gratuito.setLimitesConfigurados(false);
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    private Plan dadoCatalogo() {
        Plan gratuito = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        return gratuito;
    }

    private Suscripcion dadoSuscripcion(Plan plan, EstadoSuscripcion estado, OffsetDateTime fin) {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setPlanId(plan.getId());
        suscripcion.setEstado(estado);
        suscripcion.setInicioEn(AHORA.minusDays(3));
        suscripcion.setFinEn(fin);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        if (suscripcion.estaVigente(AHORA)) {
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        }
        return suscripcion;
    }

    // --- act ---
    private void cuandoSeConsulta(OffsetDateTime ahora) {
        respuesta = new ConsultarPlanes(negocioRepository, suscripcionRepository,
                planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(USUARIO_ID);
    }

    private void cuandoSeConsultaConError(OffsetDateTime ahora) {
        error = catchThrowable(() -> cuandoSeConsulta(ahora));
    }

    // --- assert ---
    private void entoncesSeMuestraElGratuitoConSuCatalogoYSuPrueba() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.planes()).extracting(PlanItem::codigo).containsExactly("GRATUITO", "PREMIUM");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isTrue();
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.prueba()).isNull();
    }

    private void entoncesElUsoEsElDelMesArgentino() {
        assertThat(respuesta.uso()).isEqualTo(new UsoPlanItem(2, 1001, 7, 10, "MENSUAL",
                INICIO_DEL_MES_EN_UTC, INICIO_DEL_MES_SIGUIENTE_EN_UTC, 3, 5));
    }

    private void entoncesSeBloqueaElNegocioAntesDeLeerLaSuscripcion() {
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
    }

    private void entoncesElUsoEsElDeLaPrueba() {
        assertThat(respuesta.suscripcion().pruebaActiva()).isTrue();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.uso().paginasIa()).isEqualTo(8);
        assertThat(respuesta.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(respuesta.uso().periodoIa()).isEqualTo("PRUEBA");
        assertThat(respuesta.uso().limiteObjetivosIa()).isEqualTo(20);
        assertThat(respuesta.uso().inicioPeriodoIa()).isEqualTo(prueba().inicioEn());
        assertThat(respuesta.uso().finPeriodoIa()).isEqualTo(prueba().finEn());
    }

    private void entoncesNoSeConsultaElUsoMensualDeIa() {
        verify(usoPlanRepository, never()).paginasEnPeriodo(NEGOCIO_ID, INICIO_DEL_MES, INICIO_DEL_MES.plusMonths(1));
    }

    private void entoncesSeMuestraPremiumConSusLimitesMensuales() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("PREMIUM");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.uso().limitePaginasIa()).isEqualTo(100);
        assertThat(respuesta.uso().limiteObjetivosIa()).isEqualTo(20);
        assertThat(respuesta.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void entoncesSeMuestraElGratuitoConElHistorialDeLaPrueba() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.prueba()).isEqualTo(PruebaPremiumResponse.de(prueba()));
        assertThat(respuesta.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void entoncesSeMuestraElGratuitoConPruebaDisponible() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    private void entoncesNoSeOfreceLaPrueba() {
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
    }

    private void entoncesFallaPorRecursoInexistente(String mensaje) {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorRestriccionDePlan() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesNoSeConsultaLaSuscripcion() {
        verifyNoInteractions(suscripcionRepository);
    }

    private void entoncesNoSeConsultaNingunDatoDelPlan() {
        verifyNoInteractions(planRepository, suscripcionRepository, usoPlanRepository);
    }
}
