package com.ensambla.backend.domain.useCase.metrica.interpretarPedido;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.HOY;
import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static com.ensambla.backend.support.PedidosDePrueba.elInterpreteDeLaAplicacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterpretarPedidoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final InterpretarPedido interpretarPedido = new MetricaUseCaseConfig().interpretarPedido(
            negocioRepository,
            proyectoRepository,
            datosDeProyectoService,
            new MotorMetricas(),
            elInterpreteDeLaAplicacion(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private SugeridaItem propuesta;

    private Throwable rechazo;

    @Test
    @DisplayName("Arma la métrica que se pidió en palabras y muestra lo que daría hoy, sin guardarla")
    void armaLaMetricaPedidaYMuestraLoQueDariaHoy() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();

        cuandoSePide("¿Cuánto vendí hoy?");

        entoncesPropone(unaReceta("Cuánto vendí hoy", VENTAS, SUMA, NINGUNA, HOY, false), "300");
    }

    @Test
    @DisplayName("Un pedido que no se entiende se rechaza antes de leer el cuaderno")
    void unPedidoQueNoSeEntiendeSeRechazaAntesDeLeerElCuaderno() {
        cuandoSeIntentaPedir("hola, ¿cómo andás?");

        entoncesSeRechazaPorque("Decime si es de tus ventas, tus gastos o tu stock");
        entoncesNoSeLeyoElCuaderno();
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404")
    void unCuadernoAjenoRespondeNoEncontrado() {
        dadoUnCuadernoAjeno();

        cuandoSeIntentaPedir("¿Cuánto vendí hoy?");

        entoncesNoSeEncuentraElCuaderno();
    }

    private void dadoUnAlmacenQueHoyVendioPorTrescientos() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(unAlmacenQueVendio(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false)));
    }

    private void dadoUnCuadernoAjeno() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void cuandoSePide(String pedido) {
        propuesta = interpretarPedido.execute(new InterpretarPedidoRequest(USUARIO_ID, PROYECTO_ID, pedido));
    }

    private void cuandoSeIntentaPedir(String pedido) {
        rechazo = catchThrowable(() -> cuandoSePide(pedido));
    }

    private void entoncesPropone(DefinicionMetrica receta, String valor) {
        assertThat(propuesta.definicion()).isEqualTo(DefinicionItem.de(receta));
        assertThat(propuesta.resultado().valor()).isEqualByComparingTo(valor);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }

    private void entoncesNoSeLeyoElCuaderno() {
        verifyNoInteractions(negocioRepository, proyectoRepository, datosDeProyectoService);
    }

    private void entoncesNoSeEncuentraElCuaderno() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
