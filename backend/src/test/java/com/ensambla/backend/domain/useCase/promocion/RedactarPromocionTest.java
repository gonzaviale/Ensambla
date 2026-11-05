package com.ensambla.backend.domain.useCase.promocion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.promocion.CanalEstadoWhatsapp;
import com.ensambla.backend.domain.model.promocion.CanalInstagram;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocion;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest.ProductoARedactar;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PromocionDePrueba.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedactarPromocionTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final ProductoARedactar YERBA = new ProductoARedactar("Yerba Playadito 1 kg", new BigDecimal("3600"));

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);

    private RedactarPromocion redactarPromocion;

    @BeforeEach
    void setUp() {
        redactarPromocion = new RedactarPromocion(negocioRepository,
                new CatalogoDeCanales(List.of(new CanalInstagram(), new CanalEstadoWhatsapp())));
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
    }

    @Test
    @DisplayName("Escribe la promoción con el tono del canal elegido y el nombre del negocio de la sesión")
    void executeShouldWriteThePromotionForTheChosenChannel() {
        RedactarPromocionResponse result = whenRedacta("estado", List.of(YERBA));

        assertThat(result.texto()).isEqualTo("*Tu producto estrella*\nLo que más se vendió\n\n"
                + "• Yerba Playadito 1 kg: $ 3.600\n\nAlmacén de Ana");
    }

    @Test
    @DisplayName("Rechaza un canal que no conoce")
    void executeShouldRejectAnUnknownChannel() {
        assertThatThrownBy(() -> whenRedacta("telegram", List.of(YERBA)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No conocemos ese canal");
    }

    @Test
    @DisplayName("Rechaza una promoción sin productos")
    void executeShouldRejectAPromotionWithoutProducts() {
        assertThatThrownBy(() -> whenRedacta("instagram", List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un producto");
    }

    // --- act ---
    private RedactarPromocionResponse whenRedacta(String canal, List<ProductoARedactar> productos) {
        return redactarPromocion.execute(new RedactarPromocionRequest(
                USUARIO_ID, canal, "estrella", "Tu producto estrella", "Lo que más se vendió", productos));
    }
}
