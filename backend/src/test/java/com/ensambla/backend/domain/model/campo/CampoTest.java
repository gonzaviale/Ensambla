package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampoTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Un campo marcado como obligatorio es obligatorio")
    void esObligatorioShouldBeTrueWhenMarked() {
        assertThat(campoObligatorio(true).esObligatorio()).isTrue();
    }

    @Test
    @DisplayName("Un campo sin marcar o sin dato no es obligatorio")
    void esObligatorioShouldBeFalseWhenNotMarkedOrNull() {
        assertThat(campoObligatorio(false).esObligatorio()).isFalse();
        assertThat(campoObligatorio(null).esObligatorio()).isFalse();
    }

    @Test
    @DisplayName("Sin indicar tipo, el campo es de texto")
    void builderShouldBuildTextoByDefault() {
        assertThat(Campo.builder().build()).isInstanceOf(CampoTexto.class);
    }

    @Test
    @DisplayName("Un campo de texto no es número, conexión ni fórmula, y no tiene datos de otros tipos")
    void aTextoCampoShouldHaveNoDataOfOtherTypes() {
        Campo texto = Campo.builder().tipo(TipoCampo.TEXTO).build();

        assertThat(texto.esNumerico()).isFalse();
        assertThat(texto.esRelacion()).isFalse();
        assertThat(texto.esFormula()).isFalse();
        assertThat(texto.getOpciones()).isEmpty();
        assertThat(texto.getBloqueRelacionadoId()).isNull();
        assertThat(texto.getFormula()).isNull();
    }

    @Test
    @DisplayName("Un campo nuevo queda en el lugar del bloque con las dos fechas iguales")
    void ubicadoEnShouldPlaceTheCampoInTheBloque() {
        Campo campo = Campo.builder().nombre("Talle").build();

        Campo ubicado = campo.ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) 2, AHORA);

        assertThat(ubicado).isSameAs(campo);
        assertThat(ubicado.getId()).isNull();
        assertThat(ubicado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(ubicado.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(ubicado.getPosicion()).isEqualTo((short) 2);
        assertThat(ubicado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(ubicado.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("El campo que reemplaza a otro conserva su id, su lugar y su fecha de creación")
    void reemplazandoAShouldKeepIdentityAndPlace() {
        Campo anterior = campoGuardado();
        Campo nuevo = Campo.builder().nombre("Precio").tipo(TipoCampo.NUMERO).build();

        Campo reemplazo = nuevo.reemplazandoA(anterior, AHORA.plusHours(1));

        assertThat(reemplazo.getNombre()).isEqualTo("Precio");
        assertThat(reemplazo.getTipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(reemplazo.getId()).isEqualTo(CAMPO_ID);
        assertThat(reemplazo.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(reemplazo.getPosicion()).isEqualTo((short) 3);
        assertThat(reemplazo.getClave()).isEqualTo("price");
        assertThat(reemplazo.getMinimo()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(reemplazo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(reemplazo.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Un campo que ninguna fórmula usa se puede eliminar")
    void validarSinFormulasQueLoUsenShouldPassWhenNoFormulaUsesIt() {
        Campo precio = precioNumerico();

        assertThatCode(() -> precio.validarSinFormulasQueLoUsen(List.of(precio), "eliminar"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un campo del que depende una fórmula no se puede eliminar")
    void validarSinFormulasQueLoUsenShouldRejectWhenAFormulaUsesIt() {
        Campo precio = precioNumerico();
        Campo total = formulaQueUsa(precio);

        assertThatThrownBy(() -> precio.validarSinFormulasQueLoUsen(List.of(precio, total), "eliminar"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede eliminar «Precio»: la fórmula «Total» lo usa");
    }

    @Test
    @DisplayName("Un campo de texto no puede cambiar de tipo si una fórmula lo usa")
    void validarCambioDeTipoShouldRejectWhenAFormulaUsesIt() {
        Campo precio = Campo.builder().id(CAMPO_ID).tipo(TipoCampo.TEXTO).nombre("Precio").build();
        Campo total = formulaQueUsa(precio);

        assertThatThrownBy(() -> precio.validarCambioDeTipo(List.of(precio, total)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cambiar el tipo de");
    }

    private Campo campoObligatorio(Boolean obligatorio) {
        return Campo.builder().obligatorio(obligatorio).build();
    }

    private Campo campoGuardado() {
        return Campo.builder()
                .id(CAMPO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .posicion((short) 3)
                .clave("price")
                .minimo(BigDecimal.TEN)
                .creadoEn(AHORA)
                .build();
    }

    private Campo precioNumerico() {
        return Campo.builder().id(CAMPO_ID).tipo(TipoCampo.NUMERO).nombre("Precio").build();
    }

    private Campo formulaQueUsa(Campo campo) {
        return Campo.builder()
                .tipo(TipoCampo.FORMULA)
                .nombre("Total")
                .formula("{" + campo.getId() + "} * 2")
                .build();
    }
}
