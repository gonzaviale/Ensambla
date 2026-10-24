package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActualizarNegocioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final BigDecimal LATITUD = new BigDecimal("-34.670500");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562500");
    private static final UbicacionNegocioItem UBICACION =
            new UbicacionNegocioItem("Calle 123", "San Justo", LATITUD, LONGITUD);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaService zonaService = mock(ZonaService.class);
    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private final ActualizarNegocio actualizarNegocio = new ActualizarNegocio(negocioRepository, zonaService,
            ubicacionService, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Asigna la zona que contiene el punto al guardar la ubicación y la devuelve en la configuración")
    void executeShouldAssignZoneForNewPoint() {
        Zona zona = givenZona();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());
        givenLocalidadDelMapa("San Justo");
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        when(zonaService.obtener(zona.getId())).thenReturn(Optional.of(zona));

        ConfiguracionNegocioResponse result = whenActualizaUbicacion();

        assertThat(negocioGuardado().getZonaId()).isEqualTo(zona.getId());
        assertThat(result.zona()).isEqualTo("San Justo - UNLaM");
    }

    @Test
    @DisplayName("Deja el negocio sin zona si ninguna contiene el punto")
    void executeShouldLeaveZoneEmptyWhenNoneMatches() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).zonaId(ZonaId.nuevo()).build());
        givenLocalidadDelMapa("San Justo");
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        ConfiguracionNegocioResponse result = whenActualizaUbicacion();

        assertThat(negocioGuardado().getZonaId()).isNull();
        assertThat(result.zona()).isNull();
    }

    @Test
    @DisplayName("Completa la zona de un negocio con punto y sin zona aunque el punto no cambie")
    void executeShouldFillMissingZoneForSamePoint() {
        Zona zona = givenZona();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD).build());
        givenLocalidadDelMapa("San Justo");
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));

        whenActualizaUbicacion();

        assertThat(negocioGuardado().getZonaId()).isEqualTo(zona.getId());
    }

    @Test
    @DisplayName("Conserva la zona y no la vuelve a resolver si el punto no cambió")
    void executeShouldKeepZoneForSamePoint() {
        ZonaId zonaId = ZonaId.nuevo();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD)
                .localidad("San Justo").zonaId(zonaId).build());

        whenActualizaUbicacion();

        assertThat(negocioGuardado().getZonaId()).isEqualTo(zonaId);
        verify(zonaService, never()).resolver(any(), any());
    }

    @Test
    @DisplayName("Acepta la localidad que coincide con la del mapa aunque cambie mayúsculas")
    void executeShouldAcceptMatchingLocality() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());
        givenLocalidadDelMapa("San Justo");

        actualizarNegocio.execute(new ActualizarNegocioRequest(USUARIO_ID, null, null,
                new UbicacionNegocioItem("Calle 123", "san justo", LATITUD, LONGITUD), null));

        verify(ubicacionService).resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant());
        verify(negocioRepository).save(any());
    }

    @Test
    @DisplayName("No guarda nada si la localidad no coincide con la del mapa y dice cuál es")
    void executeShouldRejectLocalityOutsideMap() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Mi negocio").build();
        givenNegocioParaActualizar(negocio);
        givenLocalidadDelMapa("Ramos Mejía");

        assertThatThrownBy(this::whenActualizaUbicacion)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La localidad no coincide con la del mapa (Ramos Mejía)");

        assertThat(negocio.getLatitud()).isNull();
        verify(negocioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Propaga la indisponibilidad del mapa al verificar la localidad")
    void executeShouldPropagateMapFailure() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());
        when(ubicacionService.resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant()))
                .thenThrow(new ServicioUbicacionException("Sin servicio"));

        assertThatThrownBy(this::whenActualizaUbicacion).isInstanceOf(ServicioUbicacionException.class);
    }

    @Test
    @DisplayName("No consulta al mapa si la ubicación enviada es la ya guardada")
    void executeShouldSkipLocalityCheckForSameLocation() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD)
                .localidad("San Justo").zonaId(ZonaId.nuevo()).build());

        whenActualizaUbicacion();

        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("Sin ubicación no verifica la localidad ni resuelve zona")
    void executeShouldSkipLocalityAndZoneWithoutLocation() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());

        ConfiguracionNegocioResponse result = actualizarNegocio.execute(
                new ActualizarNegocioRequest(USUARIO_ID, "Nuevo nombre", RubroNegocio.KIOSCO, null, null));

        assertThat(result.nombre()).isEqualTo("Nuevo nombre");
        assertThat(result.rubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(negocioGuardado().getZonaId()).isNull();
        verifyNoInteractions(ubicacionService);
        verify(zonaService, never()).resolver(any(), any());
    }

    @Test
    @DisplayName("Responde no encontrado al actualizar la configuración de un usuario sin negocio")
    void executeShouldRejectMissingNegocio() {
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarNegocio.execute(
                new ActualizarNegocioRequest(USUARIO_ID, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(ZonaId.nuevo()).nombre("San Justo - UNLaM").build();
    }

    private void givenNegocioParaActualizar(Negocio negocio) {
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(zonaService.obtener(any())).thenReturn(Optional.empty());
    }

    private void givenLocalidadDelMapa(String localidad) {
        when(ubicacionService.resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant()))
                .thenReturn(new LocalidadesDelPunto(List.of(localidad)));
    }

    // --- act ---
    private ConfiguracionNegocioResponse whenActualizaUbicacion() {
        return actualizarNegocio.execute(new ActualizarNegocioRequest(USUARIO_ID, null, null, UBICACION, null));
    }

    // --- assert ---
    private Negocio negocioGuardado() {
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        return captor.getValue();
    }
}
