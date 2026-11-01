package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditarRegistroTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Map<String, Object> CAMBIOS = Map.of("campo", "nuevo");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private RegistroRepository registroRepository;

    private EditarRegistro editarRegistro;

    @BeforeEach
    void setUp() {
        editarRegistro = new EditarRegistro(
                negocioRepository, campoRepository, registroDatosService, registroRepository, Clock
                        .fixed(INSTANTE, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Edita el registro con los campos de su bloque y devuelve el resultado mapeado")
    void executeShouldEditRegistroWithCamposOfItsBloque() {
        Registro actual = Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).build();
        Registro editado = Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(CAMBIOS).build();
        List<Campo> campos = List.of(Campo.builder().nombre("Producto").build());
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(actual);
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
        when(registroDatosService.editar(actual, campos, CAMBIOS, AHORA)).thenReturn(editado);

        EditarRegistroResponse response = editarRegistro.execute(
                new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, CAMBIOS));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(response.registro().valores()).isEqualTo(CAMBIOS);
    }

    @Test
    @DisplayName("No edita nada cuando el registro es de otro negocio")
    void executeShouldPropagateNotFoundWhenRegistroIsNotOfTheNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> editarRegistro.execute(
                new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, CAMBIOS)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(campoRepository);
    }
}
