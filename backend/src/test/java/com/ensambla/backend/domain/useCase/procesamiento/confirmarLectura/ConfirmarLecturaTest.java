package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaRequest.CampoItem;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaRequest.FilaItem;
import com.ensambla.backend.support.LoteTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmarLecturaTest {

    private static final ProyectoId PROYECTO_ID = ProyectoId.de("00000000-0000-4000-8000-000000000021");
    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000022");
    private static final UUID MONTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000023");
    private static final UUID FECHA_ID = UUID.fromString("00000000-0000-4000-8000-000000000024");
    private static final UUID EXTRA_ID = UUID.fromString("00000000-0000-4000-8000-000000000025");
    private static final UUID FILA_AJENA_ID = UUID.fromString("00000000-0000-4000-8000-000000000099");
    private static final String NOMBRE_PROYECTO = "Ventas";
    private static final String NOMBRE_BLOQUE = "Hoja 1";

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private LoteSubidaRepository loteSubidaRepository;

    private ConfirmarLectura confirmarLectura;
    private LoteSubida lote;
    private Bloque bloqueNuevo;
    private ConfirmacionLectura confirmacion;
    private ConfirmarLecturaResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnCasoDeUsoDeConfirmarLecturas() {
        confirmarLectura = new ConfirmarLectura(negocioRepository, proyectoRepository, bloqueRepository,
                campoRepository,
                registroDatosService, controlPlanService, registroRepository, loteSubidaRepository,
                new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        lote = loteListo();
    }

    @Test
    @DisplayName("Crea proyecto, tabla, columnas y registros con su foto y confirma el lote al final")
    void creaProyectoTablaColumnasYRegistrosConSuFotoYConfirmaElLoteAlFinal() {
        dadoUnLoteConfirmable();
        dadoUnProyectoYUnaTablaNuevosConMontoYFecha();
        dadoUnaConfirmacionNuevaConLasFilas(filaConPagina(1, 850, "2026-10-03"), filaSinPagina(100));

        cuandoSeConfirma();

        entoncesSeGuardanDosRegistrosUnoConFotoYOtroSin();
        entoncesElLoteSeConfirmaDespuesDeGuardarLosRegistros();
    }

    @Test
    @DisplayName("Crea la tabla dentro de un proyecto existente sin crear otro proyecto")
    void creaLaTablaDentroDeUnProyectoExistenteSinCrearOtroProyecto() {
        dadoUnLoteConfirmable();
        dadoUnProyectoExistenteYUnaTablaNueva();
        dadoUnaConfirmacion(PROYECTO_ID, null, null, List.of(columnaNueva("monto", "Monto", "NUMERO")),
                List.of(filaSinPagina(5)));

        cuandoSeConfirma();

        entoncesLaRespuestaEsDelProyectoExistenteSinCrearOtro();
    }

    @Test
    @DisplayName("Suma las filas a un bloque existente, reutiliza sus campos y agrega los nuevos")
    void sumaLasFilasAUnBloqueExistenteReutilizaSusCamposYAgregaLosNuevos() {
        dadoUnLoteConfirmable();
        dadoUnBloqueExistenteConUnMontoAlQueSeLeAgregaUnaNota();
        dadoUnaConfirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", CampoId.de(MONTO_ID), "Monto", "NUMERO", List.of()),
                        columnaNueva("nota", "Nota", "TEXTO")),
                List.of(new FilaConfirmada(1, Map.of("monto", 20, "nota", "  fiado "))));

        cuandoSeConfirma();

        entoncesSeGuardaLaFilaConElMontoYLaNotaSinCrearProyectoNiTabla();
    }

    @Test
    @DisplayName("Rechaza una columna que apunta a un campo que no es del bloque")
    void rechazaUnaColumnaQueApuntaAUnCampoQueNoEsDelBloque() {
        dadoUnBloqueExistenteSinCampos();
        dadoUnaConfirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", CampoId.de(MONTO_ID), "Monto", "NUMERO", List.of())),
                List.of());

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaPorCampoInexistenteSinValidarRegistros();
    }

    @Test
    @DisplayName("Ubica el error de una columna nueva por su posición")
    void ubicaElErrorDeUnaColumnaNuevaPorSuPosicion() {
        dadoUnProyectoYUnaTablaNuevosSinCampos();
        dadoUnaConfirmacion(null, NOMBRE_PROYECTO, null, List.of(columnaNueva("monto", "  ", "NUMERO")),
                List.of(filaSinPagina(1)));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElCampoPorErrores(Map.of("campos[0].nombre", "Escribí un nombre"));
        entoncesNoSeValidaNingunRegistro();
    }

    @Test
    @DisplayName("Ubica en su columna el nombre repetido de un campo nuevo y no guarda registros")
    void ubicaEnSuColumnaElNombreRepetidoDeUnCampoNuevoYNoGuardaRegistros() {
        dadoUnBloqueExistenteConUnMonto();
        dadoUnaConfirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", CampoId.de(MONTO_ID), "Monto", "NUMERO", List.of()),
                        new CampoConfirmado("importe", null, "Monto", "NUMERO", List.of())),
                List.of(new FilaConfirmada(1, Map.of("monto", 1, "importe", 2))));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElCampoPorErrores(
                Map.of("campos[1].nombre", "Ya hay un campo llamado \"Monto\" en este bloque"));
        entoncesNoSeGuardaNadaNiSeConfirmaElLote();
    }

    @Test
    @DisplayName("Ubica el valor inválido de una fila por su posición y su columna y no guarda nada")
    void ubicaElValorInvalidoDeUnaFilaPorSuPosicionYSuColumnaYNoGuardaNada() {
        dadoUnProyectoYUnaTablaNuevosConMontoYFecha();
        dadoUnaConfirmacionNuevaConLasFilas(filaConPagina(1, 850, "2026-10-03"), filaSinPagina("abc"));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElRegistroPorErrores(Map.of("filas[1].monto", "Tiene que ser un número"));
        entoncesNoSeGuardaNingunRegistroNiSeConfirmaElLote();
    }

    @Test
    @DisplayName("Marca una página que el lote no tiene")
    void marcaUnaPaginaQueElLoteNoTiene() {
        dadoUnProyectoYUnaTablaNuevosConMontoYFecha();
        dadoUnaConfirmacionNuevaConLasFilas(filaConPagina(4, 850, "2026-10-03"));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElRegistroPorErrores(Map.of("filas[0].pagina", "Esta página no está en el cuaderno"));
        entoncesNoSeGuardaNingunRegistro();
    }

    @Test
    @DisplayName("Ubica por id el error de un campo obligatorio del bloque que la revisión no incluyó")
    void ubicaPorIdElErrorDeUnCampoObligatorioDelBloqueQueLaRevisionNoIncluyo() {
        dadoUnBloqueExistenteConUnMontoYUnExtraObligatorio();
        dadoUnaConfirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", CampoId.de(MONTO_ID), "Monto", "NUMERO", List.of())),
                List.of(new FilaConfirmada(1, Map.of("monto", 3))));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElRegistroPorErrores(Map.of("filas[0]." + EXTRA_ID, "Este dato es obligatorio"));
    }

    @Test
    @DisplayName("Ubica por columna el error de una conexión que apunta a una fila que no existe")
    void ubicaPorColumnaElErrorDeUnaConexionQueApuntaAUnaFilaQueNoExiste() {
        dadoUnBloqueExistenteConUnaConexionACliente();
        dadoQueLaFilaConectadaYaNoExiste();
        dadoUnaConfirmacion(null, null, BLOQUE_ID, List.of(columnaDeConexion()),
                List.of(new FilaConfirmada(1, Map.of("cliente", FILA_AJENA_ID.toString()))));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaElRegistroPorErrores(Map.of("filas[0].cliente", "Esa fila ya no existe"));
        entoncesNoSeGuardaNingunRegistro();
    }

    @Test
    @DisplayName("Guarda las filas con la conexión elegida cuando la fila existe")
    void guardaLasFilasConLaConexionElegidaCuandoLaFilaExiste() {
        dadoUnLoteConfirmable();
        dadoUnBloqueExistenteConUnaConexionACliente();
        dadoUnaConfirmacion(null, null, BLOQUE_ID, List.of(columnaDeConexion()),
                List.of(new FilaConfirmada(1, Map.of("cliente", FILA_AJENA_ID.toString()))));

        cuandoSeConfirma();

        entoncesSeGuardaLaFilaConLaConexionElegida();
    }

    @Test
    @DisplayName("No toca nada cuando el lote no se puede confirmar")
    void noTocaNadaCuandoElLoteNoSePuedeConfirmar() {
        dadoUnLoteYaConfirmado();
        dadoUnaConfirmacionNuevaConLasFilas(filaSinPagina(1));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaPorQueElCuadernoYaSeGuardo();
        entoncesNoSeTocaNingunRepositorioNiSeConfirmaElLote();
    }

    @Test
    @DisplayName("No toca nada cuando la confirmación no tiene columnas")
    void noTocaNadaCuandoLaConfirmacionNoTieneColumnas() {
        dadoUnaConfirmacion(null, NOMBRE_PROYECTO, null, List.of(), List.of());

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaPorFaltaDeColumnas();
        entoncesNoSeTocaNingunRepositorioNiSeConfirmaElLote();
    }

    @Test
    @DisplayName("Si otra confirmación se adelantó no deja nada guardado")
    void siOtraConfirmacionSeAdelantoNoDejaNadaGuardado() {
        dadoUnBloqueExistenteConUnMonto();
        dadoQueOtraConfirmacionSeAdelanto();
        dadoUnaConfirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", CampoId.de(MONTO_ID), "Monto", "NUMERO", List.of())),
                List.of(new FilaConfirmada(1, Map.of("monto", 3))));

        cuandoSeIntentaConfirmar();

        entoncesSeRechazaPorQueElCuadernoYaSeGuardo();
    }

    @Test
    @DisplayName("No busca el lote cuando el usuario no tiene negocio")
    void noBuscaElLoteCuandoElUsuarioNoTieneNegocio() {
        dadoUnUsuarioSinNegocio();
        dadoUnaConfirmacionNuevaConLasFilas(filaSinPagina(1));

        cuandoSeIntentaConfirmarSinNegocio();

        entoncesSeRechazaPorReglaDeNegocioSinBuscarNada();
    }

    // --- dado ---
    private void dadoUnLoteConfirmable() {
        when(loteSubidaRepository.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA)).thenReturn(true);
    }

    private void dadoQueOtraConfirmacionSeAdelanto() {
        when(loteSubidaRepository.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA)).thenReturn(false);
    }

    private void dadoUnLoteYaConfirmado() {
        lote = loteListo();
        lote.setEstado(EstadoLote.CONFIRMADO);
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new BusinessRuleException("Sin negocio"));
    }

    private void dadoUnProyectoYUnaTablaNuevosConMontoYFecha() {
        dadoUnProyectoYUnaTablaNuevos();
        dadoQueSeCreaElCampoNuevo("Monto", unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        dadoQueSeCreaElCampoNuevo("Fecha", unCampo(FECHA_ID, "Fecha", TipoCampo.FECHA, false));
        dadoLosCamposDelBloque(List.of(), List.of(
                unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false),
                unCampo(FECHA_ID, "Fecha", TipoCampo.FECHA, false)), 2);
    }

    private void dadoUnProyectoYUnaTablaNuevosSinCampos() {
        dadoUnProyectoYUnaTablaNuevos();
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnProyectoYUnaTablaNuevos() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        bloqueNuevo = unBloque();
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(bloqueRepository.save(any(Bloque.class))).thenReturn(bloqueNuevo);
    }

    private void dadoUnProyectoExistenteYUnaTablaNueva() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(bloqueRepository.save(any(Bloque.class))).thenReturn(unBloque());
        dadoQueSeCreaElCampoNuevo("Monto", unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        dadoLosCamposDelBloque(List.of(), List.of(unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false)), 1);
    }

    private void dadoUnBloqueExistente() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(unBloque());
    }

    private void dadoUnBloqueExistenteSinCampos() {
        dadoUnBloqueExistente();
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnBloqueExistenteConUnMonto() {
        dadoUnBloqueExistente();
        Campo monto = unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false);
        dadoLosCamposDelBloque(List.of(monto), List.of(monto));
    }

    private void dadoUnBloqueExistenteConUnMontoAlQueSeLeAgregaUnaNota() {
        dadoUnBloqueExistente();
        Campo monto = unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false);
        Campo nota = unCampo(FECHA_ID, "Nota", TipoCampo.TEXTO, false);
        dadoQueSeCreaElCampoNuevo("Nota", nota);
        dadoLosCamposDelBloque(List.of(monto), List.of(monto, nota), 1);
    }

    private void dadoUnBloqueExistenteConUnMontoYUnExtraObligatorio() {
        dadoUnBloqueExistente();
        Campo monto = unCampo(MONTO_ID, "Monto", TipoCampo.NUMERO, false);
        Campo extra = unCampo(EXTRA_ID, "Extra", TipoCampo.TEXTO, true);
        dadoLosCamposDelBloque(List.of(monto, extra), List.of(monto, extra));
    }

    private void dadoUnBloqueExistenteConUnaConexionACliente() {
        dadoUnBloqueExistente();
        Campo cliente = unCampo(EXTRA_ID, "Cliente", TipoCampo.RELACION, false);
        dadoLosCamposDelBloque(List.of(cliente), List.of(cliente));
    }

    private void dadoQueLaFilaConectadaYaNoExiste() {
        doThrow(new RegistroInvalidoException(Map.of(EXTRA_ID.toString(), "Esa fila ya no existe")))
                .when(registroDatosService).validarConexiones(eq(NEGOCIO_ID), any(), any());
    }

    private void dadoQueSeCreaElCampoNuevo(String nombre, Campo campo) {
        when(campoRepository.save(argThat(nuevo -> nuevo != null && nombre.equals(nuevo.getNombre()))))
                .thenReturn(campo);
    }

    private void dadoLosCamposDelBloque(List<Campo> antes, List<Campo> despues) {
        dadoLosCamposDelBloque(antes, despues, 0);
    }

    /** Se leen los campos del bloque al empezar, antes de crear cada columna nueva y al armar los registros. */
    private void dadoLosCamposDelBloque(List<Campo> antes, List<Campo> despues, int columnasNuevas) {
        OngoingStubbing<List<Campo>> lecturas =
                when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(antes);
        for (int i = 0; i < columnasNuevas; i++) {
            lecturas = lecturas.thenReturn(antes);
        }
        lecturas.thenReturn(despues);
    }

    private void dadoUnaConfirmacionNuevaConLasFilas(FilaConfirmada... filas) {
        dadoUnaConfirmacion(null, NOMBRE_PROYECTO, null, List.of(
                columnaNueva("monto", "Monto", "NUMERO"), columnaNueva("fecha", "Fecha", "FECHA")), List.of(filas));
    }

    private void dadoUnaConfirmacion(ProyectoId proyectoId, String nombreProyecto, BloqueId bloqueId,
                                     List<CampoConfirmado> campos, List<FilaConfirmada> filas) {
        confirmacion = new ConfirmacionLectura(proyectoId, nombreProyecto, bloqueId, NOMBRE_BLOQUE, campos, filas);
    }

    private LoteSubida loteListo() {
        LoteSubida listo = LoteTestData.lote(List.of(LoteTestData.foto()));
        listo.setId(LOTE_ID);
        listo.setEstado(EstadoLote.LISTO);
        return listo;
    }

    private Bloque unBloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build();
    }

    private Campo unCampo(UUID id, String nombre, TipoCampo tipo, boolean obligatorio) {
        return Campo.builder().id(CampoId.de(id)).nombre(nombre).tipo(tipo).obligatorio(obligatorio)
                .posicion((short) 0).build();
    }

    private CampoConfirmado columnaNueva(String clave, String nombre, String tipo) {
        return new CampoConfirmado(clave, null, nombre, tipo, List.of());
    }

    private CampoConfirmado columnaDeConexion() {
        return new CampoConfirmado("cliente", CampoId.de(EXTRA_ID), "Cliente", "RELACION", List.of());
    }

    private FilaConfirmada filaConPagina(int pagina, Object monto, String fecha) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("monto", monto);
        valores.put("fecha", fecha);
        return new FilaConfirmada(pagina, valores);
    }

    private FilaConfirmada filaSinPagina(Object monto) {
        return new FilaConfirmada(null, Map.of("monto", monto));
    }

    private ConfirmarLecturaRequest unPedidoDeConfirmacion() {
        return new ConfirmarLecturaRequest(USUARIO_ID, LOTE_ID, confirmacion.proyectoId(),
                confirmacion.nombreProyecto(), confirmacion.bloqueId(), confirmacion.nombreBloque(),
                confirmacion.campos().stream()
                        .map(campo -> new CampoItem(campo.clave(), campo.campoId(), campo.nombre(), campo.tipo(),
                                campo.opciones()))
                        .toList(),
                confirmacion.filas().stream().map(fila -> new FilaItem(fila.pagina(), fila.valores())).toList());
    }

    // --- cuando ---
    private void cuandoSeConfirma() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(lote);
        respuesta = confirmarLectura.execute(unPedidoDeConfirmacion());
    }

    private void cuandoSeIntentaConfirmar() {
        rechazo = catchThrowable(this::cuandoSeConfirma);
    }

    private void cuandoSeIntentaConfirmarSinNegocio() {
        rechazo = catchThrowable(() -> confirmarLectura.execute(unPedidoDeConfirmacion()));
    }

    // --- entonces ---
    private List<Registro> registrosGuardados() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Registro>> registros = ArgumentCaptor.forClass(List.class);
        verify(registroRepository).saveAll(registros.capture());
        return registros.getValue();
    }

    private void entoncesSeGuardanDosRegistrosUnoConFotoYOtroSin() {
        assertThat(respuesta).isEqualTo(new ConfirmarLecturaResponse(PROYECTO_ID, BLOQUE_ID, 2));
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 2, AHORA);
        List<Registro> guardados = registrosGuardados();
        assertThat(guardados).hasSize(2);
        assertThat(guardados.get(0).getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardados.get(0).getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardados.get(0).getFotoOrigenId()).isEqualTo(FOTO_ID);
        assertThat(guardados.get(0).getValores())
                .containsEntry(MONTO_ID.toString(), 850)
                .containsEntry(FECHA_ID.toString(), "2026-10-03");
        assertThat(guardados.get(1).getFotoOrigenId()).isNull();
        assertThat(guardados.get(1).getValores()).containsOnly(Map.entry(MONTO_ID.toString(), 100));
    }

    private void entoncesElLoteSeConfirmaDespuesDeGuardarLosRegistros() {
        InOrder orden = inOrder(registroRepository, loteSubidaRepository);
        orden.verify(registroRepository).saveAll(any());
        orden.verify(loteSubidaRepository).confirmar(lote.getId(), lote.getNegocioId(), bloqueNuevo.getId(), AHORA);
    }

    private void entoncesLaRespuestaEsDelProyectoExistenteSinCrearOtro() {
        assertThat(respuesta.proyectoId()).isEqualTo(PROYECTO_ID);
        verify(proyectoRepository, never()).save(any());
    }

    private void entoncesSeGuardaLaFilaConElMontoYLaNotaSinCrearProyectoNiTabla() {
        assertThat(respuesta.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(registrosGuardados().get(0).getValores())
                .containsEntry(MONTO_ID.toString(), 20)
                .containsEntry(FECHA_ID.toString(), "fiado");
        verifyNoInteractions(proyectoRepository);
        verify(bloqueRepository, never()).save(any());
    }

    private void entoncesSeGuardaLaFilaConLaConexionElegida() {
        assertThat(registrosGuardados().getFirst().getValores())
                .containsEntry(EXTRA_ID.toString(), FILA_AJENA_ID.toString());
        verify(registroDatosService).validarConexiones(eq(NEGOCIO_ID), any(), any());
    }

    private void entoncesSeRechazaPorCampoInexistenteSinValidarRegistros() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Ese dato ya no existe en la sección");
        verifyNoInteractions(registroDatosService);
    }

    private void entoncesSeRechazaElCampoPorErrores(Map<String, String> errores) {
        assertThat(rechazo).isInstanceOfSatisfying(CampoInvalidoException.class,
                e -> assertThat(e.getErrores()).containsExactlyInAnyOrderEntriesOf(errores));
    }

    private void entoncesSeRechazaElRegistroPorErrores(Map<String, String> errores) {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                e -> assertThat(e.getErrores()).containsExactlyInAnyOrderEntriesOf(errores));
    }

    private void entoncesNoSeValidaNingunRegistro() {
        verifyNoInteractions(registroDatosService);
    }

    private void entoncesNoSeGuardaNingunRegistro() {
        verifyNoInteractions(registroRepository);
    }

    private void entoncesNoSeGuardaNadaNiSeConfirmaElLote() {
        verifyNoInteractions(registroDatosService, registroRepository);
        verify(loteSubidaRepository, never()).confirmar(any(), any(), any(), any());
    }

    private void entoncesNoSeGuardaNingunRegistroNiSeConfirmaElLote() {
        verifyNoInteractions(registroRepository);
        verify(loteSubidaRepository, never()).confirmar(any(), any(), any(), any());
    }

    private void entoncesSeRechazaPorQueElCuadernoYaSeGuardo() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage("Este cuaderno ya se guardó");
    }

    private void entoncesSeRechazaPorFaltaDeColumnas() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage("Dejá al menos una columna");
    }

    private void entoncesNoSeTocaNingunRepositorioNiSeConfirmaElLote() {
        verifyNoInteractions(proyectoRepository, bloqueRepository, campoRepository, registroDatosService,
                registroRepository);
        verify(loteSubidaRepository, never()).confirmar(any(), any(), any(), any());
    }

    private void entoncesSeRechazaPorReglaDeNegocioSinBuscarNada() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(loteSubidaRepository, proyectoRepository, bloqueRepository, registroRepository);
    }
}
