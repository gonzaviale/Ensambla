package com.ensambla.backend.domain.repository.registro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistroRepositoryTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    private final RegistroRepository registroRepository = mock(RegistroRepository.class, CALLS_REAL_METHODS);

    private Registro registro;
    private Registro obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve el registro del negocio")
    void devuelveElRegistroDelNegocio() {
        dadoUnRegistroDelNegocio();

        cuandoSeObtieneElRegistro();

        entoncesSeDevuelveElMismoRegistro();
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el registro es de otro negocio o no existe")
    void respondeNoExisteCuandoElRegistroEsDeOtroNegocioONoExiste() {
        dadoQueElRegistroNoExiste();

        cuandoSeIntentaObtenerElRegistro();

        entoncesSeRespondeQueElRegistroNoExiste();
    }

    // --- dado ---

    private void dadoUnRegistroDelNegocio() {
        registro = Registro.builder().id(REGISTRO_ID).negocioId(NEGOCIO_ID).build();
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.of(registro));
    }

    private void dadoQueElRegistroNoExiste() {
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElRegistro() {
        obtenido = registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID);
    }

    private void cuandoSeIntentaObtenerElRegistro() {
        rechazo = catchThrowable(this::cuandoSeObtieneElRegistro);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElMismoRegistro() {
        assertThat(obtenido).isSameAs(registro);
    }

    private void entoncesSeRespondeQueElRegistroNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("El registro no existe");
    }
}
