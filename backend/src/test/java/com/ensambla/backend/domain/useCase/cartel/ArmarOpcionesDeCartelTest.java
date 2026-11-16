package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.DibujanteDeCartelPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelRequest;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelResponse;
import com.ensambla.backend.infrastructure.config.useCase.CartelUseCaseConfig;
import com.ensambla.backend.support.CartelesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArmarOpcionesDeCartelTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);
    private final KitDeMarcaRepository kitDeMarcaRepository = mock(KitDeMarcaRepository.class, CALLS_REAL_METHODS);
    private final DibujanteDeCartelPort dibujante = mock(DibujanteDeCartelPort.class);
    private final ArmarOpcionesDeCartel armarOpciones = new CartelUseCaseConfig()
            .armarOpcionesDeCartel(negocioRepository, kitDeMarcaRepository, dibujante, CLOCK);

    private ArmarOpcionesDeCartelResponse respuesta;

    @Test
    @DisplayName("Arma las tres opciones del negocio de la sesión, con su marca, y las dibuja")
    void executeShouldBuildAndDrawThreeOptions() {
        givenUnNegocioSinKitDeDonRufino();
        givenUnDibujanteQueEscribeLaVariante();

        whenSePidenLasOpciones();

        assertThat(respuesta.opciones()).extracting(OpcionDeCartelItem::variante)
                .containsExactly(VarianteDeComposicion.values());
        assertThat(respuesta.opciones().getFirst().dibujo()).isEqualTo("dibujo de BLOQUE");
    }

    @Test
    @DisplayName("Responde 404 si el usuario no tiene negocio")
    void executeShouldFailWhenUserHasNoNegocio() {
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(this::whenSePidenLasOpciones).isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenUnNegocioSinKitDeDonRufino() {
        Negocio negocio = Negocio.builder().id(CartelesDePrueba.NEGOCIO_ID).nombre("Don Rufino").build();
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(kitDeMarcaRepository.findByNegocioId(CartelesDePrueba.NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenUnDibujanteQueEscribeLaVariante() {
        when(dibujante.dibujar(any(ComposicionDeCartel.class)))
                .thenAnswer(call -> "dibujo de " + ((ComposicionDeCartel) call.getArgument(0)).variante());
    }

    private void whenSePidenLasOpciones() {
        respuesta = armarOpciones.execute(new ArmarOpcionesDeCartelRequest(USUARIO_ID, TipoDeCartel.PROMO_PRECIO,
                FormatoDeCartel.A4, "Aceite a precio de vecino", "20 % menos", "$2.000", "Antes $2.500", List.of()));
    }
}
