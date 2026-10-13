package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("Encuentra los campos de otros bloques que se conectan con el bloque, sin contar la autorreferencia")
    void findRelacionesHaciaShouldReturnConexionesFromOtherBloques() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID clientes = givenBloque(negocioId, "Clientes");
        UUID pedidos = givenBloque(negocioId, "Pedidos");
        givenCampo(negocioId, pedidos, "Total", TipoCampo.NUMERO, 0);
        givenRelacion(negocioId, clientes, "Recomendado por", clientes);
        UUID cliente = givenRelacion(negocioId, pedidos, "Cliente", clientes);

        List<CampoEntity> result = campos.findRelacionesHacia(clientes, negocioId);

        assertThat(result).extracting(CampoEntity::getId).containsExactly(cliente);
    }

    @Test
    @DisplayName("No devuelve conexiones de otro negocio")
    void findRelacionesHaciaShouldReturnNothingForOtherNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID clientes = givenBloque(negocioA, "Clientes");
        givenRelacion(negocioA, givenBloque(negocioA, "Pedidos"), "Cliente", clientes);

        List<CampoEntity> result = campos.findRelacionesHacia(clientes, negocioB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guarda y relee la tabla con la que se conecta un campo de conexión")
    void saveShouldRoundTripBloqueRelacionado() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID clientes = givenBloque(negocioId, "Clientes");
        UUID campoId = givenRelacion(negocioId, givenBloque(negocioId, "Pedidos"), "Cliente", clientes);
        entityManager.clear();

        CampoEntity result = campos.findByIdAndNegocioId(campoId, negocioId).orElseThrow();

        assertThat(result.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(result.getBloqueRelacionadoId()).isEqualTo(clientes);
    }

    @Test
    @DisplayName("La base rechaza un campo de conexión sin tabla")
    void saveShouldRejectRelacionWithoutBloque() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID pedidos = givenBloque(negocioId, "Pedidos");

        assertThatThrownBy(() -> givenRelacion(negocioId, pedidos, "Cliente", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una tabla conectada en un campo que no es de conexión")
    void saveShouldRejectBloqueRelacionadoOnOtherTipo() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID clientes = givenBloque(negocioId, "Clientes");
        CampoEntity texto = campo(negocioId, givenBloque(negocioId, "Pedidos"), "Cliente", TipoCampo.TEXTO, clientes);

        assertThatThrownBy(() -> campos.saveAndFlush(texto)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza conectar con una tabla de otro negocio")
    void saveShouldRejectBloqueRelacionadoOfOtherNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID clientesDeB = givenBloque(negocioB, "Clientes");
        UUID pedidosDeA = givenBloque(negocioA, "Pedidos");

        assertThatThrownBy(() -> givenRelacion(negocioA, pedidosDeA, "Cliente", clientesDeB))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borrar la tabla conectada borra en cascada el campo de conexión")
    void deleteBloqueShouldCascadeToRelacion() {
        UUID negocioId = givenNegocio("a@example.com").getId();
        UUID clientes = givenBloque(negocioId, "Clientes");
        UUID campoId = givenRelacion(negocioId, givenBloque(negocioId, "Pedidos"), "Cliente", clientes);

        bloques.deleteByIdAndNegocioId(clientes, negocioId);

        assertThat(campos.findById(campoId)).isEmpty();
    }

    // --- arrange ---
    private UUID givenRelacion(UUID negocioId, UUID bloqueId, String nombre, UUID relacionadoId) {
        return campos.saveAndFlush(campo(negocioId, bloqueId, nombre, TipoCampo.RELACION, relacionadoId)).getId();
    }

    private CampoEntity campo(UUID negocioId, UUID bloqueId, String nombre, TipoCampo tipo, UUID relacionadoId) {
        return CampoEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre(nombre)
                .tipo(tipo)
                .obligatorio(false)
                .opciones(List.of())
                .bloqueRelacionadoId(relacionadoId)
                .posicion((short) 1)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }
}
