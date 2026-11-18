package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
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

import static com.ensambla.backend.support.NegociosDePrueba.unNegocioConPuntoSinZona;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioConPuntoYZona;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioLlamado;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioSinUbicacion;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioSinUbicacionEnUnaZonaAnterior;
import static com.ensambla.backend.support.NegociosDePrueba.unaZonaLlamada;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final GeocodificadorPort geocodificadorPort = mock(GeocodificadorPort.class);
    private final ActualizarNegocio actualizarNegocio = new ActualizarNegocio(negocioRepository, zonaRepository,
            geocodificadorPort, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private Zona zona;
    private Negocio negocio;
    private ConfiguracionNegocioResponse respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Asigna la zona que contiene el punto al guardar la ubicación y la devuelve en la configuración")
    void asignaLaZonaQueContieneElPuntoAlGuardarLaUbicacionYLaDevuelveEnLaConfiguracion() {
        dadoUnNegocioSinUbicacionQueSeUbicaEnUnaZonaConocida();

        cuandoSeActualizaLaUbicacion();

        entoncesElNegocioGuardadoQuedaEnLaZona();
        entoncesSeRespondeConLaZona("San Justo - UNLaM");
    }

    @Test
    @DisplayName("Deja el negocio sin zona si ninguna contiene el punto")
    void dejaElNegocioSinZonaSiNingunaContieneElPunto() {
        dadoUnNegocioConZonaAnteriorYUnPuntoFueraDeTodaZona();

        cuandoSeActualizaLaUbicacion();

        entoncesElNegocioGuardadoQuedaSinZona();
        entoncesLaRespuestaNoTieneZona();
    }

    @Test
    @DisplayName("Completa la zona de un negocio con punto y sin zona aunque el punto no cambie")
    void completaLaZonaDeUnNegocioConPuntoYSinZonaAunqueElPuntoNoCambie() {
        dadoUnNegocioConElMismoPuntoSinZonaEnUnaZonaConocida();

        cuandoSeActualizaLaUbicacion();

        entoncesElNegocioGuardadoQuedaEnLaZona();
    }

    @Test
    @DisplayName("Conserva la zona y no la vuelve a resolver si el punto no cambió")
    void conservaLaZonaYNoLaVuelveAResolverSiElPuntoNoCambio() {
        dadoUnNegocioConElMismoPuntoYSuZona();

        cuandoSeActualizaLaUbicacion();

        entoncesElNegocioGuardadoConservaSuZona();
        entoncesNoSeResuelveNingunaZona();
    }

    @Test
    @DisplayName("Acepta la localidad que coincide con la del mapa aunque cambie mayúsculas")
    void aceptaLaLocalidadQueCoincideConLaDelMapaAunqueCambienMayusculas() {
        dadoUnNegocioSinUbicacionYUnMapaQueReconoceSanJusto();

        cuandoSeActualizaLaLocalidadEscritaComo("san justo");

        entoncesSeConsultoElMapaYSeGuardoElNegocio();
    }

    @Test
    @DisplayName("No guarda nada si la localidad no coincide con la del mapa y dice cuál es")
    void noGuardaNadaSiLaLocalidadNoCoincideConLaDelMapaYDiceCualEs() {
        dadoUnNegocioSinPuntoYUnMapaQueReconoceRamosMejia();

        cuandoSeIntentaActualizarLaUbicacion();

        entoncesSeRechazaPorque("La localidad no coincide con la del mapa (Ramos Mejía)");
        entoncesElNegocioNoRecibioElPunto();
        entoncesNoSeGuardaNingunNegocio();
    }

    @Test
    @DisplayName("Propaga la indisponibilidad del mapa al verificar la localidad")
    void propagaLaIndisponibilidadDelMapaAlVerificarLaLocalidad() {
        dadoUnMapaCaido();

        cuandoSeIntentaActualizarLaUbicacion();

        entoncesSeRechazaPorqueElMapaNoEstaDisponible();
    }

    @Test
    @DisplayName("No consulta al mapa si la ubicación enviada es la ya guardada")
    void noConsultaAlMapaSiLaUbicacionEnviadaEsLaYaGuardada() {
        dadoUnNegocioConLaMismaUbicacionYSuZona();

        cuandoSeActualizaLaUbicacion();

        entoncesNoSeConsultaElMapa();
    }

    @Test
    @DisplayName("Sin ubicación no verifica la localidad ni resuelve zona")
    void sinUbicacionNoVerificaLaLocalidadNiResuelveZona() {
        dadoUnNegocioSinUbicacion();

        cuandoSeActualizaSoloElNombreYElRubro();

        entoncesLaRespuestaTieneElNombreYElRubroNuevos();
        entoncesElNegocioGuardadoQuedaSinZona();
        entoncesNoSeConsultaElMapa();
        entoncesNoSeResuelveNingunaZona();
    }

    @Test
    @DisplayName("Responde no encontrado al actualizar la configuración de un usuario sin negocio")
    void respondeNoEncontradoAlActualizarLaConfiguracionDeUnUsuarioSinNegocio() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaActualizarSinCambios();

        entoncesSeRechazaPorqueNoHayNegocio();
    }

    // --- dado ---
    private void dadoUnNegocioSinUbicacionQueSeUbicaEnUnaZonaConocida() {
        zona = unaZonaLlamada("San Justo - UNLaM");
        dadoUnNegocioParaActualizar(unNegocioSinUbicacion());
        dadoQueElMapaReconoce("San Justo");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        when(zonaRepository.findByIdSiTiene(zona.getId())).thenReturn(Optional.of(zona));
    }

    private void dadoUnNegocioConZonaAnteriorYUnPuntoFueraDeTodaZona() {
        dadoUnNegocioParaActualizar(unNegocioSinUbicacionEnUnaZonaAnterior());
        dadoQueElMapaReconoce("San Justo");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());
    }

    private void dadoUnNegocioConElMismoPuntoSinZonaEnUnaZonaConocida() {
        zona = unaZonaLlamada("San Justo - UNLaM");
        dadoUnNegocioParaActualizar(unNegocioConPuntoSinZona(LATITUD, LONGITUD));
        dadoQueElMapaReconoce("San Justo");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
    }

    private void dadoUnNegocioConElMismoPuntoYSuZona() {
        zona = unaZonaLlamada("San Justo - UNLaM");
        dadoUnNegocioParaActualizar(unNegocioConPuntoYZona(LATITUD, LONGITUD, "San Justo", zona.getId()));
    }

    private void dadoUnNegocioSinUbicacionYUnMapaQueReconoceSanJusto() {
        dadoUnNegocioParaActualizar(unNegocioSinUbicacion());
        dadoQueElMapaReconoce("San Justo");
    }

    private void dadoUnNegocioSinPuntoYUnMapaQueReconoceRamosMejia() {
        dadoUnNegocioParaActualizar(unNegocioLlamado("Mi negocio"));
        dadoQueElMapaReconoce("Ramos Mejía");
    }

    private void dadoUnMapaCaido() {
        dadoUnNegocioParaActualizar(unNegocioSinUbicacion());
        when(geocodificadorPort.resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant()))
                .thenThrow(new ServicioUbicacionException("Sin servicio"));
    }

    private void dadoUnNegocioConLaMismaUbicacionYSuZona() {
        dadoUnNegocioParaActualizar(unNegocioConPuntoYZona(LATITUD, LONGITUD, "San Justo", ZonaId.nuevo()));
    }

    private void dadoUnNegocioSinUbicacion() {
        dadoUnNegocioParaActualizar(unNegocioSinUbicacion());
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnNegocioParaActualizar(Negocio paraActualizar) {
        negocio = paraActualizar;
        when(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(zonaRepository.findByIdSiTiene(any())).thenReturn(Optional.empty());
    }

    private void dadoQueElMapaReconoce(String localidad) {
        when(geocodificadorPort.resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant()))
                .thenReturn(new LocalidadesDelPunto(List.of(localidad)));
    }

    // --- cuando ---
    private void cuandoSeActualizaLaUbicacion() {
        respuesta = actualizarNegocio.execute(new ActualizarNegocioRequest(USUARIO_ID, null, null, UBICACION, null));
    }

    private void cuandoSeIntentaActualizarLaUbicacion() {
        rechazo = catchThrowable(this::cuandoSeActualizaLaUbicacion);
    }

    private void cuandoSeActualizaLaLocalidadEscritaComo(String localidad) {
        respuesta = actualizarNegocio.execute(new ActualizarNegocioRequest(USUARIO_ID, null, null,
                new UbicacionNegocioItem("Calle 123", localidad, LATITUD, LONGITUD), null));
    }

    private void cuandoSeActualizaSoloElNombreYElRubro() {
        respuesta = actualizarNegocio.execute(
                new ActualizarNegocioRequest(USUARIO_ID, "Nuevo nombre", RubroNegocio.KIOSCO, null, null));
    }

    private void cuandoSeIntentaActualizarSinCambios() {
        rechazo = catchThrowable(() -> actualizarNegocio.execute(
                new ActualizarNegocioRequest(USUARIO_ID, null, null, null, null)));
    }

    // --- entonces ---
    private void entoncesElNegocioGuardadoQuedaEnLaZona() {
        assertThat(negocioGuardado().getZonaId()).isEqualTo(zona.getId());
    }

    private void entoncesElNegocioGuardadoQuedaSinZona() {
        assertThat(negocioGuardado().getZonaId()).isNull();
    }

    private void entoncesElNegocioGuardadoConservaSuZona() {
        assertThat(negocioGuardado().getZonaId()).isEqualTo(zona.getId());
    }

    private void entoncesSeRespondeConLaZona(String nombre) {
        assertThat(respuesta.zona()).isEqualTo(nombre);
    }

    private void entoncesLaRespuestaNoTieneZona() {
        assertThat(respuesta.zona()).isNull();
    }

    private void entoncesLaRespuestaTieneElNombreYElRubroNuevos() {
        assertThat(respuesta.nombre()).isEqualTo("Nuevo nombre");
        assertThat(respuesta.rubro()).isEqualTo(RubroNegocio.KIOSCO);
    }

    private void entoncesSeConsultoElMapaYSeGuardoElNegocio() {
        verify(geocodificadorPort).resolverLocalidades(LATITUD, LONGITUD, AHORA.toInstant());
        verify(negocioRepository).save(any());
    }

    private void entoncesNoSeConsultaElMapa() {
        verifyNoInteractions(geocodificadorPort);
    }

    private void entoncesNoSeResuelveNingunaZona() {
        verify(zonaRepository, never()).resolver(any(), any());
    }

    private void entoncesElNegocioNoRecibioElPunto() {
        assertThat(negocio.getLatitud()).isNull();
    }

    private void entoncesNoSeGuardaNingunNegocio() {
        verify(negocioRepository, never()).save(any());
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeRechazaPorqueElMapaNoEstaDisponible() {
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class);
    }

    private void entoncesSeRechazaPorqueNoHayNegocio() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private Negocio negocioGuardado() {
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        return captor.getValue();
    }
}
