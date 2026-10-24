package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID AVISO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private AccionAvisoService accionAvisoService;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private AvisoService avisoService;

    @Test
    @DisplayName("Lista primero los pendientes, del más urgente al menos urgente y del más nuevo al más viejo")
    void listarShouldPutPendingFirstThenByPrioridadAndDate() {
        Aviso descartado = givenAviso("descartado", 3, EstadoAviso.DESCARTADO, 5);
        Aviso pocoUrgente = givenAviso("poco urgente", 1, EstadoAviso.PENDIENTE, 5);
        Aviso viejoUrgente = givenAviso("viejo urgente", 3, EstadoAviso.PENDIENTE, 1);
        Aviso nuevoUrgente = givenAviso("nuevo urgente", 3, EstadoAviso.PENDIENTE, 4);
        when(avisoRepository.findByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(descartado, pocoUrgente, viejoUrgente, nuevoUrgente));

        List<Aviso> result = avisoService.listar(NEGOCIO_ID, null);

        assertThat(result).extracting(Aviso::getTitulo)
                .containsExactly("nuevo urgente", "viejo urgente", "poco urgente", "descartado");
    }

    @Test
    @DisplayName("Con estado lista sólo los avisos de ese estado")
    void listarShouldFilterByEstado() {
        Aviso pendiente = givenAviso("pendiente", 2, EstadoAviso.PENDIENTE, 1);
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));

        assertThat(avisoService.listar(NEGOCIO_ID, EstadoAviso.PENDIENTE)).containsExactly(pendiente);
        verify(avisoRepository, never()).findByNegocioId(any());
    }

    @Test
    @DisplayName("Cuenta los pendientes y cuántos son urgentes")
    void contarShouldCountPendingAndUrgent() {
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(
                givenAviso("a", 3, EstadoAviso.PENDIENTE, 1),
                givenAviso("b", 2, EstadoAviso.PENDIENTE, 1),
                givenAviso("c", 1, EstadoAviso.PENDIENTE, 1)));

        assertThat(avisoService.contar(NEGOCIO_ID)).isEqualTo(new ContadorAvisos(3, 2));
    }

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

    @Test
    @DisplayName("Aplicar ejecuta la acción sugerida y guarda el aviso como aplicado")
    void aplicarShouldRunActionAndSaveAppliedAviso() {
        Aviso aviso = givenAviso("a", 2, EstadoAviso.PENDIENTE, 1);
        when(avisoRepository.save(aviso)).thenReturn(aviso);

        Aviso result = avisoService.aplicar(aviso, AHORA);

        assertThat(result.getEstado()).isEqualTo(EstadoAviso.APLICADO);
        InOrder orden = inOrder(accionAvisoService, avisoRepository);
        orden.verify(accionAvisoService).ejecutar(aviso, AHORA);
        orden.verify(avisoRepository).save(aviso);
    }

    @Test
    @DisplayName("Aplicar un aviso que ya fue resuelto no ejecuta nada")
    void aplicarShouldThrowWithoutRunningActionWhenAvisoWasResolved() {
        Aviso aviso = givenAviso("a", 2, EstadoAviso.DESCARTADO, 1);

        assertThatThrownBy(() -> avisoService.aplicar(aviso, AHORA)).isInstanceOf(BusinessRuleException.class);
        verify(accionAvisoService, never()).ejecutar(any(), any());
        verify(avisoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Descartar guarda el aviso como descartado")
    void descartarShouldSaveDiscardedAviso() {
        Aviso aviso = givenAviso("a", 2, EstadoAviso.PENDIENTE, 1);
        when(avisoRepository.save(aviso)).thenReturn(aviso);

        Aviso result = avisoService.descartar(aviso, AHORA);

        assertThat(result.getEstado()).isEqualTo(EstadoAviso.DESCARTADO);
        assertThat(result.getResueltoEn()).isEqualTo(AHORA);
    }

    private static Aviso givenAviso(String titulo, int prioridad, EstadoAviso estado, int hora) {
        return Aviso.builder()
                .id(UUID.randomUUID()).negocioId(NEGOCIO_ID).titulo(titulo).prioridad((short) prioridad)
                .estado(estado).creadoEn(AHORA.withHour(hora)).acciones(List.of())
                .build();
    }
}
