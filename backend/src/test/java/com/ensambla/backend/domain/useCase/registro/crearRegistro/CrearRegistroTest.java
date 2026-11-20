package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearRegistroTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId GARANTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final RegistroId ANA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroRepository registroRepository;

    private CrearRegistro crearRegistro;
    private CrearRegistroResponse creado;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        RegistroDatosService registroDatosService =
                new RegistroDatosService(registroRepository, mock(BloqueRepository.class),
                        mock(HistorialPrecioRepository.class), new TransaccionDirecta());
        crearRegistro = new CrearRegistro(negocioRepository, bloqueRepository, campoRepository, registroDatosService,
                controlPlanService, registroRepository, new TransaccionDirecta(),
                Clock.fixed(INSTANTE, ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Crea en el bloque del negocio de la sesión el registro con los valores validados y lo devuelve")
    void creaElRegistroConLosValoresValidadosEnElBloqueDelNegocio() {
        dadoUnBloqueConCampos(unosCamposDeNombreYCantidad());
        dadoQueGuardarDevuelveElMismoRegistro();

        cuandoSeCreaConValores(Map.of(NOMBRE_ID.toString(), "  Harina ", "ajeno", 1));

        entoncesSeGuardaElRegistroConElNombre("Harina");
        entoncesSeDevuelveElRegistroConElNombre("Harina");
        entoncesSeExigioElCupoDeAltas();
    }

    @Test
    @DisplayName("No guarda nada cuando los valores no son válidos")
    void noGuardaNadaCuandoLosValoresNoSonValidos() {
        dadoUnBloqueConCampos(unosCamposDeNombreYCantidad());

        cuandoSeIntentaCrearConValores(Map.of(CANTIDAD_ID.toString(), 3));

        entoncesSeRechazaPorValoresInvalidos();
        entoncesNoSeGuardaNingunRegistro();
    }

    @Test
    @DisplayName("Crea el registro cuando la fila a la que apunta la conexión existe")
    void creaElRegistroCuandoLaFilaALaQueApuntaLaConexionExiste() {
        dadoUnBloqueConCampos(unasConexiones());
        dadoQueExistenLasFilas(Set.of(ANA_ID), ANA_ID);
        dadoQueGuardarDevuelveElMismoRegistro();

        cuandoSeCreaConValores(valoresDelCliente(ANA_ID));

        entoncesElRegistroApuntaALaFila(ANA_ID);
    }

    @Test
    @DisplayName("Rechaza el registro cuando la fila a la que apunta la conexión ya no existe")
    void rechazaElRegistroCuandoLaFilaALaQueApuntaLaConexionYaNoExiste() {
        dadoUnBloqueConCampos(unasConexiones());
        dadoQueExistenLasFilas(Set.of(ANA_ID));

        cuandoSeIntentaCrearConValores(valoresDelCliente(ANA_ID));

        entoncesSeRechazaPorFilaInexistenteEn(CLIENTE_ID);
        entoncesNoSeGuardaNingunRegistro();
    }

    @Test
    @DisplayName("Busca de una vez las filas de todas las conexiones a la misma tabla y marca sólo las que faltan")
    void buscaDeUnaVezLasFilasDeLasConexionesALaMismaTablaYMarcaSoloLasQueFaltan() {
        dadoUnBloqueConCampos(unasConexiones());
        dadoQueExistenLasFilas(Set.of(ANA_ID, BETO_ID), ANA_ID);

        cuandoSeIntentaCrearConValores(
                Map.of(CLIENTE_ID.toString(), ANA_ID.toString(), GARANTE_ID.toString(), BETO_ID.toString()));

        entoncesSeRechazaPorFilaInexistenteEn(GARANTE_ID);
    }

    @Test
    @DisplayName("No consulta otras tablas cuando las conexiones quedan vacías")
    void noConsultaOtrasTablasCuandoLasConexionesQuedanVacias() {
        dadoUnBloqueConCampos(unasConexiones());
        dadoQueGuardarDevuelveElMismoRegistro();

        cuandoSeCreaConValores(Map.of());

        entoncesNoSeConsultaNingunaOtraTabla();
    }

    @Test
    @DisplayName("No valida ni guarda cuando el bloque no es del negocio")
    void noValidaNiGuardaCuandoElBloqueNoEsDelNegocio() {
        dadoQueElBloqueNoEsDelNegocio();

        cuandoSeIntentaCrearConValores(Map.of());

        entoncesSeRechazaPorBloqueInexistente();
        entoncesNoSeConsultaNingunCampoNiSeGuardaNada();
    }

    // --- dado ---
    private void dadoUnBloqueConCampos(List<Campo> campos) {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
    }

    private void dadoQueExistenLasFilas(Set<RegistroId> buscadas, RegistroId... existentes) {
        List<Registro> filas = List.of(existentes).stream()
                .map(id -> Registro.builder().id(id).bloqueId(CLIENTES_ID).build())
                .toList();
        when(registroRepository.findAllByIdsAndBloque(buscadas, CLIENTES_ID, NEGOCIO_ID)).thenReturn(filas);
    }

    private void dadoQueGuardarDevuelveElMismoRegistro() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoQueElBloqueNoEsDelNegocio() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private static List<Campo> unasConexiones() {
        return List.of(
                Campo.builder().id(CLIENTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(GARANTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
    }

    private static List<Campo> unosCamposDeNombreYCantidad() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    private static Map<String, Object> valoresDelCliente(RegistroId filaId) {
        return Map.of(CLIENTE_ID.toString(), filaId.toString());
    }

    // --- cuando ---
    private void cuandoSeCreaConValores(Map<String, Object> valores) {
        creado = crearRegistro.execute(new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, valores));
    }

    private void cuandoSeIntentaCrearConValores(Map<String, Object> valores) {
        rechazo = catchThrowable(() -> cuandoSeCreaConValores(valores));
    }

    // --- entonces ---
    private void entoncesSeGuardaElRegistroConElNombre(String nombre) {
        ArgumentCaptor<Registro> guardado = ArgumentCaptor.forClass(Registro.class);
        verify(registroRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getValue().getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getValue().getValores()).containsOnly(Map.entry(NOMBRE_ID.toString(), nombre));
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeDevuelveElRegistroConElNombre(String nombre) {
        assertThat(creado.registro().valores()).containsOnly(Map.entry(NOMBRE_ID.toString(), nombre));
    }

    private void entoncesSeExigioElCupoDeAltas() {
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
    }

    private void entoncesElRegistroApuntaALaFila(RegistroId filaId) {
        assertThat(creado.registro().valores()).containsEntry(CLIENTE_ID.toString(), filaId.toString());
    }

    private void entoncesSeRechazaPorValoresInvalidos() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
    }

    private void entoncesSeRechazaPorFilaInexistenteEn(CampoId campoId) {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class)
                .extracting(error -> ((RegistroInvalidoException) error).getErrores())
                .isEqualTo(Map.of(campoId.toString(), "Esa fila ya no existe"));
    }

    private void entoncesSeRechazaPorBloqueInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNingunRegistro() {
        verify(registroRepository, never()).save(any());
    }

    private void entoncesNoSeConsultaNingunaOtraTabla() {
        verify(registroRepository, never()).findAllByIdsAndBloque(any(), any(), any());
    }

    private void entoncesNoSeConsultaNingunCampoNiSeGuardaNada() {
        verifyNoInteractions(campoRepository, registroRepository);
    }
}
