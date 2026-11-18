package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
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
        UUID negocioId = dadoNegocio("a@example.com").getId();
        UUID bloqueId = dadoBloque(NegocioId.de(negocioId), "Productos");
        UUID campoId = dadoCampo(NegocioId.de(negocioId), BloqueId.de(bloqueId), "Nombre", TipoCampo.TEXTO, 0);
        UUID registroId = dadoRegistro(NegocioId.de(negocioId), BloqueId.de(bloqueId), Map
                .of(campoId.toString(), "Vela"), AHORA);

        bloques.deleteByIdAndNegocioId(bloqueId, negocioId);

        assertThat(bloques.findById(bloqueId)).isEmpty();
        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).isEmpty();
    }

    @Test
    @DisplayName("Borrar con el negocio equivocado no toca el bloque ajeno")
    void deleteByIdAndNegocioIdShouldNotDeleteBloqueOfOtherNegocio() {
        UUID negocioA = dadoNegocio("a@example.com").getId();
        UUID negocioB = dadoNegocio("b@example.com").getId();
        UUID bloqueDeA = dadoBloque(NegocioId.de(negocioA), "Productos");

        bloques.deleteByIdAndNegocioId(bloqueDeA, negocioB);

        assertThat(bloques.findByIdAndNegocioId(bloqueDeA, negocioA)).isPresent();
    }
}
