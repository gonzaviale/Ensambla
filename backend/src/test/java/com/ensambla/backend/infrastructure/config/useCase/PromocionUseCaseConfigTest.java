package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.promocion.CanalPromocion;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.promocion.ContextoPromocion;
import com.ensambla.backend.domain.model.promocion.PromocionLiquidar;
import com.ensambla.backend.domain.model.promocion.PromocionProductoEstrella;
import com.ensambla.backend.domain.model.promocion.ReglaPromocion;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest.ProductoARedactar;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.model.negocio.Negocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.PromocionDePrueba.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromocionUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final PromocionUseCaseConfig config = new PromocionUseCaseConfig();

    private NegocioRepository negocioRepository;

    @BeforeEach
    void setUp() {
        negocioRepository = mock(NegocioRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
    }

    @Test
    @DisplayName("Registra las ideas de promoción y los canales, cada uno con su clave")
    void beansShouldRegisterTheRulesAndTheChannels() {
        List<ReglaPromocion> reglas = List.of(config.promocionProductoEstrella(), config.promocionLiquidar());
        List<CanalPromocion> canales = List.of(
                config.canalInstagram(), config.canalEstadoWhatsapp(), config.canalMensajeAClientes());

        assertThat(reglas).hasSize(2).hasAtLeastOneElementOfType(PromocionProductoEstrella.class)
                .hasAtLeastOneElementOfType(PromocionLiquidar.class);
        assertThat(config.catalogoDeCanales(canales).todos()).extracting(CanalPromocion::clave)
                .containsExactly("instagram", "estado", "clientes");
    }

    @Test
    @DisplayName("El wiring de sugerir conecta el sistema, las métricas de venta, las reglas y los canales")
    void sugerirPromocionesShouldWireCollaborators() {
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).build();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(new DatosDeProyecto("Almacén", List.of(), List.of(), List.of()));
        ReglaPromocion regla = (ContextoPromocion contexto) -> Optional.empty();

        var result = config.sugerirPromociones(negocioRepository, proyectoRepository, datosDeProyectoService,
                        new MetricasVentas(30, BigDecimal.ONE), List.of(regla),
                        config.catalogoDeCanales(List.of(config.canalInstagram())), Clock.systemUTC())
                .execute(new SugerirPromocionesRequest(USUARIO_ID, PROYECTO_ID));

        assertThat(result.promociones()).isEmpty();
        assertThat(result.canales()).hasSize(1);
    }

    @Test
    @DisplayName("El wiring de redactar conecta el negocio y el catálogo de canales")
    void redactarPromocionShouldWireCollaborators() {
        CatalogoDeCanales catalogo = config.catalogoDeCanales(List.of(config.canalEstadoWhatsapp()));

        var result = config.redactarPromocion(negocioRepository, catalogo)
                .execute(new RedactarPromocionRequest(USUARIO_ID, "estado", "estrella", "Título", "Gancho",
                        List.of(new ProductoARedactar("Yerba", BigDecimal.TEN))));

        assertThat(result.texto()).contains("Almacén de Ana", "Yerba");
    }
}
