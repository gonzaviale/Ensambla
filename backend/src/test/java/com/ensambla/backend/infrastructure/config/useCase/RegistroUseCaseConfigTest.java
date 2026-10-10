package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.dto.registro.CrearRegistroRequest;
import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EditarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroUseCaseConfigTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final RegistroUseCaseConfig config = new RegistroUseCaseConfig();
    private final Clock clock = Clock.systemUTC();

    private NegocioService negocioService;
    private BloqueService bloqueService;
    private CampoService campoService;
    private RegistroService registroService;

    @BeforeEach
    void setUp() {
        negocioService = mock(NegocioService.class);
        bloqueService = mock(BloqueService.class);
        campoService = mock(CampoService.class);
        registroService = mock(RegistroService.class);
        when(negocioService.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El wiring de crear conecta los servicios, el mapper y el reloj")
    void crearRegistroUseCaseShouldWireCollaborators() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(registroService.crear(any(), any(), any(), any(), any())).thenReturn(registro());

        CrearRegistroResponse response = config.crearRegistroUseCase(negocioService, bloqueService, campoService, registroService,
                config.crearRegistroMapper(), clock)
                .execute(new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, Map.of()));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("El wiring de listar conecta los servicios y el mapper")
    void listarRegistrosUseCaseShouldWireCollaborators() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(registroService.listar(any())).thenReturn(new Pagina<>(List.of(registro()), 0, 20, 1));

        ListarRegistrosResponse response = config.listarRegistrosUseCase(negocioService, bloqueService, campoService, registroService,
                config.listarRegistrosMapper())
                .execute(new ListarRegistrosRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));

        assertThat(response.contenido()).hasSize(1);
    }

    @Test
    @DisplayName("El wiring de obtener conecta los servicios y el mapper")
    void obtenerRegistroUseCaseShouldWireCollaborators() {
        when(registroService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(registro());

        ObtenerRegistroResponse response = config.obtenerRegistroUseCase(negocioService, registroService, config.obtenerRegistroMapper())
                .execute(new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("El wiring de editar conecta los servicios, el mapper y el reloj")
    void editarRegistroUseCaseShouldWireCollaborators() {
        Registro actual = registro();
        when(registroService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(actual);
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(registroService.editar(any(), any(), any(), any())).thenReturn(actual);

        EditarRegistroResponse response = config.editarRegistroUseCase(negocioService, campoService, registroService,
                config.editarRegistroMapper(), clock)
                .execute(new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, Map.of()));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("El wiring de eliminar conecta los servicios")
    void eliminarRegistroUseCaseShouldWireCollaborators() {
        Registro actual = registro();
        when(registroService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(actual);

        config.eliminarRegistroUseCase(negocioService, registroService)
                .execute(new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID));

        verify(registroService).eliminar(actual);
    }

    // --- helpers ---
    private Registro registro() {
        return Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of()).build();
    }
}
