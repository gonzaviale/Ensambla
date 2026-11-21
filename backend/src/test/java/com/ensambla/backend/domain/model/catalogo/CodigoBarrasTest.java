package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CodigoBarrasTest {

    private CodigoBarras codigoBarras;
    private Throwable rechazo;

    @ParameterizedTest
    @ValueSource(strings = {"77903754", "00000077903754", "036000291452", "7790895000997"})
    @DisplayName("Admite GTIN válidos y conserva ceros iniciales al comparar presentaciones equivalentes")
    void admiteGtinValidosYConservaCerosInicialesAlCompararPresentacionesEquivalentes(String codigo) {
        cuandoSeCreaElCodigo(codigo);

        entoncesConservaElValorYTieneUnaIdentidadDeCatorceDigitos(codigo);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "abcd5678", "00000000", "77903755"})
    @DisplayName("Rechaza nulos, longitudes, caracteres, ceros y dígitos de control inválidos")
    void rechazaNulosLongitudesCaracteresCerosYDigitosDeControlInvalidos(String codigo) {
        cuandoSeIntentaCrearElCodigo(codigo);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    // --- cuando ---

    private void cuandoSeCreaElCodigo(String codigo) {
        codigoBarras = new CodigoBarras(codigo);
    }

    private void cuandoSeIntentaCrearElCodigo(String codigo) {
        rechazo = catchThrowable(() -> new CodigoBarras(codigo));
    }

    // --- entonces ---

    private void entoncesConservaElValorYTieneUnaIdentidadDeCatorceDigitos(String codigo) {
        assertThat(codigoBarras.valor()).isEqualTo(codigo);
        assertThat(codigoBarras.identidad()).hasSize(14).endsWith(codigo);
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }
}
