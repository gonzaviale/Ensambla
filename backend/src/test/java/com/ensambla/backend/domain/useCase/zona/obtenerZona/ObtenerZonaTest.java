package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.VistaZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.zona.ProductosPropiosService;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ZonasDePrueba.NOMBRE_ZONA;
import static com.ensambla.backend.support.ZonasDePrueba.USUARIO_ID;
import static com.ensambla.backend.support.ZonasDePrueba.ZONA_ID;
import static com.ensambla.backend.support.ZonasDePrueba.autorizaSoloLaPrimera;
import static com.ensambla.backend.support.ZonasDePrueba.autorizaTodo;
import static com.ensambla.backend.support.ZonasDePrueba.unAnalisisDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaDeSanJusto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerZonaTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final ProductosPropiosService productosPropiosService = mock(ProductosPropiosService.class);

    private ControlPlanService controlPlanService = mock(ControlPlanService.class, autorizaTodo());
    private ZonaItem zonaItem;

    @Test
    @DisplayName("Devuelve los agregados de la zona del negocio de la sesión")
    void devuelveLosAgregadosDeLaZonaDelNegocioDeLaSesion() {
        dadoUnNegocioConUnaZonaQueVende(unProductoDeZona("Yerba 1kg"));

        cuandoSePideLaVista(VistaZona.PRECIOS);

        entoncesSeInformaLaZonaConLosDatosDeLaYerba();
    }

    @Test
    @DisplayName("Devuelve una zona vacía cuando el negocio todavía no tiene zona, sin mirar su inventario")
    void devuelveUnaZonaVaciaCuandoElNegocioTodaviaNoTieneZona() {
        dadoUnNegocioSinZona();

        cuandoSePideLaVista(VistaZona.PRECIOS);

        entoncesLaZonaVieneVacia();
        entoncesNoSeAnalizaNiSeMiraElInventario();
    }

    @Test
    @DisplayName("Precios gasta su cupo y deja ver primero los productos que el negocio tiene")
    void preciosGastaSuCupoYDejaVerPrimeroLosProductosQueElNegocioTiene() {
        dadoUnNegocioQueTieneYerbaYUnaZonaQueVende(unProductoDeZona("Azúcar 1 kg"), unProductoDeZona("Yerba 1kg"));
        dadoUnPlanQueAutorizaSoloElPrimero();

        cuandoSePideLaVista(VistaZona.PRECIOS);

        entoncesSoloSeVeLaYerbaDeLaZona();
        entoncesSeGastaElCupoDe(TipoContenidoPlan.ZONA);
    }

    @Test
    @DisplayName("Qué se vende cerca gasta su propio cupo y deja ver primero lo que el negocio todavía no tiene")
    void queSeVendeCercaGastaSuPropioCupoYDejaVerPrimeroLoQueElNegocioTodaviaNoTiene() {
        dadoUnNegocioQueTieneYerbaYUnaZonaQueVende(unProductoDeZona("Yerba 1kg"), unProductoDeZona("Azúcar 1 kg"));
        dadoUnPlanQueAutorizaSoloElPrimero();

        cuandoSePideLaVista(VistaZona.CERCA);

        entoncesSoloSeVeElAzucarDeLaZona();
        entoncesSeGastaElCupoDe(TipoContenidoPlan.ZONA_CERCA);
    }

    @Test
    @DisplayName("Una vista que no ordena por el inventario no lo consulta y gasta su propio cupo")
    void unaVistaQueNoOrdenaPorElInventarioNoLoConsultaYGastaSuPropioCupo() {
        dadoUnNegocioConUnaZonaQueVende(unProductoDeZona("Yerba 1kg"), unProductoDeZona("Azúcar 1 kg"));

        cuandoSePideLaVista(VistaZona.OTRA);

        entoncesSeVenLosDosProductosDeLaZona();
        entoncesNoSeMiraElInventario();
        entoncesSeGastaElCupoDe(TipoContenidoPlan.ZONA_OTRA);
    }

    // --- dado ---
    private void dadoUnNegocioConUnaZonaQueVende(AgregadoZonaProducto... productos) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).zonaId(ZONA_ID).build());
        when(zonaRepository.findByIdSiTiene(ZONA_ID)).thenReturn(Optional.of(unaZonaDeSanJusto()));
        when(agregadoZonaRepository.analizar(any())).thenReturn(unAnalisisDeZona(10, productos));
    }

    private void dadoUnNegocioQueTieneYerbaYUnaZonaQueVende(AgregadoZonaProducto... productos) {
        dadoUnNegocioConUnaZonaQueVende(productos);
        when(productosPropiosService.nombresDelInventario(NEGOCIO_ID)).thenReturn(List.of("Yerba"));
    }

    private void dadoUnNegocioSinZona() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(Negocio.builder().build());
        when(zonaRepository.findByIdSiTiene(null)).thenReturn(Optional.empty());
    }

    private void dadoUnPlanQueAutorizaSoloElPrimero() {
        controlPlanService = mock(ControlPlanService.class, autorizaSoloLaPrimera());
    }

    // --- cuando ---
    private void cuandoSePideLaVista(VistaZona vista) {
        zonaItem = casoDeUso().execute(new ObtenerZonaRequest(USUARIO_ID, vista));
    }

    private ObtenerZona casoDeUso() {
        return new ZonaUseCaseConfig().obtenerZona(negocioRepository, zonaRepository, agregadoZonaRepository,
                controlPlanService, productosPropiosService, CLOCK);
    }

    // --- entonces ---
    private void entoncesSeInformaLaZonaConLosDatosDeLaYerba() {
        assertThat(zonaItem.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(zonaItem.comercios()).isEqualTo(10);
        assertThat(zonaItem.productos()).singleElement().satisfies(producto -> {
            assertThat(producto.nombre()).isEqualTo("Yerba 1kg");
            assertThat(producto.comercios()).isEqualTo(8);
            assertThat(producto.precioMinimo()).isEqualByComparingTo("2800");
            assertThat(producto.precioPromedio()).isEqualByComparingTo("3100");
            assertThat(producto.precioMaximo()).isEqualByComparingTo("3400");
            assertThat(producto.comerciosVenden()).isEqualTo(6);
            assertThat(producto.unidadesPorSemana()).isEqualByComparingTo("12.5");
        });
    }

    private void entoncesLaZonaVieneVacia() {
        assertThat(zonaItem.nombre()).isEmpty();
        assertThat(zonaItem.comercios()).isZero();
        assertThat(zonaItem.productos()).isEmpty();
    }

    private void entoncesSoloSeVeLaYerbaDeLaZona() {
        assertThat(zonaItem.productos()).extracting(ProductoItem::nombre).containsExactly("Yerba 1kg");
    }

    private void entoncesSoloSeVeElAzucarDeLaZona() {
        assertThat(zonaItem.productos()).extracting(ProductoItem::nombre).containsExactly("Azúcar 1 kg");
    }

    private void entoncesSeVenLosDosProductosDeLaZona() {
        assertThat(zonaItem.productos()).hasSize(2);
    }

    private void entoncesNoSeAnalizaNiSeMiraElInventario() {
        verifyNoInteractions(agregadoZonaRepository, productosPropiosService);
    }

    private void entoncesNoSeMiraElInventario() {
        verifyNoInteractions(productosPropiosService);
    }

    private void entoncesSeGastaElCupoDe(TipoContenidoPlan tipo) {
        verify(controlPlanService).seleccionar(eq(NEGOCIO_ID), eq(tipo), any(), any());
    }
}
