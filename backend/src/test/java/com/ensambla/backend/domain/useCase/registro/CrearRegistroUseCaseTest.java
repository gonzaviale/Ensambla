package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.CrearRegistroRequest;
import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.registro.CrearRegistroMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearRegistroUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Map<String, Object> VALORES = Map.of("campo", "dato");

    @Mock
    private NegocioService negocioService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    private CrearRegistroUseCase crearRegistroUseCase;

    @BeforeEach
    void setUp() {
        crearRegistroUseCase = new CrearRegistroUseCase(negocioService, bloqueService, campoService,
                registroDatosService, new CrearRegistroMapper(), Clock.fixed(INSTANTE, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Crea el registro en el bloque del negocio de la sesión y lo devuelve mapeado")
    void executeShouldCreateRegistroInBloqueOfTheSessionNegocio() {
        List<Campo> campos = List.of(Campo.builder().nombre("Producto").build());
        Registro creado = Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(VALORES).build();
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
        when(registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, campos, VALORES, AHORA)).thenReturn(creado);

        CrearRegistroResponse response = crearRegistroUseCase.execute(
                new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, VALORES));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(response.registro().valores()).isEqualTo(VALORES);
    }

    @Test
    @DisplayName("No valida ni guarda cuando el bloque no es del negocio")
    void executeShouldPropagateNotFoundWhenBloqueIsNotOfTheNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> crearRegistroUseCase.execute(new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, VALORES)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(campoService, registroDatosService);
        verify(bloqueService).obtener(BLOQUE_ID, NEGOCIO_ID);
    }
}
