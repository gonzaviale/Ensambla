package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
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
import static org.assertj.core.api.Assertions.catchThrowable;

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
    void dadoUnBloqueConUnPanYUnaHarina() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(NegocioId.de(negocioA), "Productos");
        campoPrecio = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Precio", TipoCampo.NUMERO, 0);
        pan = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), AHORA);
        harina = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), AHORA);
    }

    private RecetaEntity releida;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda las líneas como JSON con las cantidades como texto y las devuelve iguales")
    void guardaLasLineasComoJsonConLasCantidadesComoTextoYLasDevuelveIguales() {
        dadoUnaRecetaDelPan();

        cuandoSeReleeLaRecetaDelPan();

        entoncesLasLineasVuelvenIgualesConLasCantidadesComoTexto();
    }

    @Test
    @DisplayName("Un negocio ajeno no ve la receta del producto")
    void unNegocioAjenoNoVeLaRecetaDelProducto() {
        dadoUnaRecetaDelPan();

        entoncesSoloElNegocioDuenioVeLaReceta();
    }

    @Test
    @DisplayName("Un producto tiene una sola receta")
    void unProductoTieneUnaSolaReceta() {
        dadoUnaRecetaDelPan();

        cuandoSeIntentaGuardarOtraRecetaParaElMismoProducto();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("No se guarda la receta de un producto de otro negocio")
    void noSeGuardaLaRecetaDeUnProductoDeOtroNegocio() {
        cuandoSeIntentaGuardarLaRecetaDelPanConElNegocioB();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Borra la receta del producto filtrando por negocio")
    void borraLaRecetaDelProductoFiltrandoPorNegocio() {
        dadoUnaRecetaDelPan();

        cuandoSeBorraLaRecetaConElNegocioEquivocado();
        entoncesLaRecetaSigueExistiendo();

        cuandoSeBorraLaRecetaConSuNegocio();
        entoncesLaRecetaYaNoExiste();
    }

    @Test
    @DisplayName("Al borrar el producto o la columna del precio se borra la receta")
    void alBorrarElProductoOLaColumnaDelPrecioSeBorraLaReceta() {
        dadoUnaRecetaDelPan();

        cuandoSeBorraLaColumnaDelPrecio();
        entoncesLaRecetaYaNoExiste();

        dadoUnaRecetaDelPanConOtraColumnaDePrecio();
        cuandoSeBorraElProductoPan();
        entoncesLaRecetaYaNoExiste();
    }

    // --- dado ---
    private void dadoUnaRecetaDelPan() {
        recetas.saveAndFlush(unaReceta(negocioA, pan, campoPrecio));
    }

    private void dadoUnaRecetaDelPanConOtraColumnaDePrecio() {
        UUID otroPrecio = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Costo", TipoCampo.NUMERO, 1);
        recetas.saveAndFlush(unaReceta(negocioA, pan, otroPrecio));
    }

    private RecetaEntity unaReceta(UUID negocioId, UUID productoId, UUID campoPrecioId) {
        return RecetaEntity.builder()
                .negocioId(negocioId)
                .productoId(productoId)
                .campoPrecioId(campoPrecioId)
                .lineas(List.of(Map.of("insumoId", harina.toString(), "cantidad", "0.125")))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private void cuandoSeReleeLaRecetaDelPan() {
        recetas.flush();
        releida = recetas.findByProductoIdAndNegocioId(pan, negocioA).orElseThrow();
    }

    private void cuandoSeIntentaGuardarOtraRecetaParaElMismoProducto() {
        rechazo = catchThrowable(() -> recetas.saveAndFlush(unaReceta(negocioA, pan, campoPrecio)));
    }

    private void cuandoSeIntentaGuardarLaRecetaDelPanConElNegocioB() {
        rechazo = catchThrowable(() -> recetas.saveAndFlush(unaReceta(negocioB, pan, campoPrecio)));
    }

    private void cuandoSeBorraLaRecetaConElNegocioEquivocado() {
        recetas.deleteByProductoIdAndNegocioId(pan, negocioB);
    }

    private void cuandoSeBorraLaRecetaConSuNegocio() {
        recetas.deleteByProductoIdAndNegocioId(pan, negocioA);
        recetas.flush();
    }

    private void cuandoSeBorraLaColumnaDelPrecio() {
        campos.deleteById(campoPrecio);
        campos.flush();
        recetas.flush();
    }

    private void cuandoSeBorraElProductoPan() {
        registros.deleteById(pan);
        registros.flush();
    }

    // --- entonces ---
    private void entoncesLasLineasVuelvenIgualesConLasCantidadesComoTexto() {
        assertThat(releida.getLineas()).singleElement().satisfies(linea -> {
            assertThat(linea).containsEntry("insumoId", harina.toString());
            assertThat(linea).containsEntry("cantidad", "0.125");
        });
        assertThat(releida.getCampoPrecioId()).isEqualTo(campoPrecio);
    }

    private void entoncesSoloElNegocioDuenioVeLaReceta() {
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isPresent();
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioB)).isEmpty();
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesLaRecetaSigueExistiendo() {
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isPresent();
    }

    private void entoncesLaRecetaYaNoExiste() {
        assertThat(recetas.findByProductoIdAndNegocioId(pan, negocioA)).isEmpty();
    }
}
