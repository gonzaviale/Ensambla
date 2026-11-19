package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TipoDeCampoEnPantallaTest {

    private String clave;
    private TipoCampo tipo;

    @Test
    @DisplayName("Una fórmula se muestra en pantalla como número")
    void unaFormulaSeMuestraComoNumero() {
        cuandoSePideLaClaveDe(TipoCampo.FORMULA);

        entoncesLaClaveEs("number");
    }

    @Test
    @DisplayName("Cada tipo del dominio que se puede crear desde la pantalla vuelve al mismo tipo al guardar")
    void cadaTipoVuelveAlMismoTipoAlGuardar() {
        entoncesTodosLosTiposSalvoFormulaVuelvenAlMismoTipo();
    }

    @Test
    @DisplayName("Una clave que la pantalla no conoce se guarda como texto")
    void unaClaveDesconocidaSeGuardaComoTexto() {
        cuandoSeGuardaLaClave("columna-rara");

        entoncesElTipoEs(TipoCampo.TEXTO);
    }

    private void cuandoSePideLaClaveDe(TipoCampo tipoDelDominio) {
        clave = TipoDeCampoEnPantalla.claveDe(tipoDelDominio);
    }

    private void cuandoSeGuardaLaClave(String clavePedida) {
        tipo = TipoDeCampoEnPantalla.tipoAlGuardar(clavePedida);
    }

    private void entoncesLaClaveEs(String esperada) {
        assertThat(clave).isEqualTo(esperada);
    }

    private void entoncesElTipoEs(TipoCampo esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }

    private void entoncesTodosLosTiposSalvoFormulaVuelvenAlMismoTipo() {
        assertThat(Arrays.stream(TipoCampo.values()).filter(tipoDelDominio -> tipoDelDominio != TipoCampo.FORMULA))
                .allSatisfy(tipoDelDominio -> assertThat(TipoDeCampoEnPantalla.tipoAlGuardar(
                        TipoDeCampoEnPantalla.claveDe(tipoDelDominio))).isEqualTo(tipoDelDominio));
    }
}
