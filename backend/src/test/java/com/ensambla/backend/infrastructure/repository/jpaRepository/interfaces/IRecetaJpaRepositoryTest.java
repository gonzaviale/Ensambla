package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IRecetaJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private IRecetaJpaRepository recetas;

    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueA;
    private UUID campoPrecio;
    private UUID pan;
    private UUID harina;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(negocioA, "Productos");
        campoPrecio = givenCampo(negocioA, bloqueA, "Precio", TipoCampo.NUMERO, 0);
        pan = givenRegistro(negocioA, bloqueA, Map.of(), AHORA);
        harina = givenRegistro(negocioA, bloqueA, Map.of(), AHORA);
    }

    @Test
    @DisplayName("Guarda las líneas como JSON con las cantidades como texto y las devuelve iguales")
    void saveShouldRoundTripTheLines() {
        recetas.saveAndFlush(receta(negocioA, pan, campoPrecio));
        recetas.flush();

        RecetaEntity result = recetas.findByProductoIdAndNegocioId(pan, negocioA).orElseThrow();

        assertThat(result.getLineas()).singleElement().satisfies(linea -> {
            assertThat(linea).containsEntry("insumoId", harina.toString());
            assertThat(linea).containsEntry("cantidad", "0.125");
        });
        assertThat(result.getCampoPrecioId()).isEqualTo(campoPrecio);
    }

    @Test
    @DisplayName("Un negocio ajeno no ve la receta del producto")
    void findShouldRespectNegocio() {
        recetas.saveAndFlush(receta(negocioA, pan, campoPrecio));

        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isPresent();
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioB)).isEmpty();
    }

    @Test
    @DisplayName("Un producto tiene una sola receta")
    void shouldRejectASecondRecetaForTheSameProduct() {
        recetas.saveAndFlush(receta(negocioA, pan, campoPrecio));

        assertThatThrownBy(() -> recetas.saveAndFlush(receta(negocioA, pan, campoPrecio)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("No se guarda la receta de un producto de otro negocio")
    void shouldRejectProductOfAnotherNegocio() {
        assertThatThrownBy(() -> recetas.saveAndFlush(receta(negocioB, pan, campoPrecio)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borra la receta del producto filtrando por negocio")
    void deleteShouldRespectNegocio() {
        recetas.saveAndFlush(receta(negocioA, pan, campoPrecio));

        recetas.deleteByProductoIdAndNegocioId(pan, negocioB);
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isPresent();

        recetas.deleteByProductoIdAndNegocioId(pan, negocioA);
        recetas.flush();
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isEmpty();
    }

    @Test
    @DisplayName("Al borrar el producto o la columna del precio se borra la receta")
    void deletingTheProductOrThePriceColumnShouldDeleteTheReceta() {
        recetas.saveAndFlush(receta(negocioA, pan, campoPrecio));

        campos.deleteById(campoPrecio);
        campos.flush();
        recetas.flush();
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isEmpty();

        UUID otroPrecio = givenCampo(negocioA, bloqueA, "Costo", TipoCampo.NUMERO, 1);
        recetas.saveAndFlush(receta(negocioA, pan, otroPrecio));
        registros.deleteById(pan);
        registros.flush();
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isEmpty();
    }

    private RecetaEntity receta(UUID negocioId, UUID productoId, UUID campoPrecioId) {
        return RecetaEntity.builder()
                .negocioId(negocioId)
                .productoId(productoId)
                .campoPrecioId(campoPrecioId)
                .lineas(List.of(Map.of("insumoId", harina.toString(), "cantidad", "0.125")))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }
}
