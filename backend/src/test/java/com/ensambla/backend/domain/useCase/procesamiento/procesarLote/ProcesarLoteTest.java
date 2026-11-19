package com.ensambla.backend.domain.useCase.procesamiento.procesarLote;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarLoteTest {

    private static final FotoLectura FOTO = new FotoLectura("foto.jpg", "image/jpeg");
    private static final UUID CLIENTES_ID = UUID.fromString("00000000-0000-4000-8000-0000000000c1");

    @Mock
    private ProcesamientoRepository procesamientoRepository;

    @Mock
    private ArchivoCuadernoPort archivoCuadernoPort;

    @Mock
    private LectorCuadernoPort lectorCuadernoPort;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private ControlPlanService controlPlanService;

    private ProcesarLote procesarLote;
    private LoteTrabajo lote;
    private ResultadoIntento resultado;

    @BeforeEach
    void dadoUnLoteDeUnIntento() {
        lote = ProcesamientoTestData.lote(1);
        procesarLote = new ProcesarLote(procesamientoRepository, archivoCuadernoPort, lectorCuadernoPort,
                campoRepository, registroDatosService, controlPlanService,
                ProcesamientoTestData.politica(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Sin trabajo pendiente sólo recupera reclamos vencidos y no lee nada")
    void sinTrabajoPendienteSoloRecuperaReclamosVencidosYNoLeeNada() {
        dadoQueNoHayTrabajoPendiente();

        cuandoSeEjecutaElProcesamiento();

        entoncesSoloSeRecuperaronReclamosVencidosYNoSeLeyoNada();
    }

    @Test
    @DisplayName("Lee archivos, reserva consumo y llama al proveedor en ese orden")
    void leeArchivosReservaConsumoYLlamaAlProveedorEnEseOrden() {
        dadoUnLoteLegibleConReservaAprobada();
        dadoQueElProveedorLee();

        cuandoSeProcesaElLoteReclamado();

        entoncesElIntentoSalioBienSinReintento();
        entoncesSeLeyoSeReservoYSeLlamoAlProveedorEnEseOrden();
    }

    @Test
    @DisplayName("Con bloque destino le pasa a la IA sólo ese bloque, con tipos y opciones")
    void conBloqueDestinoLePasaALaIaSoloEseBloqueConTiposYOpciones() {
        dadoUnLoteConBloqueDestino();
        dadoUnLoteLegibleConOtroBloque();
        dadoQueLaReservaEsAprobada(true);
        dadoQueElProveedorSeCorta();

        cuandoSeProcesaElLoteReclamado();

        entoncesLaIaSoloVioElBloqueDestinoConSusTiposYOpciones();
    }

    @Test
    @DisplayName("Sin bloque destino le pasa a la IA todos los bloques del negocio")
    void sinBloqueDestinoLePasaALaIaTodosLosBloquesDelNegocio() {
        dadoUnLoteLegibleConOtroBloque();
        dadoQueLaReservaEsAprobada(true);
        dadoQueElProveedorSeCorta();

        cuandoSeProcesaElLoteReclamado();

        entoncesLaIaVioTodosLosBloquesDelNegocio();
    }

    @Test
    @DisplayName("Le dice a la IA a qué tabla apunta cada campo de conexión")
    void leDiceALaIaAQueTablaApuntaCadaCampoDeConexion() {
        dadoUnaTablaDeVentasConDosConexiones();
        dadoQueLaReservaEsAprobada(true);
        dadoQueElProveedorSeCorta();

        cuandoSeProcesaElLoteReclamado();

        entoncesLaIaSabeALaTablaQueApuntaCadaConexion();
    }

    @Test
    @DisplayName("Rechaza un lote sin fotos antes de usar archivos o IA")
    void rechazaUnLoteSinFotosAntesDeUsarArchivosOIa() {
        dadoUnLoteSinFotos();

        cuandoSeProcesaElLoteReclamado();

        entoncesSeRechazaPorCantidadDePaginasSinUsarArchivosNiIa();
    }

    @Test
    @DisplayName("Rechaza más fotos que el máximo por lote")
    void rechazaMasFotosQueElMaximoPorLote() {
        dadoUnLoteConOnceFotos();

        cuandoSeProcesaElLoteReclamado();

        entoncesSeRechazaPorMasDeDiezPaginasSinLlamarALaIa();
    }

    @Test
    @DisplayName("No invoca IA cuando la reserva diaria fue rechazada")
    void noInvocaIaCuandoLaReservaDiariaFueRechazada() {
        dadoUnLoteLegible();
        dadoQueLaReservaEsAprobada(false);

        cuandoSeProcesaElLoteReclamado();

        entoncesSeRechazaPorLimiteDiarioSinLlamarALaIa();
    }

    @Test
    @DisplayName("Conserva un fallo transitorio del lector para reintentar")
    void conservaUnFalloTransitorioDelLectorParaReintentar() {
        dadoUnLoteLegibleConReservaAprobada();
        dadoQueElProveedorFallaPor("Timeout");

        cuandoSeProcesaElLoteReclamado();

        entoncesElIntentoFallaPorConReintento("Timeout", true);
    }

    @Test
    @DisplayName("No reintenta un archivo inválido")
    void noReintentaUnArchivoInvalido() {
        dadoUnArchivoQueNoEsValido();

        cuandoSeProcesaElLoteReclamado();

        entoncesElIntentoFallaPorConReintento("Foto inválida", false);
    }

    @Test
    @DisplayName("Oculta detalles técnicos de errores inesperados")
    void ocultaDetallesTecnicosDeErroresInesperados() {
        dadoQueLaBusquedaDeFotosFallaConUnDetalleTecnico();

        cuandoSeProcesaElLoteReclamado();

        entoncesElIntentoFallaPorConReintento(ProcesarLote.ERROR_TEMPORAL, true);
    }

    @Test
    @DisplayName("Finaliza un resultado correcto con costo simulado y sin reintento")
    void finalizaUnResultadoCorrectoConCostoSimuladoYSinReintento() {
        dadoUnLoteLegibleConReservaAprobada();
        dadoQueElProveedorLee();
        dadoUnLoteReclamado();

        cuandoSeEjecutaElProcesamiento();

        entoncesSeFinalizaConCostoCeroYSinReintento();
    }

    @Test
    @DisplayName("Programa un nuevo intento ante un error recuperable")
    void programaUnNuevoIntentoAnteUnErrorRecuperable() {
        dadoUnLoteLegibleConReservaAprobada();
        dadoQueElProveedorFallaPor("Timeout");
        dadoUnLoteReclamado();

        cuandoSeEjecutaElProcesamiento();

        entoncesSeReintentaEnTreintaSegundos();
    }

    @Test
    @DisplayName("Deja ERROR al alcanzar el máximo de intentos")
    void dejaErrorAlAlcanzarElMaximoDeIntentos() {
        dadoUnLoteEnSuTercerIntento();
        dadoUnLoteLegibleConReservaAprobada();
        dadoQueElProveedorFallaPor("Timeout");
        dadoUnLoteReclamado();

        cuandoSeEjecutaElProcesamiento();

        entoncesSeFinalizaSinReintento();
    }

    @Test
    @DisplayName("Recupera reclamos vencidos usando la política de reintentos")
    void recuperaReclamosVencidosUsandoLaPoliticaDeReintentos() {
        dadoUnReclamoVencido();

        cuandoSeEjecutaElProcesamiento();

        entoncesSeFinalizaElReclamoVencidoConReintentoEnTreintaSegundos();
    }

    // --- dado ---
    private void dadoQueNoHayTrabajoPendiente() {
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.empty());
    }

    private void dadoUnLoteReclamado() {
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.of(lote));
    }

    private void dadoUnLoteConBloqueDestino() {
        lote = new LoteTrabajo(LOTE_ID, NEGOCIO_ID, ProcesamientoTestData.BLOQUE_ID, lote.reclamoId(), 1);
    }

    private void dadoUnLoteEnSuTercerIntento() {
        lote = ProcesamientoTestData.lote(3);
    }

    private void dadoUnLoteLegible() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(new BloqueLectura(
                ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(new CampoConocido(
                ProcesamientoTestData.CAMPO_ID, "Monto", TipoCampo.NUMERO, List.of())))));
    }

    private void dadoUnLoteLegibleConOtroBloque() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(
                new BloqueLectura(ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(new CampoConocido(
                        ProcesamientoTestData.CAMPO_ID, "Monto", TipoCampo.NUMERO, List.of()))),
                new BloqueLectura(BloqueId.nuevo(), "Gastos", List.of())));
    }

    private void dadoUnLoteLegibleConReservaAprobada() {
        dadoUnLoteLegible();
        dadoQueLaReservaEsAprobada(true);
    }

    private void dadoUnaTablaDeVentasConDosConexiones() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(
                new BloqueLectura(ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(
                        new CampoConocido(ProcesamientoTestData.CAMPO_ID, "Cliente", TipoCampo.RELACION,
                                List.of(), BloqueId.de(CLIENTES_ID)),
                        new CampoConocido(CampoId.nuevo(), "Fiado", TipoCampo.RELACION, List.of(),
                                BloqueId.nuevo()))),
                new BloqueLectura(BloqueId.de(CLIENTES_ID), "Clientes", List.of())));
    }

    private void dadoUnLoteSinFotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of());
    }

    private void dadoUnLoteConOnceFotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(Collections.nCopies(11, FOTO));
    }

    private void dadoUnArchivoQueNoEsValido() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave()))
                .thenThrow(new LecturaCuadernoException("Foto inválida", null, false));
    }

    private void dadoQueLaBusquedaDeFotosFallaConUnDetalleTecnico() {
        when(procesamientoRepository.fotos(lote)).thenThrow(new IllegalStateException("dato privado"));
    }

    private void dadoUnReclamoVencido() {
        when(procesamientoRepository.vencidos(AHORA)).thenReturn(List.of(lote));
        dadoQueNoHayTrabajoPendiente();
    }

    /** El control de plan admite el intento y delega la reserva diaria en el repositorio. */
    private void dadoQueLaReservaEsAprobada(boolean entra) {
        when(controlPlanService.reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any()))
                .thenAnswer(invocacion -> invocacion.<BooleanSupplier>getArgument(3).getAsBoolean());
        when(procesamientoRepository.reservar(lote, 1, 100, AHORA)).thenReturn(entra);
    }

    private void dadoQueElProveedorLee() {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(ProcesamientoTestData.lectura());
    }

    private void dadoQueElProveedorSeCorta() {
        dadoQueElProveedorFallaPor("Corte");
    }

    private void dadoQueElProveedorFallaPor(String motivo) {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException(motivo));
    }

    // --- cuando ---
    private void cuandoSeEjecutaElProcesamiento() {
        procesarLote.execute();
    }

    private void cuandoSeProcesaElLoteReclamado() {
        dadoUnLoteReclamado();
        cuandoSeEjecutaElProcesamiento();
        ArgumentCaptor<ResultadoIntento> intento = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), intento.capture(), any(), eq(AHORA), any());
        resultado = intento.getValue();
    }

    // --- entonces ---
    @SuppressWarnings("unchecked")
    private List<BloqueConocido> bloquesQueRecibioLaIa(BloqueId destino) {
        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(destino));
        return contexto.getValue();
    }

    private void entoncesSoloSeRecuperaronReclamosVencidosYNoSeLeyoNada() {
        verify(procesamientoRepository).vencidos(AHORA);
        verify(procesamientoRepository, never()).finalizar(any(), any(), any(), any(), any());
        verifyNoInteractions(archivoCuadernoPort, lectorCuadernoPort, controlPlanService);
    }

    private void entoncesElIntentoSalioBienSinReintento() {
        assertThat(resultado.propuesta().lectura().tipoRegistro())
                .isEqualTo(ProcesamientoTestData.lectura().tipoRegistro());
        assertThat(resultado.error()).isNull();
        assertThat(resultado.recuperable()).isFalse();
    }

    private void entoncesSeLeyoSeReservoYSeLlamoAlProveedorEnEseOrden() {
        var orden = inOrder(archivoCuadernoPort, controlPlanService, procesamientoRepository, lectorCuadernoPort);
        orden.verify(archivoCuadernoPort).leer(NEGOCIO_ID, FOTO.clave());
        orden.verify(controlPlanService).reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any());
        orden.verify(procesamientoRepository).reservar(lote, 1, 100, AHORA);
        orden.verify(lectorCuadernoPort).leer(any(), any(), any());
    }

    private void entoncesLaIaSoloVioElBloqueDestinoConSusTiposYOpciones() {
        List<BloqueConocido> contexto = bloquesQueRecibioLaIa(ProcesamientoTestData.BLOQUE_ID);
        assertThat(contexto).extracting(BloqueConocido::nombre).containsExactly("Ventas");
        assertThat(contexto.getFirst().campos())
                .containsExactly(new CampoContexto("Monto", TipoCampo.NUMERO, List.of()));
    }

    private void entoncesLaIaVioTodosLosBloquesDelNegocio() {
        assertThat(bloquesQueRecibioLaIa(null)).extracting(BloqueConocido::nombre)
                .containsExactly("Ventas", "Gastos");
    }

    private void entoncesLaIaSabeALaTablaQueApuntaCadaConexion() {
        assertThat(bloquesQueRecibioLaIa(null).getFirst().campos()).containsExactly(
                new CampoContexto("Cliente", TipoCampo.RELACION, List.of(), "Clientes"),
                new CampoContexto("Fiado", TipoCampo.RELACION, List.of(), null));
    }

    private void entoncesSeRechazaPorCantidadDePaginasSinUsarArchivosNiIa() {
        assertThat(resultado.error()).contains("entre 1 y 10");
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(archivoCuadernoPort, lectorCuadernoPort);
    }

    private void entoncesSeRechazaPorMasDeDiezPaginasSinLlamarALaIa() {
        assertThat(resultado.error()).contains("entre 1 y 10");
        verifyNoInteractions(lectorCuadernoPort);
    }

    private void entoncesSeRechazaPorLimiteDiarioSinLlamarALaIa() {
        assertThat(resultado.error()).isEqualTo(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(lectorCuadernoPort);
    }

    private void entoncesElIntentoFallaPorConReintento(String motivo, boolean recuperable) {
        assertThat(resultado.error()).isEqualTo(motivo);
        assertThat(resultado.recuperable()).isEqualTo(recuperable);
    }

    private void entoncesSeFinalizaConCostoCeroYSinReintento() {
        verify(procesamientoRepository).finalizar(eq(lote), any(), eq(BigDecimal.ZERO), eq(AHORA), isNull());
    }

    private void entoncesSeReintentaEnTreintaSegundos() {
        verify(procesamientoRepository).finalizar(eq(lote), any(), isNull(), eq(AHORA), eq(AHORA.plusSeconds(30)));
    }

    private void entoncesSeFinalizaSinReintento() {
        verify(procesamientoRepository).finalizar(eq(lote), any(), isNull(), eq(AHORA), isNull());
    }

    private void entoncesSeFinalizaElReclamoVencidoConReintentoEnTreintaSegundos() {
        ArgumentCaptor<ResultadoIntento> captor = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), captor.capture(), eq(null), eq(AHORA),
                eq(AHORA.plusSeconds(30)));
        assertThat(captor.getValue().error()).isEqualTo(ProcesarLote.RECLAMO_VENCIDO);
    }
}
