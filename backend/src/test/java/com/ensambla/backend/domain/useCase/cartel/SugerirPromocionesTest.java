package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromocionesResponse;
import com.ensambla.backend.infrastructure.config.useCase.CartelUseCaseConfig;
import com.ensambla.backend.support.CartelesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SugerirPromocionesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final SugerirPromociones sugerir = new CartelUseCaseConfig().sugerirPromociones(negocioRepository, avisoRepository);

    private SugerirPromocionesResponse respuesta;

    @Test
    @DisplayName("Sugiere las promociones de los avisos pendientes de productos estancados y de combos, con sus números")
    void executeShouldSuggestFromPendingNotices() {
        givenUnNegocioConAvisosPendientes();

        whenSePidenLasSugerencias();

        assertThat(respuesta.promociones()).extracting(PromocionSugeridaItem::descuentoPorcentaje)
                .extracting(BigDecimal::intValue).containsExactly(20, 10);
        assertThat(respuesta.promociones().getFirst().productos()).containsExactly("Aceite");
        assertThat(respuesta.promociones().getLast().ahorro()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("Un aviso que no trae los números para armar la promoción se saltea")
    void executeShouldSkipNoticesWithoutNumbers() {
        givenUnNegocioConUnAvisoIncompleto();

        whenSePidenLasSugerencias();

        assertThat(respuesta.promociones()).isEmpty();
    }

    @Test
    @DisplayName("Responde 404 si el usuario no tiene negocio")
    void executeShouldFailWhenUserHasNoNegocio() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(this::whenSePidenLasSugerencias).isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenUnNegocioConAvisosPendientes() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(CartelesDePrueba.NEGOCIO_ID));
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(CartelesDePrueba.NEGOCIO_ID, TipoAviso.ESTANCADO,
                EstadoAviso.PENDIENTE)).thenReturn(List.of(Aviso.builder().tipo(TipoAviso.ESTANCADO)
                .titulo("Aceite está caro y no se mueve").datos(Map.of("precio", 2500, "sugerido", 2000)).build()));
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(CartelesDePrueba.NEGOCIO_ID, TipoAviso.COMBO,
                EstadoAviso.PENDIENTE)).thenReturn(List.of(Aviso.builder().tipo(TipoAviso.COMBO)
                .titulo("Alfajor y Gaseosa se venden juntos")
                .datos(Map.of("precioA", 900, "precioB", 1600, "sugerido", 2250)).build()));
    }

    private void givenUnNegocioConUnAvisoIncompleto() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(CartelesDePrueba.NEGOCIO_ID));
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(CartelesDePrueba.NEGOCIO_ID, TipoAviso.ESTANCADO,
                EstadoAviso.PENDIENTE)).thenReturn(List.of(Aviso.builder().tipo(TipoAviso.ESTANCADO)
                .titulo("Aceite está caro y no se mueve").datos(Map.of()).build()));
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(CartelesDePrueba.NEGOCIO_ID, TipoAviso.COMBO,
                EstadoAviso.PENDIENTE)).thenReturn(List.of());
    }

    private void whenSePidenLasSugerencias() {
        respuesta = sugerir.execute(new SugerirPromocionesRequest(USUARIO_ID));
    }
}
