package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarRegistroTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private RegistroRepository registroRepository;

    private EliminarRegistro eliminarRegistro;

    @BeforeEach
    void setUp() {
        eliminarRegistro = new EliminarRegistro(
                negocioService, registroDatosService, registroRepository, new TransaccionDirecta());
    }

    @Test
    @DisplayName("Elimina el registro del negocio de la sesión")
    void executeShouldDeleteRegistroOfTheSessionNegocio() {
        Registro registro = Registro.builder().id(REGISTRO_ID).build();
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(registro);

        eliminarRegistro.execute(new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID));

        verify(registroRepository).deleteById(REGISTRO_ID);
    }

    @Test
    @DisplayName("No elimina nada cuando el registro es de otro negocio")
    void executeShouldNotDeleteWhenRegistroIsNotOfTheNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> eliminarRegistro.execute(
                new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(registroDatosService).obtener(REGISTRO_ID, NEGOCIO_ID);
        verifyNoMoreInteractions(registroDatosService);
        verifyNoInteractions(registroRepository);
    }
}
