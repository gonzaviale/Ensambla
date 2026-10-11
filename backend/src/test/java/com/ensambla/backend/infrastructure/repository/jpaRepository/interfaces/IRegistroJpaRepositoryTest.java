package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IRegistroJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final Pageable PRIMERA = PageRequest.of(0, 50);

    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueA;
    private UUID campoNombre;
    private UUID campoCantidad;
    private UUID campoActivo;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(negocioA, "Productos");
        campoNombre = givenCampo(negocioA, bloqueA, "Producto", TipoCampo.TEXTO, 0);
        campoCantidad = givenCampo(negocioA, bloqueA, "Cantidad", TipoCampo.NUMERO, 1);
        campoActivo = givenCampo(negocioA, bloqueA, "Activo", TipoCampo.SI_NO, 2);
    }

    @Test
    @DisplayName("Encuentra el registro por id sólo dentro de su negocio")
    void findByIdAndNegocioIdShouldRespectNegocio() {
        UUID registroId = givenRegistro(negocioA, bloqueA, Map.of(), hace(0));

        Optional<RegistroEntity> propio = registros.findByIdAndNegocioId(registroId, negocioA);
        Optional<RegistroEntity> ajeno = registros.findByIdAndNegocioId(registroId, negocioB);

        assertThat(propio).isPresent();
        assertThat(ajeno).isEmpty();
    }

    @Test
    @DisplayName("Lista los más nuevos primero y sólo los del bloque y negocio pedidos")
    void buscarDescendenteShouldListNewestFirstScopedToBloqueAndNegocio() {
        UUID viejo = givenRegistro(negocioA, bloqueA, Map.of(), hace(2));
        UUID nuevo = givenRegistro(negocioA, bloqueA, Map.of(), hace(1));
        UUID otroBloque = givenBloque(negocioA, "Clientes");
        givenRegistro(negocioA, otroBloque, Map.of(), hace(0));
        UUID bloqueDeB = givenBloque(negocioB, "Productos");
        givenRegistro(negocioB, bloqueDeB, Map.of(), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(nuevo, viejo);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("No devuelve registros cuando el bloque pertenece a otro negocio")
    void buscarShouldReturnNothingWhenBloqueBelongsToOtherNegocio() {
        givenRegistro(negocioA, bloqueA, Map.of(), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioB, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Lista los más viejos primero en orden ascendente por fecha de carga")
    void buscarAscendenteShouldListOldestFirst() {
        UUID viejo = givenRegistro(negocioA, bloqueA, Map.of(), hace(2));
        UUID nuevo = givenRegistro(negocioA, bloqueA, Map.of(), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(negocioA, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(viejo, nuevo);
    }

    @Test
    @DisplayName("Busca el texto entre los valores sin distinguir mayúsculas")
    void buscarShouldMatchValuesIgnoringCase() {
        UUID harina = givenRegistro(negocioA, bloqueA, valores(campoNombre, "Harina 000"), hace(2));
        givenRegistro(negocioA, bloqueA, valores(campoNombre, "Azúcar"), hace(1));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%HARINA%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(harina);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Busca también en valores numéricos")
    void buscarShouldMatchNumericValues() {
        UUID conCantidad = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 4500), hace(1));
        givenRegistro(negocioA, bloqueA, valores(campoCantidad, 12), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%450%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(conCantidad);
    }

    @Test
    @DisplayName("No busca entre los ids de los campos")
    void buscarShouldNotMatchFieldIds() {
        givenRegistro(negocioA, bloqueA, valores(campoNombre, "Harina"), hace(0));
        String fragmentoDelId = campoNombre.toString().substring(0, 8);

        Page<RegistroEntity> result = registros.buscarDescendente(
                negocioA, bloqueA, "%" + fragmentoDelId + "%", null, PRIMERA);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Un patrón con comodines escapados busca el símbolo literal")
    void buscarShouldMatchEscapedWildcardsLiterally() {
        UUID conPorcentaje = givenRegistro(negocioA, bloqueA, valores(campoNombre, "Oferta 50% off"), hace(1));
        givenRegistro(negocioA, bloqueA, valores(campoNombre, "Oferta 50 off"), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%50\\%%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(conPorcentaje);
    }

    @Test
    @DisplayName("Ordena los números por valor y no como texto")
    void buscarAscendenteShouldOrderNumbersNumerically() {
        UUID diez = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 10), hace(3));
        UUID nueve = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 9), hace(2));
        UUID cien = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 100), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(nueve, diez, cien);
    }

    @Test
    @DisplayName("Deja al final a los registros sin el dato, tanto en orden ascendente como descendente")
    void buscarShouldPutRegistrosWithoutValueLast() {
        UUID sinDato = givenRegistro(negocioA, bloqueA, Map.of(), hace(0));
        UUID cinco = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 5), hace(2));
        UUID siete = givenRegistro(negocioA, bloqueA, valores(campoCantidad, 7), hace(1));

        Page<RegistroEntity> ascendente = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);
        Page<RegistroEntity> descendente = registros.buscarDescendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);

        assertThat(ascendente.getContent()).extracting(RegistroEntity::getId).containsExactly(cinco, siete, sinDato);
        assertThat(descendente.getContent()).extracting(RegistroEntity::getId).containsExactly(siete, cinco, sinDato);
    }

    @Test
    @DisplayName("Ordena los textos sin distinguir mayúsculas")
    void buscarAscendenteShouldOrderTextsIgnoringCase() {
        UUID zeta = givenRegistro(negocioA, bloqueA, valores(campoNombre, "Zeta"), hace(3));
        UUID banana = givenRegistro(negocioA, bloqueA, valores(campoNombre, "banana"), hace(2));
        UUID apple = givenRegistro(negocioA, bloqueA, valores(campoNombre, "Apple"), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoNombre.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(apple, banana, zeta);
    }

    @Test
    @DisplayName("Ordena los sí/no poniendo primero el no")
    void buscarAscendenteShouldOrderBooleansFalseFirst() {
        UUID si = givenRegistro(negocioA, bloqueA, valores(campoActivo, true), hace(2));
        UUID no = givenRegistro(negocioA, bloqueA, valores(campoActivo, false), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoActivo.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(no, si);
    }

    @Test
    @DisplayName("Pagina los resultados e informa el total sin paginar")
    void buscarShouldPaginateAndCountTotal() {
        for (int i = 0; i < 5; i++) {
            givenRegistro(negocioA, bloqueA, valores(campoCantidad, i), hace(10 - i));
        }

        Page<RegistroEntity> ultima = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PageRequest.of(2, 2));

        assertThat(ultima.getTotalElements()).isEqualTo(5);
        assertThat(ultima.getTotalPages()).isEqualTo(3);
        assertThat(ultima.getContent()).hasSize(1);
        assertThat(ultima.getContent().getFirst().getValores()).containsEntry(campoCantidad.toString(), 4);
    }

    // --- helpers ---
    private OffsetDateTime hace(int minutos) {
        return AHORA.minusMinutes(minutos);
    }

    private Map<String, Object> valores(UUID campoId, Object valor) {
        Map<String, Object> valores = new HashMap<>();
        valores.put(campoId.toString(), valor);
        return valores;
    }
}
