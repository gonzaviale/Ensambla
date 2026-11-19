package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ZonasDePrueba.ZONA_ID;
import static com.ensambla.backend.support.ZonasDePrueba.unAnalisisDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaDeSanJusto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextoDeResumenServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000002");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final ContextoDeResumenService contexto = new ContextoDeResumenService(
            negocioRepository, zonaRepository, agregadoZonaRepository, avisoRepository);

    @Test
    @DisplayName("El resumen se arma con los agregados de la zona del negocio")
    void elResumenSeArmaConLosAgregadosDeLaZonaDelNegocio() {
        AnalisisZona analisis = dadoUnNegocioEnUnaZonaConProductos();

        AnalisisZona zona = cuandoSePideLaZonaDelNegocio();

        entoncesSeUsanLosAgregadosDeEsaZona(zona, analisis);
    }

    @Test
    @DisplayName("Un negocio sin zona asignada se resume con una zona vacía")
    void unNegocioSinZonaAsignadaSeResumeConUnaZonaVacia() {
        dadoUnNegocioSinZona();

        AnalisisZona zona = cuandoSePideLaZonaDelNegocio();

        entoncesLaZonaEstaVacia(zona);
    }

    @Test
    @DisplayName("Un resumen sólo cuenta los avisos del sistema pedido")
    void unResumenSoloCuentaLosAvisosDelSistemaPedido() {
        dadoUnNegocioConAvisosDeDosSistemas();

        List<Aviso> avisos = cuandoSePidenLosAvisosDelSistema();

        entoncesSoloQuedanLosAvisosDeEseSistema(avisos);
    }

    // --- dado ---
    private AnalisisZona dadoUnNegocioEnUnaZonaConProductos() {
        Zona zona = unaZonaDeSanJusto();
        AnalisisZona analisis = unAnalisisDeZona(8, unProductoDeZona("Yerba"));
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);
        return analisis;
    }

    private void dadoUnNegocioSinZona() {
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnNegocioConAvisosDeDosSistemas() {
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of(
                unAvisoDelSistema(PROYECTO_ID), unAvisoDelSistema(OTRO_PROYECTO_ID), unAvisoDelSistema(null)));
    }

    // --- cuando ---
    private AnalisisZona cuandoSePideLaZonaDelNegocio() {
        return contexto.zonaDe(NEGOCIO_ID);
    }

    private List<Aviso> cuandoSePidenLosAvisosDelSistema() {
        return contexto.avisosDe(NEGOCIO_ID, Proyecto.builder().id(PROYECTO_ID).build());
    }

    // --- entonces ---
    private void entoncesSeUsanLosAgregadosDeEsaZona(AnalisisZona obtenida, AnalisisZona esperada) {
        assertThat(obtenida).isSameAs(esperada);
    }

    private void entoncesLaZonaEstaVacia(AnalisisZona zona) {
        assertThat(zona.comercios()).isZero();
        assertThat(zona.productos()).isEmpty();
    }

    private void entoncesSoloQuedanLosAvisosDeEseSistema(List<Aviso> avisos) {
        assertThat(avisos).extracting(Aviso::getProyectoId).containsExactly(PROYECTO_ID);
    }

    // --- datos ---
    private static Aviso unAvisoDelSistema(ProyectoId proyectoId) {
        return Aviso.builder().proyectoId(proyectoId).tipo(TipoAviso.STOCK).estado(EstadoAviso.PENDIENTE).build();
    }
}
