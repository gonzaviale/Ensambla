package com.ensambla.backend.domain.repository.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventarioCajaRepositoryTest {

    private final SistemaRepository sistemas = mock(SistemaRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene únicamente el inventario del proyecto del negocio")
    void obtenerShouldReturnOwnedInventory() {
        when(sistemas.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(CajaTestData.sistema()));

        var inventario = sistemas.obtenerInventarioCaja(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);

        assertThat(inventario.getId()).isEqualTo(CajaTestData.INVENTARIO);
    }

    @Test
    @DisplayName("Oculta un proyecto que no pertenece al negocio")
    void obtenerShouldHideForeignProject() {
        when(sistemas.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(CajaTestData.sistema()));

        assertThatThrownBy(() -> sistemas.obtenerInventarioCaja(CajaTestData.NEGOCIO, ProyectoId.nuevo(), CajaTestData.INVENTARIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza una tabla ausente")
    void obtenerShouldRejectUnknownInventory() {
        when(sistemas.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(CajaTestData.sistema()));

        assertThatThrownBy(() -> sistemas.obtenerInventarioCaja(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, BloqueId.nuevo()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Una tabla de otro tipo no puede abrir una caja")
    void obtenerShouldRejectWrongType() {
        var sistema = CajaTestData.sistema();
        sistema.proyecto().getBloques().getFirst().setTipo(TipoBloque.PEDIDOS);
        when(sistemas.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(sistema));

        assertThatThrownBy(() -> sistemas.obtenerInventarioCaja(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
