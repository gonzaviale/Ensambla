package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IBloqueJpaRepositoryTest extends PostgresDatosTestSupport {

    @Test
    @DisplayName("Borrar un bloque elimina en cascada sus campos y registros")
    void deleteByIdAndNegocioIdShouldCascadeToCamposAndRegistros() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID bloqueId = givenBloque(negocioId, "Productos");
        UUID campoId = givenCampo(negocioId, bloqueId, "Nombre", TipoCampo.TEXTO, 0);
        UUID registroId = givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Vela"), AHORA);

        bloques.deleteByIdAndNegocioId(bloqueId, negocioId);

        assertThat(bloques.findById(bloqueId)).isEmpty();
        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).isEmpty();
    }

    @Test
    @DisplayName("Borrar con el negocio equivocado no toca el bloque ajeno")
    void deleteByIdAndNegocioIdShouldNotDeleteBloqueOfOtherNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID bloqueDeA = givenBloque(negocioA, "Productos");

        bloques.deleteByIdAndNegocioId(bloqueDeA, negocioB);

        assertThat(bloques.findByIdAndNegocioId(bloqueDeA, negocioA)).isPresent();
    }
}
