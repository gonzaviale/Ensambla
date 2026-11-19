package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRecetaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RecetaRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaRepositoryImplTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    @Mock
    private IRecetaJpaRepository jpa;

    private RecetaRepositoryImpl repository;

    @BeforeEach
    void dadoUnRepositorioDeRecetas() {
        repository = new RecetaRepositoryImpl(jpa, new RecetaRepositoryMapper());
    }

    private Receta guardada;
    private Optional<Receta> delProducto;
    private Optional<Receta> deOtroProducto;

    @Test
    @DisplayName("Guarda la receta y devuelve el modelo persistido")
    void guardaLaRecetaYDevuelveElModeloPersistido() {
        dadoQueGuardarDevuelveLaReceta();

        cuandoSeGuardaUnaRecetaNueva();

        entoncesSeDevuelveLaRecetaPersistidaConSuLinea();
    }

    @Test
    @DisplayName("Busca la receta por producto y negocio")
    void buscaLaRecetaPorProductoYNegocio() {
        dadoQueSoloElProductoTieneReceta();

        cuandoSeBuscaLaRecetaDelProductoYDeOtro();

        entoncesSoloSeEncuentraLaRecetaDelProducto();
    }

    @Test
    @DisplayName("Borra la receta por producto y negocio")
    void borraLaRecetaPorProductoYNegocio() {
        cuandoSeBorraLaRecetaDelProducto();

        entoncesSeDelegoElBorradoAlJpa();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveLaReceta() {
        when(jpa.save(any(RecetaEntity.class))).thenReturn(entity());
    }

    private void dadoQueSoloElProductoTieneReceta() {
        when(jpa.findByProductoIdAndNegocioId(PRODUCTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(entity()));
        when(jpa.findByProductoIdAndNegocioId(HARINA_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private static RecetaEntity entity() {
        return RecetaEntity.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID.valor())
                .productoId(PRODUCTO_ID.valor())
                .campoPrecioId(HARINA_ID.valor())
                .lineas(List.of(Map.of("insumoId", HARINA_ID.toString(), "cantidad", "1.5", "unidad", "kg")))
                .build();
    }

    // --- cuando ---
    private void cuandoSeGuardaUnaRecetaNueva() {
        guardada = repository.save(Receta.builder()
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(CampoId.de(HARINA_ID.valor()))
                .lineas(List.of(new LineaReceta(HARINA_ID, BigDecimal.ONE, null)))
                .build());
    }

    private void cuandoSeBuscaLaRecetaDelProductoYDeOtro() {
        delProducto = repository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);
        deOtroProducto = repository.findByProductoIdAndNegocioId(HARINA_ID, NEGOCIO_ID);
    }

    private void cuandoSeBorraLaRecetaDelProducto() {
        repository.deleteByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeDevuelveLaRecetaPersistidaConSuLinea() {
        assertThat(guardada.getId()).isEqualTo(RecetaId.de(ID));
        assertThat(guardada.getLineas()).containsExactly(new LineaReceta(HARINA_ID, new BigDecimal("1.5"), "kg"));
    }

    private void entoncesSoloSeEncuentraLaRecetaDelProducto() {
        assertThat(delProducto).get().extracting(Receta::getId).isEqualTo(RecetaId.de(ID));
        assertThat(deOtroProducto).isEmpty();
    }

    private void entoncesSeDelegoElBorradoAlJpa() {
        verify(jpa).deleteByProductoIdAndNegocioId(PRODUCTO_ID.valor(), NEGOCIO_ID.valor());
    }
}
