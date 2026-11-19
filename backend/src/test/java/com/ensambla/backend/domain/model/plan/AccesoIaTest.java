package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccesoIaTest {

    private AccesoIa acceso;
    private boolean permiteUnEmail;
    private boolean permiteOtroEmail;
    private boolean permiteUnEmailAjeno;
    private boolean permiteConListaNula;
    private boolean permiteConListaVacia;
    private Set<String> emailsConservados;

    @Test
    @DisplayName("Sin restringir, la IA es para cualquier email, incluso uno desconocido")
    void sinRestringirLaIAEsParaCualquierEmailInclusoUnoDesconocido() {
        dadoUnAccesoSinRestringir();

        cuandoSeConsultaElAccesoDe("cualquiera@example.com", null);

        entoncesPasanLosDosEmails();
    }

    @Test
    @DisplayName("Restringido, sólo pasan los emails de la lista sin importar mayúsculas ni espacios")
    void restringidoSoloPasanLosEmailsDeLaListaSinImportarMayusculasNiEspacios() {
        dadoUnAccesoRestringidoA(Set.of(" Gonzalo@Example.com ", "otra@example.com"));

        cuandoSeConsultanTresEmails();

        entoncesSoloPasanLosDosDeLaLista();
    }

    @Test
    @DisplayName("Restringido, un email ausente no pasa")
    void restringidoUnEmailAusenteNoPasa() {
        dadoUnAccesoRestringidoA(Set.of("a@example.com"));

        cuandoSeConsultaElAccesoDeUnEmailAusente();

        entoncesNoPasaElEmailAusente();
    }

    @Test
    @DisplayName("Restringido con la lista vacía no deja pasar a nadie")
    void restringidoConLaListaVaciaNoDejaPasarANadie() {
        cuandoSeConsultaUnAccesoRestringidoSinListaYConListaVacia();

        entoncesNoPasaNadie();
    }

    @Test
    @DisplayName("Ignora los emails vacíos o nulos de la lista")
    void ignoraLosEmailsVaciosONulosDeLaLista() {
        dadoUnAccesoRestringidoA(new HashSet<>(Arrays.asList("a@example.com", " ", "", null)));

        cuandoSePidenLosEmailsConservados();

        entoncesSoloSeConservaElEmailValido();
    }

    // --- dado ---
    private void dadoUnAccesoSinRestringir() {
        acceso = AccesoIa.LIBRE;
    }

    private void dadoUnAccesoRestringidoA(Set<String> emails) {
        acceso = new AccesoIa(true, emails);
    }

    // --- cuando ---
    private void cuandoSeConsultaElAccesoDe(String unEmail, String otroEmail) {
        permiteUnEmail = acceso.permite(unEmail);
        permiteOtroEmail = acceso.permite(otroEmail);
    }

    private void cuandoSeConsultaElAccesoDeUnEmailAusente() {
        permiteUnEmail = acceso.permite(null);
    }

    private void cuandoSeConsultanTresEmails() {
        permiteUnEmail = acceso.permite("gonzalo@example.com");
        permiteOtroEmail = acceso.permite("OTRA@example.com ");
        permiteUnEmailAjeno = acceso.permite("ajena@example.com");
    }

    private void cuandoSeConsultaUnAccesoRestringidoSinListaYConListaVacia() {
        permiteConListaNula = new AccesoIa(true, null).permite("a@example.com");
        permiteConListaVacia = new AccesoIa(true, Set.of()).permite("a@example.com");
    }

    private void cuandoSePidenLosEmailsConservados() {
        emailsConservados = acceso.emails();
    }

    // --- entonces ---
    private void entoncesPasanLosDosEmails() {
        assertThat(permiteUnEmail).isTrue();
        assertThat(permiteOtroEmail).isTrue();
    }

    private void entoncesSoloPasanLosDosDeLaLista() {
        assertThat(permiteUnEmail).isTrue();
        assertThat(permiteOtroEmail).isTrue();
        assertThat(permiteUnEmailAjeno).isFalse();
    }

    private void entoncesNoPasaElEmailAusente() {
        assertThat(permiteUnEmail).isFalse();
    }

    private void entoncesNoPasaNadie() {
        assertThat(permiteConListaNula).isFalse();
        assertThat(permiteConListaVacia).isFalse();
    }

    private void entoncesSoloSeConservaElEmailValido() {
        assertThat(emailsConservados).containsExactly("a@example.com");
    }
}
