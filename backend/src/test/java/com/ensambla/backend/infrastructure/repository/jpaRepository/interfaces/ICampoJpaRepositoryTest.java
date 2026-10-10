package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ICampoJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Encuentra el campo dentro de su negocio")
    void findByIdAndNegocioIdShouldReturnCampoOfNegocio() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID campoId = givenCampo(negocioId, givenBloque(negocioId, "Productos"), "Precio", TipoCampo.NUMERO, 0);

        Optional<CampoEntity> result = campos.findByIdAndNegocioId(campoId, negocioId);

        assertThat(result).map(CampoEntity::getNombre).contains("Precio");
    }

    @Test
    @DisplayName("No devuelve un campo de otro negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenCampoBelongsToOtherNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID campoDeA = givenCampo(negocioA, givenBloque(negocioA, "Productos"), "Precio", TipoCampo.NUMERO, 0);

        Optional<CampoEntity> result = campos.findByIdAndNegocioId(campoDeA, negocioB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guarda y relee las opciones, el valor inicial y la obligatoriedad de un campo de opción")
    void saveShouldRoundTripOpcionConfiguration() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID bloqueId = givenBloque(negocioId, "Productos");
        UUID campoId = campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .obligatorio(true)
                .valorInicial("Chico")
                .opciones(List.of("Chico", "Grande"))
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        entityManager.clear();

        CampoEntity result = campos.findByIdAndNegocioId(campoId, negocioId).orElseThrow();

        assertThat(result.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(result.getObligatorio()).isTrue();
        assertThat(result.getValorInicial()).isEqualTo("Chico");
        assertThat(result.getOpciones()).containsExactly("Chico", "Grande");
    }

    @Test
    @DisplayName("Borrar un campo no borra los registros del bloque")
    void deleteByIdAndNegocioIdShouldKeepRegistros() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID bloqueId = givenBloque(negocioId, "Productos");
        UUID campoId = givenCampo(negocioId, bloqueId, "Nombre", TipoCampo.TEXTO, 0);
        UUID registroId = givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Vela"), AHORA);

        campos.deleteByIdAndNegocioId(campoId, negocioId);

        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).map(RegistroEntity::getValores)
                .contains(Map.of(campoId.toString(), "Vela"));
    }

    @Test
    @DisplayName("Borrar con el negocio equivocado no toca el campo ajeno")
    void deleteByIdAndNegocioIdShouldNotDeleteCampoOfOtherNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID campoDeA = givenCampo(negocioA, givenBloque(negocioA, "Productos"), "Precio", TipoCampo.NUMERO, 0);

        campos.deleteByIdAndNegocioId(campoDeA, negocioB);

        assertThat(campos.findByIdAndNegocioId(campoDeA, negocioA)).isPresent();
    }
}
