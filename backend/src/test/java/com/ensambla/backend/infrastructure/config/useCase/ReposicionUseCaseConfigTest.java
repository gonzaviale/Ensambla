package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.GeneradorPedidoPdfPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest.LineaDePedido;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReposicionUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final ReposicionUseCaseConfig config = new ReposicionUseCaseConfig();

    private NegocioRepository negocioRepository;
    private ReposicionService reposicionService;

    @BeforeEach
    void setUp() {
        negocioRepository = mock(NegocioRepository.class);
        reposicionService = givenUnServicioConUnProductoAgotado();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
    }

    @Test
    @DisplayName("El wiring de consultar conecta el servicio de reposición")
    void consultarReposicionShouldWireTheService() {
        ConsultarReposicionResponse result = config.consultarReposicion(negocioRepository, reposicionService)
                .execute(new ConsultarReposicionRequest(USUARIO_ID, PROYECTO_ID));

        assertThat(result.grupos()).hasSize(1);
    }

    @Test
    @DisplayName("El wiring de armar el pedido conecta el servicio, el generador del PDF y el reloj")
    void armarPedidoProveedorShouldWireCollaborators() {
        GeneradorPedidoPdfPort generador = (PedidoReposicion pedido) -> new byte[] {7};

        ArmarPedidoProveedorResponse result = config.armarPedidoProveedor(
                        negocioRepository, reposicionService, generador, Clock.systemUTC())
                .execute(new ArmarPedidoProveedorRequest(USUARIO_ID, PROYECTO_ID, "Molinos",
                        List.of(new LineaDePedido(YERBA_ID, BigDecimal.TEN))));

        assertThat(result.contenido()).containsExactly(7);
        assertThat(result.nombreArchivo()).startsWith("pedido-molinos-");
    }

    @Test
    @DisplayName("El servicio de reposición se arma con el repositorio de sistemas y el servicio de datos")
    void reposicionServiceShouldWireCollaborators() {
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).build();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(datosCon());

        ReposicionService result = config.reposicionService(proyectoRepository, datosDeProyectoService);

        assertThat(result.faltantesDe(NEGOCIO_ID, PROYECTO_ID).items()).isEmpty();
    }

    // --- arrange ---
    private ReposicionService givenUnServicioConUnProductoAgotado() {
        ReposicionService servicio = mock(ReposicionService.class);
        Faltantes faltantes = Faltantes.de(datosCon(inventarioCon(producto(YERBA_ID, "Yerba", 0, 10, "Molinos"))));
        when(servicio.faltantesDe(NEGOCIO_ID, PROYECTO_ID)).thenReturn(faltantes);
        return servicio;
    }
}
