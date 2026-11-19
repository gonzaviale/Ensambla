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

    private UUID negocioId;
    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueId;
    private UUID bloqueDeA;
    private UUID campoId;
    private UUID registroId;

    @Test
    @DisplayName("Borrar un bloque elimina en cascada sus campos y registros")
    void borrarUnBloqueEliminaEnCascadaSusCamposYRegistros() {
        dadoUnBloqueConUnCampoYUnRegistro();

        cuandoSeBorraElBloque();

        entoncesSeEliminanElBloqueSusCamposYSusRegistros();
    }

    @Test
    @DisplayName("Borrar con el negocio equivocado no toca el bloque ajeno")
    void borrarConElNegocioEquivocadoNoTocaElBloqueAjeno() {
        dadoUnBloqueDeUnNegocioYOtroNegocio();

        cuandoSeBorraElBloqueConElNegocioEquivocado();

        entoncesElBloqueAjenoSigueExistiendo();
    }

    // --- dado ---
    private void dadoUnBloqueConUnCampoYUnRegistro() {
        negocioId = givenNegocio("a@example.com").getId();
        bloqueId = givenBloque(NegocioId.de(negocioId), "Productos");
        campoId = givenCampo(NegocioId.de(negocioId), BloqueId.de(bloqueId), "Nombre", TipoCampo.TEXTO, 0);
        registroId = givenRegistro(NegocioId.de(negocioId), BloqueId.de(bloqueId), Map
                .of(campoId.toString(), "Vela"), AHORA);
    }

    private void dadoUnBloqueDeUnNegocioYOtroNegocio() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueDeA = givenBloque(NegocioId.de(negocioA), "Productos");
    }

    // --- cuando ---
    private void cuandoSeBorraElBloque() {
        bloques.deleteByIdAndNegocioId(bloqueId, negocioId);
    }

    private void cuandoSeBorraElBloqueConElNegocioEquivocado() {
        bloques.deleteByIdAndNegocioId(bloqueDeA, negocioB);
    }

    // --- entonces ---
    private void entoncesSeEliminanElBloqueSusCamposYSusRegistros() {
        assertThat(bloques.findById(bloqueId)).isEmpty();
        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).isEmpty();
    }

    private void entoncesElBloqueAjenoSigueExistiendo() {
        assertThat(bloques.findByIdAndNegocioId(bloqueDeA, negocioA)).isPresent();
    }
}
