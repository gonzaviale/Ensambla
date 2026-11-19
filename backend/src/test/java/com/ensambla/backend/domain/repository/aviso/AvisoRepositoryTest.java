package com.ensambla.backend.domain.repository.aviso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvisoRepositoryTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final AvisoRepository avisoRepository = mock(AvisoRepository.class, CALLS_REAL_METHODS);

    private Aviso aviso;
    private Aviso obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve el aviso del negocio")
    void devuelveElAvisoDelNegocio() {
        dadoUnAvisoDelNegocio();

        cuandoSeObtieneElAviso();

        entoncesSeDevuelveElMismoAviso();
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el aviso es de otro negocio o no existe")
    void respondeNoExisteCuandoElAvisoEsDeOtroNegocioONoExiste() {
        dadoQueElAvisoNoExiste();

        cuandoSeIntentaObtenerElAviso();

        entoncesSeRespondeQueElAvisoNoExiste();
    }

    // --- dado ---

    private void dadoUnAvisoDelNegocio() {
        aviso = Aviso.builder()
                .id(AvisoId.nuevo()).negocioId(NEGOCIO_ID).titulo("a").prioridad((short) 2)
                .estado(EstadoAviso.PENDIENTE).creadoEn(AHORA.withHour(1)).acciones(List.of())
                .build();
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.of(aviso));
    }

    private void dadoQueElAvisoNoExiste() {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElAviso() {
        obtenido = avisoRepository.obtener(AVISO_ID, NEGOCIO_ID);
    }

    private void cuandoSeIntentaObtenerElAviso() {
        rechazo = catchThrowable(this::cuandoSeObtieneElAviso);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElMismoAviso() {
        assertThat(obtenido).isSameAs(aviso);
    }

    private void entoncesSeRespondeQueElAvisoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("El aviso no existe");
    }
}
