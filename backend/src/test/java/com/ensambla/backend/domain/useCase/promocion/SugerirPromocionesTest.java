package com.ensambla.backend.domain.useCase.promocion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.promocion.CanalEstadoWhatsapp;
import com.ensambla.backend.domain.model.promocion.CanalInstagram;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.promocion.ContextoPromocion;
import com.ensambla.backend.domain.model.promocion.ProductoPromocionado;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.model.promocion.ReglaPromocion;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.PromocionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.PromocionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.PromocionDePrueba.fila;
import static com.ensambla.backend.support.PromocionDePrueba.inventarioCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SugerirPromocionesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final Promocion ESTRELLA = new Promocion("estrella", "Tu producto estrella", "Lo más vendido",
            List.of(new ProductoPromocionado("Yerba", new BigDecimal("3600"))));

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final ReglaPromocion reglaQueAplica = mock(ReglaPromocion.class);
    private final ReglaPromocion reglaQueNoAplica = mock(ReglaPromocion.class);

    private SugerirPromociones sugerirPromociones;

    @BeforeEach
    void setUp() {
        sugerirPromociones = new SugerirPromociones(negocioRepository, proyectoRepository, datosDeProyectoService,
                new MetricasVentas(30, new BigDecimal("0.5")), List.of(reglaQueAplica, reglaQueNoAplica),
                new CatalogoDeCanales(List.of(new CanalInstagram(), new CanalEstadoWhatsapp())),
                Clock.fixed(INSTANTE, ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(reglaQueAplica.evaluar(any(ContextoPromocion.class))).thenReturn(Optional.of(ESTRELLA));
        when(reglaQueNoAplica.evaluar(any(ContextoPromocion.class))).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Devuelve las promociones de las reglas que aplican y los canales para compartirlas")
    void executeShouldReturnThePromotionsThatApplyAndTheChannels() {
        givenUnSistemaConUnProducto();

        SugerirPromocionesResponse result = whenSugiere();

        assertThat(result.promociones()).singleElement().satisfies(promocion -> {
            assertThat(promocion.clave()).isEqualTo("estrella");
            assertThat(promocion.titulo()).isEqualTo("Tu producto estrella");
            assertThat(promocion.productos()).singleElement()
                    .satisfies(producto -> assertThat(producto.nombre()).isEqualTo("Yerba"));
        });
        assertThat(result.canales()).extracting(canal -> canal.clave()).containsExactly("instagram", "estado");
    }

    @Test
    @DisplayName("Les pasa a las reglas los productos del sistema")
    void executeShouldGiveTheRulesTheProductsOfTheProject() {
        givenUnSistemaConUnProducto();

        whenSugiere();

        ArgumentCaptor<ContextoPromocion> captor = ArgumentCaptor.forClass(ContextoPromocion.class);
        verify(reglaQueAplica).evaluar(captor.capture());
        assertThat(captor.getValue().productos()).singleElement()
                .satisfies(producto -> assertThat(producto.id()).isEqualTo(YERBA_ID));
    }

    @Test
    @DisplayName("Si ninguna regla aplica devuelve solo los canales")
    void executeShouldReturnNoPromotionsWhenNoRuleApplies() {
        givenUnSistemaConUnProducto();
        when(reglaQueAplica.evaluar(any(ContextoPromocion.class))).thenReturn(Optional.empty());

        SugerirPromocionesResponse result = whenSugiere();

        assertThat(result.promociones()).isEmpty();
        assertThat(result.canales()).hasSize(2);
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindAForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(this::whenSugiere).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenUnSistemaConUnProducto() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(
                new DatosDeProyecto("Almacén",
                        List.of(inventarioCon(fila(YERBA_ID, "Yerba", 3600, 10))), List.of(), List.of()));
    }

    // --- act ---
    private SugerirPromocionesResponse whenSugiere() {
        return sugerirPromociones.execute(new SugerirPromocionesRequest(USUARIO_ID, PROYECTO_ID));
    }
}
