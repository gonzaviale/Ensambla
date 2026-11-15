package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitDeMarcaTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");
    private static final OffsetDateTime DESPUES = AHORA.plusHours(1);
    private static final ColorHex ROJO = ColorHex.de("#CC0000");
    private static final ColorHex VERDE = ColorHex.de("#00CC00");

    private KitDeMarca kit;

    @Test
    @DisplayName("El kit predeterminado usa la paleta de Ensambla, sin logo")
    void predeterminadoShouldUseEnsamblaPalette() {
        whenSeCreaElKitPredeterminado();

        assertThat(kit.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(kit.getColorPrincipal().valor()).isEqualTo("#00506A");
        assertThat(kit.getColorSecundario().valor()).isEqualTo("#EE8000");
        assertThat(kit.getTipografia()).isEqualTo(Tipografia.PLUS_JAKARTA_SANS);
        assertThat(kit.getTono()).isEqualTo(TonoDeMarca.CERCANO);
        assertThat(kit.tieneLogo()).isFalse();
    }

    @Test
    @DisplayName("Personalizar cambia colores, tipografía y tono y mueve la fecha de actualización")
    void personalizarShouldChangeTheBrand() {
        givenUnKitPredeterminado();

        whenSePersonaliza(ROJO, VERDE);

        assertThat(kit.getColorPrincipal()).isEqualTo(ROJO);
        assertThat(kit.getColorSecundario()).isEqualTo(VERDE);
        assertThat(kit.getTipografia()).isEqualTo(Tipografia.NUNITO);
        assertThat(kit.getTono()).isEqualTo(TonoDeMarca.DIVERTIDO);
        assertThat(kit.getCreadoEn()).isEqualTo(AHORA);
        assertThat(kit.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Personalizar rechaza dos colores iguales")
    void personalizarShouldRejectEqualColors() {
        givenUnKitPredeterminado();

        assertThatThrownBy(() -> whenSePersonaliza(ROJO, ROJO))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("distintos");
    }

    @Test
    @DisplayName("Poner el logo lo deja en el kit")
    void ponerLogoShouldKeepTheLogo() {
        givenUnKitPredeterminado();

        whenSePoneUnLogo();

        assertThat(kit.tieneLogo()).isTrue();
        assertThat(kit.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void givenUnKitPredeterminado() {
        whenSeCreaElKitPredeterminado();
    }

    private void whenSeCreaElKitPredeterminado() {
        kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
    }

    private void whenSePersonaliza(ColorHex principal, ColorHex secundario) {
        kit.personalizar(principal, secundario, Tipografia.NUNITO, TonoDeMarca.DIVERTIDO, DESPUES);
    }

    private void whenSePoneUnLogo() {
        kit.ponerLogo(new LogoDeMarca(new byte[] {1}, "image/png"), DESPUES);
    }
}
