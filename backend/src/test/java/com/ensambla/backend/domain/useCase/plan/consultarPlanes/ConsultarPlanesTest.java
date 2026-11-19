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
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.ConsultaPlanesTestData.prueba;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarPlanesTest {

    private ConsultarPlanesResponse result;
    private Suscripcion suscripcion;

    private static final UsuarioId USUARIO_ID = UsuarioId.de("bb000000-0000-4000-8000-000000000009");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);

    @BeforeEach
    void dadoNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El gratuito muestra catálogo, uso del mes argentino y disponibilidad de prueba bajo el mismo bloqueo")
    void elGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajoElMismoBloqueo() {
        dadoElEscenarioDeElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo();

        cuandoSeEjecutaElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo();

        entoncesSeVerificaElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo();
    }

    @Test
    @DisplayName("La prueba usa sus diez páginas totales y fechas originales aunque cruce de mes")
    void laPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes() {
        dadoElEscenarioDeLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes();

        cuandoSeEjecutaLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes();

        entoncesSeVerificaLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes();
    }

    @Test
    @DisplayName("Premium vigente sin historial no ofrece una prueba ni altera sus cien páginas mensuales")
    void premiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginasMensuales() {
        dadoElEscenarioDePremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas();

        cuandoSeEjecutaPremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas();

        entoncesSeVerificaPremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas();
    }

    @Test
    @DisplayName("Al vencer la prueba muestra gratuito y conserva el historial sin esperar al job")
    void alVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob() {
        dadoElEscenarioDeAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob();

        cuandoSeEjecutaAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob();

        entoncesSeVerificaAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob();
    }

    @Test
    @DisplayName("Una suscripción futura usa el gratuito y conserva la disponibilidad de prueba no usada")
    void unaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNoUsada() {
        dadoElEscenarioDeUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo();

        cuandoSeEjecutaUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo();

        entoncesSeVerificaUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo();
    }

    @Test
    @DisplayName("No ofrece prueba si las capacidades premium aún no están configuradas")
    void noOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas() {
        dadoElEscenarioDeNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas();

        cuandoSeEjecutaNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas();

        entoncesSeVerificaNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas();
    }

    @Test
    @DisplayName("Un catálogo ausente falla sin consultar datos privados")
    void unCatalogoAusenteFallaSinConsultarDatosPrivados() {
        cuandoSeEjecutaUnCatalogoAusenteFallaSinConsultarDatosPrivados();

        entoncesSeVerificaUnCatalogoAusenteFallaSinConsultarDatosPrivados();
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void respondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion() {
        dadoElEscenarioDeRespondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion();

        cuandoSeEjecutaRespondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion();
    }

    @Test
    @DisplayName("Una referencia a un plan inexistente no se presenta como premium")
    void unaReferenciaAUnPlanInexistenteNoSePresentaComoPremium() {
        dadoElEscenarioDeUnaReferenciaAUnPlanInexistenteNoSePresentaComoPremium();

        cuandoSeEjecutaUnaReferenciaAUnPlanInexistenteNoSePresentaComoPremium();
    }

    @Test
    @DisplayName("Una prueba activa sin historial no devuelve capacidades incompletas")
    void unaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas() {
        dadoElEscenarioDeUnaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas();

        cuandoSeEjecutaUnaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas();
    }

    @Test
    @DisplayName("Un plan efectivo sin límites configurados falla de forma cerrada")
    void unPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada() {
        dadoElEscenarioDeUnPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada();

        cuandoSeEjecutaUnPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada();
    }

    @Test
    @DisplayName("Un usuario sin negocio no consulta planes de otro usuario")
    void unUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario() {
        dadoElEscenarioDeUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario();

        cuandoSeEjecutaUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario();

        entoncesSeVerificaUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario();
    }

    // --- dado ---
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

    // --- cuando ---
    private ConsultarPlanesResponse consultar(OffsetDateTime ahora) {
        return new ConsultarPlanes(negocioRepository, suscripcionRepository,
                planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(USUARIO_ID);
    }

    // --- dado ---

    private void dadoElEscenarioDeElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo() {
        Plan gratuito = dadoCatalogo();
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
        LocalDate inicio = LocalDate.parse("2026-11-01");
        when(usoPlanRepository.contarProyectos(NEGOCIO_ID)).thenReturn(2L);
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(1001L);
        when(usoPlanRepository.paginasEnPeriodo(NEGOCIO_ID, inicio, inicio.plusMonths(1))).thenReturn(7L);
    }

    private void dadoElEscenarioDeLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void dadoElEscenarioDePremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoElEscenarioDeAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA);
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void dadoElEscenarioDeUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo() {
        dadoCatalogo();
        suscripcion = dadoSuscripcion(premium(), EstadoSuscripcion.CANCELADA, AHORA.plusDays(30));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioEn(AHORA.plusDays(1));
    }

    private void dadoElEscenarioDeNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas() {
        Plan gratuito = plan();
        Plan premium = premium();
        premium.setLimitesConfigurados(false);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoElEscenarioDeRespondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion() {
        dadoCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoElEscenarioDeUnaReferenciaAUnPlanInexistenteNoSePresentaComoPremium() {
        dadoCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
    }

    private void dadoElEscenarioDeUnaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas() {
        dadoCatalogo();
        dadoSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
    }

    private void dadoElEscenarioDeUnPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada() {
        Plan gratuito = dadoCatalogo();
        gratuito.setLimitesConfigurados(false);
        dadoSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void dadoElEscenarioDeUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                        .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeEjecutaElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo() {
        result = consultar(OffsetDateTime.parse("2026-11-01T00:05:00-03:00"));
    }

    private void cuandoSeEjecutaLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes() {
        result = consultar(AHORA);
    }

    private void cuandoSeEjecutaPremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas() {
        result = consultar(AHORA);
    }

    private void cuandoSeEjecutaAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob() {
        result = consultar(AHORA);
    }

    private void cuandoSeEjecutaUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo() {
        result = consultar(AHORA);
    }

    private void cuandoSeEjecutaNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas() {
        result = consultar(AHORA);
    }

    private void cuandoSeEjecutaUnCatalogoAusenteFallaSinConsultarDatosPrivados() {
        assertThatThrownBy(() -> consultar(AHORA))
                        .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }

    private void cuandoSeEjecutaRespondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion() {
        assertThatThrownBy(() -> consultar(AHORA))
                        .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
    }

    private void cuandoSeEjecutaUnaReferenciaAUnPlanInexistenteNoSePresentaComoPremium() {
        assertThatThrownBy(() -> consultar(AHORA))
                        .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }

    private void cuandoSeEjecutaUnaPruebaActivaSinHistorialNoDevuelveCapacidadesIncompletas() {
        assertThatThrownBy(() -> consultar(AHORA))
                        .isInstanceOf(ResourceNotFoundException.class).hasMessage("Prueba no encontrada");
    }

    private void cuandoSeEjecutaUnPlanEfectivoSinLimitesConfiguradosFallaDeFormaCerrada() {
        assertThatThrownBy(() -> consultar(AHORA)).isInstanceOf(RestriccionPlanException.class);
    }

    private void cuandoSeEjecutaUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario() {
        assertThatThrownBy(() -> consultar(AHORA))
                        .isInstanceOf(ResourceNotFoundException.class).hasMessage("Negocio no encontrado");
    }

    // --- entonces ---

    private void entoncesSeVerificaElGratuitoMuestraCatalogoUsoDelMesArgentinoYDisponibilidadDePruebaBajo() {
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.planes()).extracting(PlanItem::codigo).containsExactly("GRATUITO", "PREMIUM");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isTrue();
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.prueba()).isNull();
        assertThat(result.uso()).isEqualTo(new UsoPlanItem(2, 1001, 7, 10, "MENSUAL",
                        OffsetDateTime.parse("2026-11-01T03:00:00Z"), OffsetDateTime.parse("2026-12-01T03:00:00Z")));
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
    }

    private void entoncesSeVerificaLaPruebaUsaSusDiezPaginasTotalesYFechasOriginalesAunqueCruceDeMes() {
        assertThat(result.suscripcion().pruebaActiva()).isTrue();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.uso().paginasIa()).isEqualTo(8);
        assertThat(result.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(result.uso().periodoIa()).isEqualTo("PRUEBA");
        assertThat(result.uso().inicioPeriodoIa()).isEqualTo(prueba().inicioEn());
        assertThat(result.uso().finPeriodoIa()).isEqualTo(prueba().finEn());
        verify(usoPlanRepository, never()).paginasEnPeriodo(NEGOCIO_ID,
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01"));
    }

    private void entoncesSeVerificaPremiumVigenteSinHistorialNoOfreceUnaPruebaNiAlteraSusCienPaginas() {
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("PREMIUM");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.uso().limitePaginasIa()).isEqualTo(100);
        assertThat(result.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void entoncesSeVerificaAlVencerLaPruebaMuestraGratuitoYConservaElHistorialSinEsperarAlJob() {
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.prueba()).isEqualTo(PruebaPremiumResponse.de(prueba()));
        assertThat(result.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void entoncesSeVerificaUnaSuscripcionFuturaUsaElGratuitoYConservaLaDisponibilidadDePruebaNo() {
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    private void entoncesSeVerificaNoOfrecePruebaSiLasCapacidadesPremiumAunNoEstanConfiguradas() {
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
    }

    private void entoncesSeVerificaUnCatalogoAusenteFallaSinConsultarDatosPrivados() {
        verifyNoInteractions(suscripcionRepository);
    }

    private void entoncesSeVerificaUnUsuarioSinNegocioNoConsultaPlanesDeOtroUsuario() {
        verifyNoInteractions(planRepository, suscripcionRepository, usoPlanRepository);
    }
}
