package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRegistroUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private RegistroService registroService;

    private ObtenerRegistroUseCase obtenerRegistroUseCase;

    @BeforeEach
    void setUp() {
        obtenerRegistroUseCase = new ObtenerRegistroUseCase(
                negocioService, registroService, new ObtenerRegistroMapper());
    }

    @Test
    @DisplayName("Devuelve el registro del negocio de la sesión mapeado")
    void executeShouldReturnMappedRegistroOfTheSessionNegocio() {
        when(negocioService.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(
                Registro.builder().id(REGISTRO_ID).valores(Map.of("campo", "dato")).build());

        ObtenerRegistroResponse response = obtenerRegistroUseCase.execute(
                new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(response.registro().valores()).containsEntry("campo", "dato");
    }

    @Test
    @DisplayName("Propaga 'no existe' cuando el registro es de otro negocio")
    void executeShouldPropagateNotFound() {
        when(negocioService.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> obtenerRegistroUseCase.execute(new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
