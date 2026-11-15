package com.ensambla.backend.domain.useCase.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.LectorDePixelesPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaResponse;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaResponse;
import com.ensambla.backend.infrastructure.config.useCase.MarcaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarcaCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);
    private static final byte[] IMAGEN = {1, 2, 3};
    private static final List<Integer> PIXELES_NARANJAS = List.of(0xF08C14, 0xF08C14);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);
    private final KitDeMarcaRepository kitDeMarcaRepository = mock(KitDeMarcaRepository.class, CALLS_REAL_METHODS);
    private final LectorDePixelesPort lectorDePixeles = mock(LectorDePixelesPort.class);
    private final MarcaUseCaseConfig config = new MarcaUseCaseConfig();
    private final ObtenerKitDeMarca obtenerKit = config.obtenerKitDeMarca(negocioRepository, kitDeMarcaRepository, CLOCK);
    private final GuardarKitDeMarca guardarKit = config.guardarKitDeMarca(
            negocioRepository, kitDeMarcaRepository, new TransaccionDirecta(), CLOCK);
    private final SubirLogoDeMarca subirLogo = config.subirLogoDeMarca(
            negocioRepository, kitDeMarcaRepository, lectorDePixeles, new TransaccionDirecta(), CLOCK);
    private final ObtenerLogoDeMarca obtenerLogo = config.obtenerLogoDeMarca(negocioRepository, kitDeMarcaRepository);

    private KitDeMarcaItem kitObtenido;

    private SubirLogoDeMarcaResponse logoSubido;

    private ObtenerLogoDeMarcaResponse logoObtenido;

    @Test
    @DisplayName("Obtener devuelve el kit guardado del negocio de la sesión")
    void obtenerKitShouldReturnStoredKit() {
        givenUnNegocioConKitGuardadoRojo();

        whenSeObtieneElKit();

        thenElKitEsRojo();
    }

    @Test
    @DisplayName("Obtener devuelve la paleta de Ensambla si el negocio todavía no armó su kit")
    void obtenerKitShouldReturnDefaultWhenMissing() {
        givenUnNegocioSinKit();

        whenSeObtieneElKit();

        assertThat(kitObtenido.colorPrincipal()).isEqualTo("#00506A");
        assertThat(kitObtenido.tieneLogo()).isFalse();
    }

    @Test
    @DisplayName("Obtener responde 404 si el usuario no tiene negocio")
    void obtenerKitShouldFailWhenUserHasNoNegocio() {
        givenUnUsuarioSinNegocio();

        assertThatThrownBy(this::whenSeObtieneElKit).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Guardar personaliza el kit del negocio y lo persiste")
    void guardarKitShouldPersistTheBrand() {
        givenUnNegocioSinKit();
        givenQueSaveDevuelveLoGuardado();

        whenSeGuardaElKitRojoYVerde("#CC0000", "#00CC00");

        thenElKitEsRojo();
        thenSeGuardoUnKit();
    }

    @Test
    @DisplayName("Guardar rechaza dos colores iguales sin guardar nada")
    void guardarKitShouldRejectEqualColors() {
        givenUnNegocioSinKit();

        assertThatThrownBy(() -> whenSeGuardaElKitRojoYVerde("#CC0000", "#cc0000"))
                .isInstanceOf(BusinessRuleException.class);
        thenNoSeGuardoNada();
    }

    @Test
    @DisplayName("Subir el logo lo guarda en el kit y sugiere colores desde sus píxeles")
    void subirLogoShouldStoreLogoAndSuggestColors() {
        givenUnNegocioSinKit();
        givenQueSaveDevuelveLoGuardado();
        givenUnLogoNaranja();

        whenSeSubeElLogo("image/png");

        assertThat(logoSubido.kit().tieneLogo()).isTrue();
        assertThat(logoSubido.colorPrincipalSugerido()).isEqualTo("#F08C14");
        assertThat(logoSubido.colorSecundarioSugerido()).isEqualTo("#00506A");
    }

    @Test
    @DisplayName("Subir un logo inválido no toca el kit")
    void subirLogoShouldRejectInvalidLogoWithoutSaving() {
        givenUnNegocioSinKit();

        assertThatThrownBy(() -> whenSeSubeElLogo("image/gif")).isInstanceOf(BusinessRuleException.class);
        thenNoSeGuardoNada();
    }

    @Test
    @DisplayName("Obtener el logo devuelve la imagen subida")
    void obtenerLogoShouldReturnTheImage() {
        givenUnNegocioConLogoGuardado();

        whenSeObtieneElLogo();

        assertThat(logoObtenido.contenido()).isEqualTo(IMAGEN);
        assertThat(logoObtenido.tipoMime()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Obtener el logo responde 404 si el negocio no tiene kit")
    void obtenerLogoShouldFailWhenThereIsNoKit() {
        givenUnNegocioSinKit();

        assertThatThrownBy(this::whenSeObtieneElLogo).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Obtener el logo responde 404 si el kit no tiene logo")
    void obtenerLogoShouldFailWhenThereIsNoLogo() {
        givenUnNegocioConKitGuardadoRojo();

        assertThatThrownBy(this::whenSeObtieneElLogo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenUnNegocioDelUsuario() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));
    }

    private void givenUnUsuarioSinNegocio() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private void givenUnNegocioSinKit() {
        givenUnNegocioDelUsuario();
        when(kitDeMarcaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenUnNegocioConKitGuardadoRojo() {
        givenUnNegocioDelUsuario();
        KitDeMarca kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
        kit.personalizar(ColorHex.de("#CC0000"), ColorHex.de("#00CC00"), Tipografia.NUNITO, TonoDeMarca.DIVERTIDO, AHORA);
        when(kitDeMarcaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(kit));
    }

    private void givenUnNegocioConLogoGuardado() {
        givenUnNegocioDelUsuario();
        KitDeMarca kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
        kit.ponerLogo(new LogoDeMarca(IMAGEN, "image/png"), AHORA);
        when(kitDeMarcaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(kit));
    }

    private void givenQueSaveDevuelveLoGuardado() {
        when(kitDeMarcaRepository.save(any(KitDeMarca.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void givenUnLogoNaranja() {
        when(lectorDePixeles.leer(any(LogoDeMarca.class))).thenReturn(PIXELES_NARANJAS);
    }

    private void whenSeObtieneElKit() {
        kitObtenido = obtenerKit.execute(new ObtenerKitDeMarcaRequest(USUARIO_ID));
    }

    private void whenSeGuardaElKitRojoYVerde(String principal, String secundario) {
        kitObtenido = guardarKit.execute(new GuardarKitDeMarcaRequest(
                USUARIO_ID, principal, secundario, Tipografia.NUNITO, TonoDeMarca.DIVERTIDO));
    }

    private void whenSeSubeElLogo(String tipoMime) {
        logoSubido = subirLogo.execute(new SubirLogoDeMarcaRequest(USUARIO_ID, IMAGEN, tipoMime));
    }

    private void whenSeObtieneElLogo() {
        logoObtenido = obtenerLogo.execute(new ObtenerLogoDeMarcaRequest(USUARIO_ID));
    }

    private void thenElKitEsRojo() {
        assertThat(kitObtenido).isEqualTo(
                new KitDeMarcaItem("#CC0000", "#00CC00", Tipografia.NUNITO, TonoDeMarca.DIVERTIDO, false));
    }

    private void thenSeGuardoUnKit() {
        verify(kitDeMarcaRepository).save(any(KitDeMarca.class));
    }

    private void thenNoSeGuardoNada() {
        verify(kitDeMarcaRepository, never()).save(any(KitDeMarca.class));
    }
}
