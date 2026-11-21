package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.eliminarRegistro.EliminarRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferenciasRequest;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ReferenciaItem;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosRequest;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosResponse;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    private final RegistroUseCaseConfig config = new RegistroUseCaseConfig();
    private final Clock clock = Clock.systemUTC();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final RegistroDatosService registroDatosService = mock(RegistroDatosService.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private RegistroId registroObtenido;
    private ListarRegistrosResponse listado;
    private List<ReferenciaItem> referencias;

    @BeforeEach
    void dadoUnNegocioConUnBloqueSinCampos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("El wiring de crear conecta los servicios, el mapper y el reloj")
    void elWiringDeCrearConectaLosServiciosYElReloj() {
        dadoQueGuardarDevuelveElRegistro();

        cuandoSeCreaUnRegistro();

        entoncesSeObtuvoElRegistro();
    }

    @Test
    @DisplayName("El wiring de listar conecta los servicios y el mapper")
    void elWiringDeListarConectaLosServicios() {
        dadaUnaPaginaConUnRegistro();

        cuandoSeListanLosRegistros();

        entoncesSeListoUnRegistro();
    }

    @Test
    @DisplayName("El wiring de referencias conecta los servicios y el mapper")
    void elWiringDeReferenciasConectaLosServicios() {
        dadoUnRegistroReferenciable();

        cuandoSeListanLasReferencias();

        entoncesLaUnicaReferenciaEsSinNombre();
    }

    @Test
    @DisplayName("El wiring de obtener conecta los servicios y el mapper")
    void elWiringDeObtenerConectaLosServicios() {
        dadoUnRegistroGuardado();

        cuandoSeObtieneElRegistro();

        entoncesSeObtuvoElRegistro();
    }

    @Test
    @DisplayName("El wiring de editar conecta los servicios, el mapper y el reloj")
    void elWiringDeEditarConectaLosServiciosYElReloj() {
        dadoUnRegistroEditable();

        cuandoSeEditaElRegistro();

        entoncesSeObtuvoElRegistro();
    }

    @Test
    @DisplayName("El wiring de eliminar conecta los servicios")
    void elWiringDeEliminarConectaLosServicios() {
        dadoUnRegistroGuardado();

        cuandoSeEliminaElRegistro();

        entoncesSeBorroElRegistro();
    }

    private void dadoUnBloque() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
    }

    private void dadoQueGuardarDevuelveElRegistro() {
        dadoUnBloque();
        when(registroRepository.save(any())).thenReturn(unRegistro());
    }

    private void dadaUnaPaginaConUnRegistro() {
        dadoUnBloque();
        when(registroRepository.buscar(any(ConsultaRegistros.class))).thenReturn(new Pagina<>(List.of(unRegistro()), 0, 20, 1));
        when(registroRepository.sumarNumericos(any(ConsultaRegistros.class), any())).thenReturn(Map.of());
    }

    private void dadoUnRegistroReferenciable() {
        dadoUnBloque();
        when(registroDatosService.buscarReferenciables(NEGOCIO_ID, BLOQUE_ID, List.of(), null, List.of()))
                .thenReturn(List.of(unRegistro()));
    }

    private void dadoUnRegistroGuardado() {
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(unRegistro());
    }

    private void dadoUnRegistroEditable() {
        dadoUnRegistroGuardado();
        when(registroDatosService.editar(any(), any(), any(), any())).thenReturn(unRegistro());
    }

    private void cuandoSeCreaUnRegistro() {
        registroObtenido = config.crearRegistro(negocioRepository, bloqueRepository, campoRepository, registroDatosService,
                        mock(ControlPlanService.class), registroRepository, new TransaccionDirecta(), clock)
                .execute(new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, Map.of())).registro().id();
    }

    private void cuandoSeListanLosRegistros() {
        listado = config.listarRegistros(negocioRepository, bloqueRepository, campoRepository, registroRepository)
                .execute(new ListarRegistrosRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));
    }

    private void cuandoSeListanLasReferencias() {
        referencias = config.listarReferencias(negocioRepository, bloqueRepository, campoRepository, registroDatosService)
                .execute(new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, null, List.of()));
    }

    private void cuandoSeObtieneElRegistro() {
        registroObtenido = config.obtenerRegistro(negocioRepository, campoRepository, registroRepository)
                .execute(new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID)).registro().id();
    }

    private void cuandoSeEditaElRegistro() {
        registroObtenido = config.editarRegistro(negocioRepository, campoRepository, registroDatosService, registroRepository,
                clock).execute(new EditarRegistroRequest(USUARIO_ID, REGISTRO_ID, Map.of())).registro().id();
    }

    private void cuandoSeEliminaElRegistro() {
        config.eliminarRegistro(negocioRepository, registroRepository, new TransaccionDirecta())
                .execute(new EliminarRegistroRequest(USUARIO_ID, REGISTRO_ID));
    }

    private void entoncesSeObtuvoElRegistro() {
        assertThat(registroObtenido).isEqualTo(REGISTRO_ID);
    }

    private void entoncesSeListoUnRegistro() {
        assertThat(listado.contenido()).hasSize(1);
        assertThat(listado.totales().cantidad()).isEqualTo(1);
    }

    private void entoncesLaUnicaReferenciaEsSinNombre() {
        assertThat(referencias).containsExactly(new ReferenciaItem(REGISTRO_ID, "Sin nombre"));
    }

    private void entoncesSeBorroElRegistro() {
        verify(registroRepository).deleteById(REGISTRO_ID);
    }

    private static Registro unRegistro() {
        return Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of()).build();
    }
}