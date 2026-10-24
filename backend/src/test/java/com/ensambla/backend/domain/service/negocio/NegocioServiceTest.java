package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NegocioServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final BigDecimal LATITUD = new BigDecimal("-34.670500");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562500");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaService zonaService = mock(ZonaService.class);
    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private final NegocioService negocioService = new NegocioService(negocioRepository, zonaService, ubicacionService, new TransaccionDirecta());

    @Test
    @DisplayName("Resuelve el negocio usando el usuario autenticado")
    void obtenerDelUsuarioShouldResolveOwner() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));

        var result = negocioService.obtenerDelUsuario(USUARIO_ID);

        assertThat(result).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los ids de todos los negocios")
    void listarIdsShouldReturnEveryNegocioId() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID));

        assertThat(negocioService.listarIds()).containsExactly(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Responde no encontrado si el usuario no tiene negocio")
    void obtenerDelUsuarioShouldRejectMissingNegocio() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.obtenerDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Devuelve la configuración del negocio del usuario")
    void obtenerConfiguracionShouldReturnOwnNegocio() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).build();
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.of(negocio));

        assertThat(negocioService.obtenerConfiguracion(USUARIO_ID)).isSameAs(negocio);
    }

    @Test
    @DisplayName("Responde no encontrado al consultar la configuración de un usuario sin negocio")
    void obtenerConfiguracionShouldRejectMissingNegocio() {
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.obtenerConfiguracion(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Asigna la zona que contiene el punto al guardar la ubicación")
    void actualizarConfiguracionShouldAssignZoneForNewPoint() {
        Zona zona = givenZona();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));

        Negocio result = whenActualizaUbicacion();

        assertThat(result.getZonaId()).isEqualTo(zona.getId());
    }

    @Test
    @DisplayName("Deja el negocio sin zona si ninguna contiene el punto")
    void actualizarConfiguracionShouldLeaveZoneEmptyWhenNoneMatches() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).zonaId(UUID.randomUUID()).build());
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        Negocio result = whenActualizaUbicacion();

        assertThat(result.getZonaId()).isNull();
    }

    @Test
    @DisplayName("Completa la zona de un negocio con punto y sin zona aunque el punto no cambie")
    void actualizarConfiguracionShouldFillMissingZoneForSamePoint() {
        Zona zona = givenZona();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD).build());
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));

        Negocio result = whenActualizaUbicacion();

        assertThat(result.getZonaId()).isEqualTo(zona.getId());
    }

    @Test
    @DisplayName("Conserva la zona y no la vuelve a resolver si el punto no cambió")
    void actualizarConfiguracionShouldKeepZoneForSamePoint() {
        UUID zonaId = UUID.randomUUID();
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD)
                .zonaId(zonaId).build());

        Negocio result = whenActualizaUbicacion();

        assertThat(result.getZonaId()).isEqualTo(zonaId);
        verifyNoInteractions(zonaService);
    }

    @Test
    @DisplayName("Verifica la localidad contra el mapa cuando cambia el punto o la localidad")
    void actualizarConfiguracionShouldCheckLocalityWhenLocationChanges() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());

        whenActualizaUbicacion();

        verify(ubicacionService).exigirLocalidadDelPunto("San Justo", LATITUD, LONGITUD, AHORA.toInstant());
    }

    @Test
    @DisplayName("No guarda nada si la localidad no coincide con la del mapa")
    void actualizarConfiguracionShouldRejectLocalityOutsideMap() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Mi negocio").build();
        givenNegocioParaActualizar(negocio);
        doThrow(new BusinessRuleException("La localidad no coincide con la del mapa (San Justo)"))
                .when(ubicacionService).exigirLocalidadDelPunto("San Justo", LATITUD, LONGITUD, AHORA.toInstant());

        assertThatThrownBy(this::whenActualizaUbicacion).isInstanceOf(BusinessRuleException.class);

        assertThat(negocio.getLatitud()).isNull();
        verify(negocioRepository, never()).save(any());
    }

    @Test
    @DisplayName("No consulta al mapa si la ubicación enviada es la ya guardada")
    void actualizarConfiguracionShouldSkipLocalityCheckForSameLocation() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).latitud(LATITUD).longitud(LONGITUD)
                .localidad("San Justo").zonaId(UUID.randomUUID()).build());

        whenActualizaUbicacion();

        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("No verifica la localidad si no se envía ubicación")
    void actualizarConfiguracionShouldSkipLocalityCheckWithoutLocation() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());

        negocioService.actualizarConfiguracion(USUARIO_ID, "Nuevo nombre", null, null, null, AHORA);

        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("No resuelve zona para un negocio sin ubicación")
    void actualizarConfiguracionShouldNotResolveZoneWithoutPoint() {
        givenNegocioParaActualizar(Negocio.builder().id(NEGOCIO_ID).build());

        Negocio result = negocioService.actualizarConfiguracion(USUARIO_ID, "Nuevo nombre", null, null, null, AHORA);

        assertThat(result.getZonaId()).isNull();
        verifyNoInteractions(zonaService);
    }

    @Test
    @DisplayName("Responde no encontrado al actualizar la configuración de un usuario sin negocio")
    void actualizarConfiguracionShouldRejectMissingNegocio() {
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.actualizarConfiguracion(USUARIO_ID, null, null, null, null, AHORA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(UUID.randomUUID()).nombre("San Justo - UNLaM").build();
    }

    private void givenNegocioParaActualizar(Negocio negocio) {
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- act ---
    private Negocio whenActualizaUbicacion() {
        return negocioService.actualizarConfiguracion(USUARIO_ID, null, null,
                new UbicacionNegocio("Calle 123", "San Justo", LATITUD, LONGITUD), null, AHORA);
    }
}
