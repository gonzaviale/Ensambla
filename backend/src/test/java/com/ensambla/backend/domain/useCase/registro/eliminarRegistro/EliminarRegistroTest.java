package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarRegistroTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private RegistroRepository registroRepository;

    private EliminarRegistro eliminarRegistro;

    @BeforeEach
    void setUp() {
        eliminarRegistro = new EliminarRegistro(
                negocioRepository, registroRepository, new TransaccionDirecta());
    }

    @Test
    @DisplayName("Elimina el registro del negocio de la sesión")
    void executeShouldDeleteRegistroOfTheSessionNegocio() {
        Registro registro = Registro.builder().id(REGISTRO_ID).build();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(registro);

        eliminarRegistro.execute(new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID));

        verify(registroRepository).deleteById(REGISTRO_ID);
    }

    @Test
    @DisplayName("No elimina nada cuando el registro es de otro negocio")
    void executeShouldNotDeleteWhenRegistroIsNotOfTheNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> eliminarRegistro.execute(
                new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(registroRepository, never()).deleteById(any());
    }
}
