package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.CamposDePrueba.referenciaA;
import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final CrearCampo crearCampo = new CampoUseCaseConfig().crearCampo(
            negocioRepository,
            bloqueRepository,
            campoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private CampoItem campoCreado;
    private Throwable rechazo;

    @Test
    @DisplayName("El campo nuevo se guarda al final de los del bloque de la sesión con la hora del reloj")
    void elCampoNuevoSeGuardaAlFinalDeLosDelBloqueDeLaSesionConLaHoraDelReloj() {
        dadoUnBloqueCon(unCampoDelBloque("Producto", 1));

        cuandoSeCreaUnCampoDeTexto(" Precio ");

        entoncesSeGuardaElCampoPrecioAlFinalDelBloque();
    }

    @Test
    @DisplayName("Devuelve el campo con la configuración pedida")
    void devuelveElCampoConLaConfiguracionPedida() {
        dadoUnBloqueSinCampos();

        cuandoSeCreaUnTalleDeOpcion();

        entoncesLaRespuestaEsUnaOpcionConLosTalles();
    }

    @Test
    @DisplayName("Si el bloque es ajeno no se crea ningún campo")
    void siElBloqueEsAjenoNoSeCreaNingunCampo() {
        dadoUnBloqueAjeno();

        cuandoSeIntentaCrearUnCampoDeTexto("Talle");

        entoncesSeRechazaPorqueElBloqueNoExiste();
        entoncesNoSeTocaNingunCampo();
    }

    @Test
    @DisplayName("Con un nombre repetido en el bloque no se guarda nada")
    void conUnNombreRepetidoEnElBloqueNoSeGuardaNada() {
        dadoUnBloqueCon(unCampoDelBloque("Producto", 0));

        cuandoSeIntentaCrearUnCampoDeTexto("producto");

        entoncesSeRechazaPorNombreRepetido();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Con una configuración inválida no se guarda nada")
    void conUnaConfiguracionInvalidaNoSeGuardaNada() {
        dadoUnBloqueSinCampos();

        cuandoSeIntentaCrearUnCampoDeOpcionSinOpciones("Talle");

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Crear un campo que no es de conexión no consulta otras tablas")
    void crearUnCampoQueNoEsDeConexionNoConsultaOtrasTablas() {
        dadoUnBloqueSinCampos();

        cuandoSeCreaUnCampoDeTexto("Precio");

        entoncesSeConsultaUnaSolaVezElBloque();
    }

    @Test
    @DisplayName("Se guarda una conexión con una tabla del mismo sistema")
    void seGuardaUnaConexionConUnaTablaDelMismoSistema() {
        dadoUnBloqueSinCampos();
        dadoUnaTablaRelacionadaDelSistema(PROYECTO_ID);

        cuandoSeCreaUnaConexion("Cliente");

        entoncesSeGuardaUnaConexionConLaTablaRelacionada();
    }

    @Test
    @DisplayName("Se rechaza una conexión con una tabla inexistente o de otro negocio")
    void seRechazaUnaConexionConUnaTablaInexistenteODeOtroNegocio() {
        dadoUnBloqueSinCampos();
        dadoUnaTablaRelacionadaInexistente();

        cuandoSeIntentaCrearUnaConexion("Cliente");

        entoncesSeRechazaPorqueLaTablaNoEsDeEsteSistema();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Se rechaza una conexión con una tabla de otro sistema")
    void seRechazaUnaConexionConUnaTablaDeOtroSistema() {
        dadoUnBloqueSinCampos();
        dadoUnaTablaRelacionadaDelSistema(OTRO_PROYECTO_ID);

        cuandoSeIntentaCrearUnaConexion("Cliente");

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Se guarda una fórmula que usa campos numéricos del bloque")
    void seGuardaUnaFormulaQueUsaCamposNumericosDelBloque() {
        Campo cantidad = unCampoDelBloque("Cantidad", 0);
        dadoUnBloqueCon(cantidad);

        cuandoSeCreaUnaFormulaQueUsaA(cantidad);

        entoncesSeGuardaUnaFormulaQueUsaA(cantidad);
    }

    @Test
    @DisplayName("Se rechaza una fórmula que cita un campo que no es del bloque")
    void seRechazaUnaFormulaQueCitaUnCampoQueNoEsDelBloque() {
        dadoUnBloqueCon(unCampoDelBloque("Cantidad", 0));

        cuandoSeIntentaCrearUnaFormulaQueUsaA(unCampoNumerico("De otra tabla"));

        entoncesSeRechazaPorqueLaFormulaSoloUsaCamposNumericosDeLaTabla();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Se rechaza una fórmula que cita un campo que no es numérico")
    void seRechazaUnaFormulaQueCitaUnCampoQueNoEsNumerico() {
        Campo nombre = unCampoDeTexto("Nombre").ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) 0, AHORA);
        dadoUnBloqueCon(nombre);

        cuandoSeIntentaCrearUnaFormulaQueUsaA(nombre);

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesNoSeGuardaNingunCampo();
    }

    private void dadoUnBloqueSinCampos() {
        dadoUnBloqueCon();
    }

    private void dadoUnBloqueCon(Campo... campos) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unBloqueDelSistema(BLOQUE_ID, PROYECTO_ID)));
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnBloqueAjeno() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaTablaRelacionadaDelSistema(ProyectoId proyectoId) {
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unBloqueDelSistema(RELACIONADO_ID, proyectoId)));
    }

    private void dadoUnaTablaRelacionadaInexistente() {
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private Bloque unBloqueDelSistema(BloqueId bloqueId, ProyectoId proyectoId) {
        return Bloque.builder().id(bloqueId).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
    }

    private Campo unCampoDelBloque(String nombre, int posicion) {
        return unCampoNumerico(nombre).ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) posicion, AHORA);
    }

    private void cuandoSeCreaUnCampoDeTexto(String nombre) {
        campoCreado = crear(pedido(nombre, "TEXTO", null, null));
    }

    private void cuandoSeCreaUnTalleDeOpcion() {
        campoCreado = crear(new CrearCampoRequest(
                USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));
    }

    private void cuandoSeCreaUnaConexion(String nombre) {
        campoCreado = crear(pedido(nombre, "RELACION", RELACIONADO_ID, null));
    }

    private void cuandoSeCreaUnaFormulaQueUsaA(Campo campo) {
        campoCreado = crear(pedido("Total", "FORMULA", null, referenciaA(campo) + " * 2"));
    }

    private void cuandoSeIntentaCrearUnCampoDeTexto(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeCreaUnCampoDeTexto(nombre));
    }

    private void cuandoSeIntentaCrearUnCampoDeOpcionSinOpciones(String nombre) {
        rechazo = catchThrowable(() -> crear(pedido(nombre, "OPCION", null, null)));
    }

    private void cuandoSeIntentaCrearUnaConexion(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeCreaUnaConexion(nombre));
    }

    private void cuandoSeIntentaCrearUnaFormulaQueUsaA(Campo campo) {
        rechazo = catchThrowable(() -> cuandoSeCreaUnaFormulaQueUsaA(campo));
    }

    private CampoItem crear(CrearCampoRequest pedido) {
        return crearCampo.execute(pedido);
    }

    private CrearCampoRequest pedido(String nombre, String tipo, BloqueId relacionadoId, String formula) {
        return new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, nombre, tipo, null, null, null, relacionadoId, formula);
    }

    private Campo campoGuardado() {
        ArgumentCaptor<Campo> guardado = ArgumentCaptor.forClass(Campo.class);
        verify(campoRepository).save(guardado.capture());
        return guardado.getValue();
    }

    private void entoncesSeGuardaElCampoPrecioAlFinalDelBloque() {
        Campo guardado = campoGuardado();
        assertThat(guardado.getNombre()).isEqualTo("Precio");
        assertThat(guardado.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getPosicion()).isEqualTo((short) 2);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campoCreado.nombre()).isEqualTo("Precio");
    }

    private void entoncesLaRespuestaEsUnaOpcionConLosTalles() {
        assertThat(campoCreado.tipo()).isEqualTo("OPCION");
        assertThat(campoCreado.opciones()).containsExactlyElementsOf(TALLES);
    }

    private void entoncesSeGuardaUnaConexionConLaTablaRelacionada() {
        Campo guardado = campoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(guardado.getBloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    private void entoncesSeGuardaUnaFormulaQueUsaA(Campo campo) {
        Campo guardado = campoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoCampo.FORMULA);
        assertThat(guardado.getFormula()).isEqualTo(referenciaA(campo) + " * 2");
    }

    private void entoncesSeRechazaPorqueElBloqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeRechazaPorNombreRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesSeRechazaPorConfiguracionInvalida() {
        assertThat(rechazo).isInstanceOf(CampoInvalidoException.class);
    }

    private void entoncesSeRechazaPorqueLaTablaNoEsDeEsteSistema() {
        assertThat(rechazo).isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                .isEqualTo(Map.of("bloqueRelacionadoId", "Elegí una tabla de este sistema")));
    }

    private void entoncesSeRechazaPorqueLaFormulaSoloUsaCamposNumericosDeLaTabla() {
        assertThat(rechazo).isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                .containsEntry("formula", "Usá sólo campos numéricos de esta tabla"));
    }

    private void entoncesNoSeGuardaNingunCampo() {
        verify(campoRepository, never()).save(any());
    }

    private void entoncesNoSeTocaNingunCampo() {
        verifyNoInteractions(campoRepository);
    }

    private void entoncesSeConsultaUnaSolaVezElBloque() {
        verify(bloqueRepository, times(1)).findByIdAndNegocioId(any(), any());
    }
}
