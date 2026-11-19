package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DestinoDeCobroTest {

    private DestinoDeCobro destino;
    private Throwable rechazo;

    @Test
    @DisplayName("Recorta los espacios del alias y del titular")
    void recortaLosEspaciosDelAliasYDelTitular() {
        cuandoSeArmaElDestino("  mi.alias.mp  ", "  Ana Pérez ");

        entoncesElDestinoEs("mi.alias.mp", "Ana Pérez");
    }

    @Test
    @DisplayName("Acepta cualquier texto como alias sin validar su formato")
    void aceptaCualquierTextoComoAliasSinValidarSuFormato() {
        cuandoSeArmaElDestino("¡Mi alias! 123 @#", null);

        entoncesElDestinoEs("¡Mi alias! 123 @#", null);
    }

    @Test
    @DisplayName("Acepta un alias de exactamente 120 caracteres")
    void aceptaUnAliasDeExactamente120Caracteres() {
        cuandoSeArmaElDestino("a".repeat(120), null);

        entoncesElDestinoEstaCargado();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un alias vacío deja el destino sin cargar")
    void unAliasVacioDejaElDestinoSinCargar(String alias) {
        cuandoSeArmaElDestino(alias, "Ana Pérez");

        entoncesElDestinoNoEstaCargado();
    }

    @Test
    @DisplayName("Un titular vacío queda sin titular")
    void unTitularVacioQuedaSinTitular() {
        cuandoSeArmaElDestino("mi.alias.mp", "   ");

        entoncesElDestinoEs("mi.alias.mp", null);
    }

    @Test
    @DisplayName("Rechaza un alias de más de 120 caracteres")
    void rechazaUnAliasDeMasDe120Caracteres() {
        cuandoSeIntentaArmarElDestino("a".repeat(121), null);

        entoncesSeRechazaPorque("El alias o CVU no puede superar los 120 caracteres");
    }

    @Test
    @DisplayName("Rechaza un titular de más de 120 caracteres")
    void rechazaUnTitularDeMasDe120Caracteres() {
        cuandoSeIntentaArmarElDestino("mi.alias.mp", "a".repeat(121));

        entoncesSeRechazaPorque("El titular no puede superar los 120 caracteres");
    }

    // --- cuando ---
    private void cuandoSeArmaElDestino(String alias, String titular) {
        destino = DestinoDeCobro.de(alias, titular);
    }

    private void cuandoSeIntentaArmarElDestino(String alias, String titular) {
        rechazo = catchThrowable(() -> cuandoSeArmaElDestino(alias, titular));
    }

    // --- entonces ---
    private void entoncesElDestinoEs(String alias, String titular) {
        assertThat(destino.alias()).isEqualTo(alias);
        assertThat(destino.titular()).isEqualTo(titular);
    }

    private void entoncesElDestinoEstaCargado() {
        assertThat(destino.estaCargado()).isTrue();
    }

    private void entoncesElDestinoNoEstaCargado() {
        assertThat(destino).isEqualTo(DestinoDeCobro.SIN_CARGAR);
        assertThat(destino.estaCargado()).isFalse();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }
}
