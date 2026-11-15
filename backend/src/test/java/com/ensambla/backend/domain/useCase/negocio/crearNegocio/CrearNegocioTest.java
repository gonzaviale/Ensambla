package com.ensambla.backend.domain.useCase.negocio.crearNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.negocio.NegocioDeLaCuentaItem;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearNegocioTest {

    private static final NegocioId ACTIVO_ID = NegocioId.de("20000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("30000000-0000-4000-8000-000000000001");
    private static final BigDecimal LATITUD = new BigDecimal("-34.670123");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.560789");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final GeocodificadorPort geocodificadorPort = mock(GeocodificadorPort.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final CrearNegocio crearNegocio = new CrearNegocio(negocioRepository, zonaRepository,
            geocodificadorPort, controlPlanService, new TransaccionDirecta(),
            Clock.fixed(FECHA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Crea sin ubicación ni rubro como Otro, sin cambiar el negocio activo")
    void executeShouldCreateDefaultBusinessWithoutActivatingIt() {
        givenSaveReturnsArgument();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(ACTIVO_ID);

        NegocioDeLaCuentaItem response = crearNegocio.execute(
                new CrearNegocioRequest(USUARIO_ID, "  Nuevo negocio  ", null, null));

        assertThat(response.nombre()).isEqualTo("Nuevo negocio");
        assertThat(response.rubro()).isEqualTo("OTRO");
        assertThat(response.activo()).isFalse();
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        assertThat(captor.getValue()).extracting(
                Negocio::getDuenioId,
                Negocio::getComparteDatosZona,
                Negocio::getCreadoEn,
                Negocio::getActualizadoEn)
                .containsExactly(USUARIO_ID, true, FECHA, FECHA);
        verify(controlPlanService).validarAltaNegocio(USUARIO_ID, FECHA);
        verifyNoInteractions(geocodificadorPort, zonaRepository);
    }

    @Test
    @DisplayName("Valida la localidad, asigna la zona y guarda la ubicación elegida")
    void executeShouldValidateLocationAndAssignZone() {
        givenSaveReturnsArgument();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(ACTIVO_ID);
        when(geocodificadorPort.resolverLocalidades(LATITUD, LONGITUD, FECHA.toInstant()))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo")));
        Zona zona = Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").build();
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));

        crearNegocio.execute(new CrearNegocioRequest(USUARIO_ID, "Kiosco", RubroNegocio.KIOSCO,
                ubicacion("San Justo")));

        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        assertThat(captor.getValue()).extracting(
                Negocio::getRubro,
                Negocio::getDireccion,
                Negocio::getLocalidad,
                Negocio::getZonaId)
                .containsExactly(RubroNegocio.KIOSCO, "Calle 123", "San Justo", ZONA_ID);
    }

    @Test
    @DisplayName("Una ubicación válida que no tiene zona se guarda sin asignación")
    void executeShouldAllowLocationOutsideKnownZones() {
        givenSaveReturnsArgument();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(ACTIVO_ID);
        when(geocodificadorPort.resolverLocalidades(LATITUD, LONGITUD, FECHA.toInstant()))
                .thenReturn(new LocalidadesDelPunto(List.of("CABA")));
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        crearNegocio.execute(new CrearNegocioRequest(USUARIO_ID, "Local", RubroNegocio.OTRO,
                ubicacion("CABA")));

        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        assertThat(captor.getValue().getZonaId()).isNull();
    }

    @Test
    @DisplayName("Rechaza una localidad que no coincide con el punto antes de guardar")
    void executeShouldRejectMismatchingLocation() {
        when(geocodificadorPort.resolverLocalidades(LATITUD, LONGITUD, FECHA.toInstant()))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo")));

        assertThatThrownBy(() -> crearNegocio.execute(new CrearNegocioRequest(
                USUARIO_ID, "Local", RubroNegocio.OTRO, ubicacion("Ramos Mejía"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La localidad no coincide con la del mapa (San Justo)");

        verify(negocioRepository, never()).save(any());
        verifyNoInteractions(zonaRepository);
    }

    private UbicacionNegocioItem ubicacion(String localidad) {
        return new UbicacionNegocioItem("Calle 123", localidad, LATITUD, LONGITUD);
    }

    private void givenSaveReturnsArgument() {
        when(negocioRepository.save(any())).thenAnswer(invocation -> {
            Negocio negocio = invocation.getArgument(0);
            negocio.setId(NEGOCIO_ID);
            return negocio;
        });
    }
}
