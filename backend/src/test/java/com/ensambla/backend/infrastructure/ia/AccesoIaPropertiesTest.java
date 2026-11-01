package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plan.AccesoIa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccesoIaPropertiesTest {

    @Test
    @DisplayName("Arma el acceso con los emails configurados")
    void accesoShouldCarryRestrictionAndEmails() {
        AccesoIa acceso = new AccesoIaProperties(true, List.of("a@example.com", "B@example.com")).acceso();

        assertThat(acceso.restringido()).isTrue();
        assertThat(acceso.emails()).containsExactlyInAnyOrder("a@example.com", "b@example.com");
    }

    @Test
    @DisplayName("Sin lista configurada nadie queda habilitado")
    void accesoShouldHaveNoEmailsWhenListIsMissing() {
        assertThat(new AccesoIaProperties(true, null).acceso().emails()).isEmpty();
    }

    @Test
    @DisplayName("Expone el acceso como bean de la configuración de IA")
    void accesoIaBeanShouldComeFromProperties() {
        assertThat(new IaConfig().accesoIa(new AccesoIaProperties(false, List.of())))
                .isEqualTo(AccesoIa.LIBRE);
    }
}
