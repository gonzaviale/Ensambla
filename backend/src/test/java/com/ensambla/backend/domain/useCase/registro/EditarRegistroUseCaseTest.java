package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.EditarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.registro.EditarRegistroMapper;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditarRegistroUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Map<String, Object> CAMBIOS = Map.of("campo", "nuevo");

    @Mock
    private NegocioService negocioService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    private EditarRegistroUseCase editarRegistroUseCase;

    @BeforeEach
    void setUp() {
        editarRegistroUseCase = new EditarRegistroUseCase(negocioService, campoService, registroDatosService,
                new EditarRegistroMapper(), Clock.fixed(INSTANTE, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Edita el registro con los campos de su bloque y devuelve el resultado mapeado")
    void executeShouldEditRegistroWithCamposOfItsBloque() {
        Registro actual = Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).build();
        Registro editado = Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(CAMBIOS).build();
        List<Campo> campos = List.of(Campo.builder().nombre("Producto").build());
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(actual);
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
        when(registroDatosService.editar(actual, campos, CAMBIOS, AHORA)).thenReturn(editado);

        EditarRegistroResponse response = editarRegistroUseCase.execute(
                new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, CAMBIOS));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(response.registro().valores()).isEqualTo(CAMBIOS);
    }

    @Test
    @DisplayName("No edita nada cuando el registro es de otro negocio")
    void executeShouldPropagateNotFoundWhenRegistroIsNotOfTheNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> editarRegistroUseCase.execute(
                new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, CAMBIOS)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(campoService);
    }
}
