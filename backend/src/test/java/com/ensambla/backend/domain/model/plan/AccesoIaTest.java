package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccesoIaTest {

    @Test
    @DisplayName("Sin restringir, la IA es para cualquier email, incluso uno desconocido")
    void permiteShouldAllowEveryoneWhenNotRestricted() {
        assertThat(AccesoIa.LIBRE.permite("cualquiera@example.com")).isTrue();
        assertThat(AccesoIa.LIBRE.permite(null)).isTrue();
    }

    @Test
    @DisplayName("Restringido, sólo pasan los emails de la lista sin importar mayúsculas ni espacios")
    void permiteShouldAllowOnlyListedEmailsWhenRestricted() {
        AccesoIa acceso = new AccesoIa(true, Set.of(" Gonzalo@Example.com ", "otra@example.com"));

        assertThat(acceso.permite("gonzalo@example.com")).isTrue();
        assertThat(acceso.permite("OTRA@example.com ")).isTrue();
        assertThat(acceso.permite("ajena@example.com")).isFalse();
    }

    @Test
    @DisplayName("Restringido, un email ausente no pasa")
    void permiteShouldRejectMissingEmailWhenRestricted() {
        assertThat(new AccesoIa(true, Set.of("a@example.com")).permite(null)).isFalse();
    }

    @Test
    @DisplayName("Restringido con la lista vacía no deja pasar a nadie")
    void permiteShouldRejectEveryoneWhenRestrictedWithoutEmails() {
        assertThat(new AccesoIa(true, null).permite("a@example.com")).isFalse();
        assertThat(new AccesoIa(true, Set.of()).permite("a@example.com")).isFalse();
    }

    @Test
    @DisplayName("Ignora los emails vacíos o nulos de la lista")
    void constructorShouldDiscardBlankAndNullEmails() {
        AccesoIa acceso = new AccesoIa(true, new HashSet<>(Arrays.asList("a@example.com", " ", "", null)));

        assertThat(acceso.emails()).containsExactly("a@example.com");
    }
}
