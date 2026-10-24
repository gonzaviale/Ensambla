package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcesamientoServiceTest {

    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class);
    private final ProcesamientoService procesamientoService = new ProcesamientoService(procesamientoRepository);

    @Test
    @DisplayName("Devuelve el estado del lote del negocio")
    void obtenerShouldReturnOwnLote() {
        EstadoLectura estado = new EstadoLectura(LOTE_ID, EstadoLote.ERROR, "Error", 3, null);
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(estado));

        EstadoLectura resultado = procesamientoService.obtener(NEGOCIO_ID, LOTE_ID);

        assertThat(resultado).isEqualTo(estado);
    }

    @Test
    @DisplayName("Un lote ajeno o inexistente responde como no encontrado")
    void obtenerShouldRejectMissingLote() {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> procesamientoService.obtener(NEGOCIO_ID, LOTE_ID))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Lote no encontrado");
    }

    @Test
    @DisplayName("Consulta el consumo del día del negocio")
    void consumoShouldUseCurrentDate() {
        ConsumoDiario esperado = new ConsumoDiario(AHORA.toLocalDate(), 5, 2, 0, 0, null, false);
        when(procesamientoRepository.consumo(NEGOCIO_ID, AHORA.toLocalDate())).thenReturn(esperado);

        ConsumoDiario resultado = procesamientoService.consumo(NEGOCIO_ID, AHORA);

        assertThat(resultado).isEqualTo(esperado);
    }
}
