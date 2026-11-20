package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.infrastructure.config.useCase.SistemaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SistemaCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final ProyectoId SISTEMA_ID = ProyectoId.de("d0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(CLOCK);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final SistemaUseCaseConfig config = new SistemaUseCaseConfig();
    private final ListarSistemas listarSistemas = config.listarSistemas(negocioRepository, sistemaRepository,
            new TransaccionDirecta(), CLOCK);
    private final GuardarSistema guardarSistema = config.guardarSistema(negocioRepository, sistemaRepository,
            usoPlanRepository, controlPlanService, new TransaccionDirecta(), CLOCK);
    private final EliminarSistema eliminarSistema =
            config.eliminarSistema(negocioRepository, proyectoRepository, new TransaccionDirecta());

    private List<SistemaItem> sistemasListados;
    private SistemaItem sistemaGuardado;
    private Throwable rechazo;

    @Test
    @DisplayName("Listar resuelve el negocio de la sesión y mapea los sistemas")
    void listarResuelveElNegocioDeLaSesionYMapeaLosSistemas() {
        dadoUnSistemaEnElNegocioDeLaSesion();

        cuandoSeListanLosSistemas();

        entoncesSeListaElSistemaMiAlmacen();
    }

    @Test
    @DisplayName("Listar no consulta sistemas si el usuario no tiene negocio")
    void listarNoConsultaSistemasSiElUsuarioNoTieneNegocio() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentanListarLosSistemas();

        entoncesSeRechazaPorqueNoExiste();
        entoncesNoSeConsultaningunSistema();
    }

    @Test
    @DisplayName("Guardar crea el sistema con el negocio de la sesión y la hora del reloj cuando el proyecto no existe")
    void guardarCreaElSistemaConElNegocioDeLaSesionYLaHoraDelRelojCuandoElProyectoNoExiste() {
        dadoUnProyectoQueTodaviaNoExiste();

        cuandoSeGuardaElSistema();

        entoncesSeGuardaConElNegocioDeLaSesionYLaHoraDelReloj();
        entoncesSeBloqueaElNegocioYSeValidaElAltaDelProyecto();
    }

    @Test
    @DisplayName("Guardar reemplaza el sistema sin validar el alta cuando el proyecto ya es del negocio")
    void guardarReemplazaElSistemaSinValidarElAltaCuandoElProyectoYaEsDelNegocio() {
        dadoUnProyectoQueYaEsDelNegocio();

        cuandoSeGuardaElSistema();

        entoncesSeReemplazaElSistema();
        entoncesNoSeValidaElAltaDelProyecto();
    }

    @Test
    @DisplayName("Guardar responde como si no existiera cuando el proyecto es de otro negocio")
    void guardarRespondeComoSiNoExistieraCuandoElProyectoEsDeOtroNegocio() {
        dadoUnProyectoDeOtroNegocio();

        cuandoSeIntentaGuardarElSistema();

        entoncesSeRechazaConElMensajeSistemaNoEncontrado();
        entoncesNoSeGuardaNingunSistema();
    }

    @Test
    @DisplayName("Guardar valida sólo el incremento de registros del sistema completo")
    void guardarValidaSoloElIncrementoDeRegistrosDelSistemaCompleto() {
        dadoUnProyectoDelNegocioConRegistros(1L);

        cuandoSeGuardaUnSistemaConFilas(2);

        entoncesSeValidaElAltaDeUnRegistro();
    }

    @Test
    @DisplayName("Guardar no valida registros cuando el reemplazo no suma filas")
    void guardarNoValidaRegistrosCuandoElReemplazoNoSumaFilas() {
        dadoUnProyectoDelNegocioConRegistros(2L);

        cuandoSeGuardaUnSistemaConFilas(2);

        entoncesNoSeValidaElAltaDeRegistros();
    }

    @Test
    @DisplayName("Eliminar traduce el id de la pantalla y borra en el negocio de la sesión sin comprobar que exista")
    void eliminarTraduceElIdDeLaPantallaYBorraEnElNegocioDeLaSesionSinComprobarQueExista() {
        dadoUnNegocioDeLaSesion();

        cuandoSeEliminaElSistemaDeLaPantalla();

        entoncesSeBorraElProyectoEnElNegocioDeLaSesion();
    }

    // --- dado ---

    private void dadoUnNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void dadoUnSistemaEnElNegocioDeLaSesion() {
        dadoUnNegocioDeLaSesion();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(unSistema()));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    private void dadoUnProyectoQueTodaviaNoExiste() {
        dadoUnNegocioDeLaSesion();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnProyectoQueYaEsDelNegocio() {
        dadoUnNegocioDeLaSesion();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(NEGOCIO_ID));
    }

    private void dadoUnProyectoDeOtroNegocio() {
        dadoUnNegocioDeLaSesion();
        when(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).thenReturn(Optional.of(OTRO_NEGOCIO_ID));
    }

    private void dadoUnProyectoDelNegocioConRegistros(long registros) {
        dadoUnProyectoQueYaEsDelNegocio();
        when(usoPlanRepository.contarRegistrosProyecto(NEGOCIO_ID, SISTEMA_ID)).thenReturn(registros);
    }

    // --- cuando ---

    private void cuandoSeListanLosSistemas() {
        sistemasListados = listarSistemas.execute(new ListarSistemasRequest(USUARIO_ID));
    }

    private void cuandoSeIntentanListarLosSistemas() {
        rechazo = catchThrowable(() -> listarSistemas.execute(new ListarSistemasRequest(USUARIO_ID)));
    }

    private void cuandoSeGuardaElSistema() {
        sistemaGuardado = guardarSistema.execute(unaSolicitudDeGuardado());
    }

    private void cuandoSeIntentaGuardarElSistema() {
        rechazo = catchThrowable(() -> guardarSistema.execute(unaSolicitudDeGuardado()));
    }

    private void cuandoSeGuardaUnSistemaConFilas(int filas) {
        sistemaGuardado = guardarSistema.execute(unaSolicitudDeGuardadoConFilas(filas));
    }

    private void cuandoSeEliminaElSistemaDeLaPantalla() {
        eliminarSistema.execute(new EliminarSistemaRequest(USUARIO_ID, "e" + SISTEMA_ID));
    }

    // --- entonces ---

    private void entoncesSeListaElSistemaMiAlmacen() {
        assertThat(sistemasListados).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(SISTEMA_ID.toString());
            assertThat(item.name()).isEqualTo("Mi almacén");
        });
    }

    private void entoncesSeRechazaPorqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeConsultaningunSistema() {
        verifyNoInteractions(sistemaRepository);
    }

    private void entoncesSeGuardaConElNegocioDeLaSesionYLaHoraDelReloj() {
        Sistema guardado = elSistemaGuardado();
        assertThat(guardado.id()).isEqualTo(SISTEMA_ID);
        assertThat(guardado.proyecto().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.proyecto().getActualizadoEn()).isEqualTo(AHORA);
        assertThat(sistemaGuardado.id()).isEqualTo(SISTEMA_ID.toString());
    }

    private void entoncesSeBloqueaElNegocioYSeValidaElAltaDelProyecto() {
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        verify(controlPlanService).validarAltaProyecto(NEGOCIO_ID, AHORA);
    }

    private void entoncesSeReemplazaElSistema() {
        elSistemaGuardado();
    }

    private void entoncesNoSeValidaElAltaDelProyecto() {
        verify(controlPlanService, never()).validarAltaProyecto(any(), any());
    }

    private void entoncesSeRechazaConElMensajeSistemaNoEncontrado() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Sistema no encontrado");
    }

    private void entoncesNoSeGuardaNingunSistema() {
        verify(sistemaRepository, never()).guardar(any(), any());
    }

    private void entoncesSeValidaElAltaDeUnRegistro() {
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
    }

    private void entoncesNoSeValidaElAltaDeRegistros() {
        verify(controlPlanService, never()).validarAltaRegistros(any(), anyLong(), any());
    }

    private void entoncesSeBorraElProyectoEnElNegocioDeLaSesion() {
        verify(proyectoRepository).deleteByIdAndNegocioId(SISTEMA_ID, NEGOCIO_ID);
    }

    // --- datos ---

    private Sistema elSistemaGuardado() {
        ArgumentCaptor<Sistema> guardado = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaRepository).guardar(guardado.capture(), isNull());
        return guardado.getValue();
    }

    private Sistema unSistema() {
        Proyecto proyecto = Proyecto.builder().id(SISTEMA_ID).negocioId(NEGOCIO_ID).nombre("Mi almacén")
                .creadoEn(OffsetDateTime.parse("2026-01-10T09:00:00Z"))
                .actualizadoEn(AHORA).build();
        return new Sistema(proyecto, List.of(), Map.of());
    }

    private GuardarSistemaRequest unaSolicitudDeGuardado() {
        return new GuardarSistemaRequest(USUARIO_ID, SISTEMA_ID.toString(), new SistemaItem(
                SISTEMA_ID.toString(), "Mi almacén", null, null, List.of(
                        new SeccionItem(UUID.randomUUID().toString(), "Vista general", List.<BloqueItem>of())),
                null, null, null, false));
    }

    private GuardarSistemaRequest unaSolicitudDeGuardadoConFilas(int filas) {
        List<Map<String, Object>> filasDelBloque = IntStream.range(0, filas)
                .<Map<String, Object>>mapToObj(i -> Map.of("id", "fila-" + i))
                .toList();
        BloqueItem bloque = new BloqueItem("tabla", "table", "Productos", null, "blue", "half", List.of(),
                filasDelBloque, null, null, null);
        return new GuardarSistemaRequest(USUARIO_ID, SISTEMA_ID.toString(), new SistemaItem(
                SISTEMA_ID.toString(), "Mi almacén", null, null, List.of(
                        new SeccionItem("general", "Vista general", List.of(bloque))),
                null, null, null, false));
    }
}
