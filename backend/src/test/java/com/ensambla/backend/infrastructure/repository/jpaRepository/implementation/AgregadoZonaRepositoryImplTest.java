package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.AgregadoZonaProductoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAgregadoZonaProductoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AgregadoZonaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgregadoZonaRepositoryImplTest {

    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");

    private final IAgregadoZonaProductoJpaRepository agregadoJpa = mock(IAgregadoZonaProductoJpaRepository.class);
    private final INegocioJpaRepository negocioJpa = mock(INegocioJpaRepository.class);
    private final AgregadoZonaRepositoryImpl repository =
            new AgregadoZonaRepositoryImpl(agregadoJpa, negocioJpa, new AgregadoZonaRepositoryMapper());

    @Test
    @DisplayName("Lista los productos agregados de la zona mapeados al dominio")
    void findByZonaIdShouldMapEveryField() {
        when(agregadoJpa.findAllByZonaIdOrderByNombreAscIdAsc(ZONA_ID.valor())).thenReturn(List.of(
                AgregadoZonaProductoEntity.builder().nombre("Yerba 1kg").comercios(8).comerciosVenden(6)
                        .precioMinimo(new BigDecimal("2800.00")).precioPromedio(new BigDecimal("3100.00"))
                        .precioMaximo(new BigDecimal("3400.00")).unidadesSemana(new BigDecimal("12.50")).build()));

        List<AgregadoZonaProducto> result = repository.findByZonaId(ZONA_ID);

        assertThat(result).singleElement().satisfies(producto -> {
            assertThat(producto.nombre()).isEqualTo("Yerba 1kg");
            assertThat(producto.comercios()).isEqualTo(8);
            assertThat(producto.comerciosVenden()).isEqualTo(6);
            assertThat(producto.precioMinimo()).isEqualByComparingTo("2800");
            assertThat(producto.precioPromedio()).isEqualByComparingTo("3100");
            assertThat(producto.precioMaximo()).isEqualByComparingTo("3400");
            assertThat(producto.unidadesSemana()).isEqualByComparingTo("12.5");
        });
    }

    @Test
    @DisplayName("Cuenta solo los comercios de la zona que comparten sus datos")
    void contarComerciosQueComparten() {
        when(negocioJpa.countByZonaIdAndComparteDatosZonaTrue(ZONA_ID.valor())).thenReturn(10L);

        assertThat(repository.contarComerciosQueComparten(ZONA_ID)).isEqualTo(10L);
    }
}
