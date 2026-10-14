package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
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
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID HARINA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    @Mock
    private IRecetaJpaRepository jpa;

    private RecetaRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RecetaRepositoryImpl(jpa, new RecetaRepositoryMapper());
    }

    @Test
    @DisplayName("Guarda la receta y devuelve el modelo persistido")
    void saveShouldPersistAndReturnDomain() {
        when(jpa.save(any(RecetaEntity.class))).thenReturn(entity());

        Receta result = repository.save(Receta.builder()
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(HARINA_ID)
                .lineas(List.of(new LineaReceta(HARINA_ID, BigDecimal.ONE, null)))
                .build());

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getLineas()).containsExactly(new LineaReceta(HARINA_ID, new BigDecimal("1.5"), "kg"));
    }

    @Test
    @DisplayName("Busca la receta por producto y negocio")
    void findShouldMapTheEntityOfTheProduct() {
        when(jpa.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(entity()));
        when(jpa.findByProductoIdAndNegocioId(HARINA_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThat(repository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).get()
                .extracting(Receta::getId).isEqualTo(ID);
        assertThat(repository.findByProductoIdAndNegocioId(HARINA_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Borra la receta por producto y negocio")
    void deleteShouldDelegateToJpa() {
        repository.deleteByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);

        verify(jpa).deleteByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);
    }

    private static RecetaEntity entity() {
        return RecetaEntity.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(HARINA_ID)
                .lineas(List.of(Map.of("insumoId", HARINA_ID.toString(), "cantidad", "1.5", "unidad", "kg")))
                .build();
    }
}
