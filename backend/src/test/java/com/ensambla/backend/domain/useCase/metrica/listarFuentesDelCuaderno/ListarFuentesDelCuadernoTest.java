package com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarFuentesDelCuadernoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final ListarFuentesDelCuaderno listarFuentes = new MetricaUseCaseConfig().listarFuentesDelCuaderno(
            negocioRepository, proyectoRepository, datosDeProyectoService);

    private List<FuenteMetrica> fuentes;

    private Throwable rechazo;

    @Test
    @DisplayName("Sólo ofrece lo que el negocio ya cargó: un almacén con ventas y stock, sin gastos, no ofrece gastos")
    void soloOfreceLoQueElNegocioYaCargo() {
        dadoUnAlmacenSinGastos();

        cuandoSeListanLasFuentes();

        entoncesSePuedeArmarDe(List.of(FuenteMetrica.VENTAS, FuenteMetrica.INVENTARIO));
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se lee nada")
    void unCuadernoAjenoRespondeNoEncontrado() {
        dadoUnCuadernoAjeno();

        cuandoSeIntentanListarLasFuentes();

        entoncesNoSeEncuentraElCuadernoNiSeLeyeronLosDatos();
    }

    private void dadoUnAlmacenSinGastos() {
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

    private void cuandoSeListanLasFuentes() {
        fuentes = listarFuentes.execute(new ListarFuentesDelCuadernoRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentanListarLasFuentes() {
        rechazo = catchThrowable(this::cuandoSeListanLasFuentes);
    }

    private void entoncesSePuedeArmarDe(List<FuenteMetrica> esperadas) {
        assertThat(fuentes).isEqualTo(esperadas);
    }

    private void entoncesNoSeEncuentraElCuadernoNiSeLeyeronLosDatos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(datosDeProyectoService);
    }
}
