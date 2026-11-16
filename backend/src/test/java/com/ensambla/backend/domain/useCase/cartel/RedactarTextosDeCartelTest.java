package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.RedaccionDeCartelIa;
import com.ensambla.backend.domain.model.cartel.TextosDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartel;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartelRequest;
import com.ensambla.backend.infrastructure.config.useCase.CartelUseCaseConfig;
import com.ensambla.backend.support.CartelesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedactarTextosDeCartelTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("prueba", "modelo", 10, 5);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);
    private final KitDeMarcaRepository kitDeMarcaRepository = mock(KitDeMarcaRepository.class, CALLS_REAL_METHODS);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final RedaccionDeCartelIaPort ia = mock(RedaccionDeCartelIaPort.class);
    private final RedactarTextosDeCartel redactar = new CartelUseCaseConfig().redactarTextosDeCartel(
            negocioRepository, kitDeMarcaRepository, controlPlanService, ia, CLOCK);

    private TextosDeCartelItem textos;

    @Test
    @DisplayName("Usa el texto de la IA cuando es coherente con los datos y descuenta un cartel del cupo")
    void executeShouldUseTheAiTextAndChargeTheQuota() {
        givenUnNegocioDonRufino();
        givenQueLaIaRedacta("Aceite a precio de vecino", "20 % menos", "El aceite pasa de $2.500 a $2.000");

        whenSeRedactan();

        assertThat(textos.redactadoConIa()).isTrue();
        assertThat(textos.titular()).isEqualTo("Aceite a precio de vecino");
        thenSeDescontoUnCartel();
    }

    @Test
    @DisplayName("Le cuenta a la IA el negocio, su rubro y su tono, y nada más de la persona")
    void executeShouldSendOnlyTheBusinessContext() {
        givenUnNegocioDonRufino();
        givenQueLaIaRedacta("Aceite a precio de vecino", "20 % menos", "Pasa de $2.500 a $2.000");

        whenSeRedactan();

        PedidoDeTextosDeCartel pedido = elPedidoALaIa();
        assertThat(pedido.negocio()).isEqualTo("Don Rufino");
        assertThat(pedido.rubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(pedido.tono()).isEqualTo(TonoDeMarca.CERCANO);
        assertThat(pedido.datos().productos()).containsExactly("Aceite de girasol 900 ml");
    }

    @Test
    @DisplayName("Si la IA dice un número que no coincide, usa los textos de reserva y no descuenta del cupo")
    void executeShouldFallBackWhenTheAiContradictsTheData() {
        givenUnNegocioDonRufino();
        givenQueLaIaRedacta("Aceite regalado", "70 % menos", "Ahora $999");

        whenSeRedactan();

        assertThat(textos.redactadoConIa()).isFalse();
        assertThat(textos.titular()).startsWith("¡Oferta en Aceite");
        thenNoSeDescontoNada();
    }

    @Test
    @DisplayName("Si la IA falla, usa los textos de reserva y no descuenta del cupo")
    void executeShouldFallBackWhenTheAiFails() {
        givenUnNegocioDonRufino();
        when(ia.redactar(any())).thenThrow(new IllegalStateException("sin respuesta"));

        whenSeRedactan();

        assertThat(textos.redactadoConIa()).isFalse();
        thenNoSeDescontoNada();
    }

    @Test
    @DisplayName("Sin cupo de carteles con IA responde el error del plan sin consultar a la IA")
    void executeShouldRejectWhenTheQuotaIsOver() {
        givenUnNegocioDonRufino();
        doThrow(new RestriccionPlanException("LIMITE_CARTELES_IA", "sin cupo"))
                .when(controlPlanService).exigirCupoDeCartelesIa(any(), any());

        assertThatThrownBy(this::whenSeRedactan).isInstanceOf(RestriccionPlanException.class);
        verify(ia, never()).redactar(any());
    }

    private void givenUnNegocioDonRufino() {
        Negocio negocio = Negocio.builder().id(CartelesDePrueba.NEGOCIO_ID).nombre("Don Rufino")
                .rubro(RubroNegocio.ALMACEN).build();
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(kitDeMarcaRepository.findByNegocioId(CartelesDePrueba.NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenQueLaIaRedacta(String titular, String bajada, String posteo) {
        TextosDeCartel redactados = new TextosDeCartel(TipoDeCartel.PROMO_PRECIO, titular, bajada, posteo);
        when(ia.redactar(any())).thenReturn(new RedaccionDeCartelIa(redactados, CONSUMO));
    }

    private void whenSeRedactan() {
        textos = redactar.execute(new RedactarTextosDeCartelRequest(USUARIO_ID, List.of(TipoDeCartel.PROMO_PRECIO),
                List.of("Aceite de girasol 900 ml"),
                List.of(new BigDecimal("2500"), new BigDecimal("2000"), new BigDecimal("20"))));
    }

    private PedidoDeTextosDeCartel elPedidoALaIa() {
        ArgumentCaptor<PedidoDeTextosDeCartel> pedido = ArgumentCaptor.forClass(PedidoDeTextosDeCartel.class);
        verify(ia).redactar(pedido.capture());
        return pedido.getValue();
    }

    private void thenSeDescontoUnCartel() {
        verify(controlPlanService).registrarCartelIa(CartelesDePrueba.NEGOCIO_ID, OffsetDateTime.now(CLOCK));
    }

    private void thenNoSeDescontoNada() {
        verify(controlPlanService, never()).registrarCartelIa(any(), any());
    }
}
