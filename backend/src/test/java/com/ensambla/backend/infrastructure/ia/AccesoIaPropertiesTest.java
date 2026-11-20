package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plan.AccesoIa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccesoIaPropertiesTest {

    private AccesoIa acceso;

    @Test
    @DisplayName("Arma el acceso con los emails configurados")
    void armaElAccesoConLosEmailsConfigurados() {
        cuandoSeArmaElAccesoRestringidoDeDosEmails();

        entoncesElAccesoEstaRestringidoYGuardaLosEmailsEnMinusculas();
    }

    @Test
    @DisplayName("Sin lista configurada nadie queda habilitado")
    void sinListaConfiguradaNadieQuedaHabilitado() {
        cuandoSeArmaElAccesoRestringidoSinLista();

        entoncesNadieQuedaHabilitado();
    }

    @Test
    @DisplayName("Expone el acceso como bean de la configuración de IA")
    void exponeElAccesoComoBeanDeLaConfiguracionDeIa() {
        cuandoSeExponeElAccesoLibreComoBean();

        entoncesElAccesoEsLibre();
    }

    // --- cuando ---

    private void cuandoSeArmaElAccesoRestringidoDeDosEmails() {
        acceso = new AccesoIaProperties(true, List.of("a@example.com", "B@example.com")).acceso();
    }

    private void cuandoSeArmaElAccesoRestringidoSinLista() {
        acceso = new AccesoIaProperties(true, null).acceso();
    }

    private void cuandoSeExponeElAccesoLibreComoBean() {
        acceso = new IaConfig().accesoIa(new AccesoIaProperties(false, List.of()));
    }

    // --- entonces ---

    private void entoncesElAccesoEstaRestringidoYGuardaLosEmailsEnMinusculas() {
        assertThat(acceso.restringido()).isTrue();
        assertThat(acceso.emails()).containsExactlyInAnyOrder("a@example.com", "b@example.com");
    }

    private void entoncesNadieQuedaHabilitado() {
        assertThat(acceso.emails()).isEmpty();
    }

    private void entoncesElAccesoEsLibre() {
        assertThat(acceso).isEqualTo(AccesoIa.LIBRE);
    }
}
