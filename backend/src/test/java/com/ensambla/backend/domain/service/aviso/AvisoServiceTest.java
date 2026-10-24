package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID AVISO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Mock
    private AvisoRepository avisoRepository;

    @InjectMocks
    private AvisoService avisoService;

    @Test
    @DisplayName("Devuelve el aviso del negocio")
    void obtenerShouldReturnAvisoOfNegocio() {
        Aviso aviso = givenAviso("a", 2, EstadoAviso.PENDIENTE, 1);
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.of(aviso));

        assertThat(avisoService.obtener(AVISO_ID, NEGOCIO_ID)).isSameAs(aviso);
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el aviso es de otro negocio o no existe")
    void obtenerShouldThrowWhenAvisoIsNotFound() {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avisoService.obtener(AVISO_ID, NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El aviso no existe");
    }

    private static Aviso givenAviso(String titulo, int prioridad, EstadoAviso estado, int hora) {
        return Aviso.builder()
                .id(UUID.randomUUID()).negocioId(NEGOCIO_ID).titulo(titulo).prioridad((short) prioridad)
                .estado(estado).creadoEn(AHORA.withHour(hora)).acciones(List.of())
                .build();
    }
}
