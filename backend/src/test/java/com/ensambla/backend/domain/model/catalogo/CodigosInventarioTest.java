package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigosInventarioTest {

    private static final CampoId CAMPO_ID = CampoId.nuevo();

    @Test
    @DisplayName("Rechaza GTIN equivalentes en dos productos del mismo inventario")
    void validarShouldRejectEquivalentDuplicates() {
        Bloque bloque = dadoBloque(TipoCampo.TEXTO);

        assertThatThrownBy(() -> CodigosInventario.validar(bloque,
                List.of(dadoRegistro("77903754"), dadoRegistro("00000077903754"))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Permite productos sin código y códigos diferentes sin modificar su stock")
    void validarShouldAllowMissingOrDifferentCodes() {
        Bloque bloque = dadoBloque(TipoCampo.TEXTO);

        assertThatCode(() -> CodigosInventario.validar(bloque,
                List.of(Registro.builder().valores(Map.of()).build(), dadoRegistro(""),
                        dadoRegistro("77903754"), dadoRegistro("77905741"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza un campo numérico para el código porque elimina los ceros iniciales")
    void validarShouldRejectNumericField() {
        Bloque bloque = dadoBloque(TipoCampo.NUMERO);

        assertThatThrownBy(() -> CodigosInventario.validar(bloque, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza un valor numérico aunque el campo de código sea texto")
    void validarShouldRejectNumericValue() {
        Bloque bloque = dadoBloque(TipoCampo.TEXTO);

        assertThatThrownBy(() -> CodigosInventario.validar(bloque, List.of(dadoRegistro(77903754))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("No interpreta como códigos los datos de otras tablas o columnas")
    void validarShouldIgnoreOtherBlocksAndFields() {
        Bloque otro = dadoBloque(TipoCampo.TEXTO);
        otro.setTipo(TipoBloque.VENTAS);
        Bloque sinCodigo = dadoBloque(TipoCampo.TEXTO);
        sinCodigo.getCampos().getFirst().setClave("name");

        assertThatCode(() -> {
            CodigosInventario.validar(otro, List.of(dadoRegistro("no es un código")));
            CodigosInventario.validar(sinCodigo, List.of(dadoRegistro("no es un código")));
        }).doesNotThrowAnyException();
    }

    // --- arrange ---
    private Bloque dadoBloque(TipoCampo tipo) {
        return Bloque.builder().tipo(TipoBloque.INVENTARIO).campos(List.of(
                Campo.builder().id(CAMPO_ID).clave("barcode").obligatorio(false).tipo(tipo).build())).build();
    }

    private Registro dadoRegistro(Object codigo) {
        return Registro.builder().valores(Map.of(CAMPO_ID.toString(), codigo)).build();
    }
}
