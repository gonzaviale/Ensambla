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

    @BeforeEach
    void setUp() {
        lote = ProcesamientoTestData.lote(1);
        procesarLote = new ProcesarLote(procesamientoRepository, archivoCuadernoPort, lectorCuadernoPort,
                campoRepository, registroDatosService, controlPlanService,
                ProcesamientoTestData.politica(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Sin trabajo pendiente sólo recupera reclamos vencidos y no lee nada")
    void executeShouldStopWhenQueueEmpty() {
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.empty());

        procesarLote.execute();

        verify(procesamientoRepository).vencidos(AHORA);
        verify(procesamientoRepository, never()).finalizar(any(), any(), any(), any(), any());
        verifyNoInteractions(archivoCuadernoPort, lectorCuadernoPort, controlPlanService);
    }

    @Test
    @DisplayName("Lee archivos, reserva consumo y llama al proveedor en ese orden")
    void executeShouldReadAndReserveBeforeProvider() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(ProcesamientoTestData.lectura());

        ResultadoIntento resultado = procesar();

        assertThat(resultado.propuesta().lectura().tipoRegistro())
                .isEqualTo(ProcesamientoTestData.lectura().tipoRegistro());
        assertThat(resultado.error()).isNull();
        assertThat(resultado.recuperable()).isFalse();
        var orden = inOrder(archivoCuadernoPort, controlPlanService, procesamientoRepository, lectorCuadernoPort);
        orden.verify(archivoCuadernoPort).leer(NEGOCIO_ID, FOTO.clave());
        orden.verify(controlPlanService).reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any());
        orden.verify(procesamientoRepository).reservar(lote, 1, 100, AHORA);
        orden.verify(lectorCuadernoPort).leer(any(), any(), any());
    }

    @Test
    @DisplayName("Con bloque destino le pasa a la IA sólo ese bloque, con tipos y opciones")
    void executeShouldSendOnlyDestinoBlockToProvider() {
        lote = new LoteTrabajo(LOTE_ID, NEGOCIO_ID, ProcesamientoTestData.BLOQUE_ID, lote.reclamoId(), 1);
        givenReadableLoteWithOtherBloque();
        givenReserva(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesar();

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(ProcesamientoTestData.BLOQUE_ID));
        assertThat(contexto.getValue()).extracting(BloqueConocido::nombre).containsExactly("Ventas");
        assertThat(contexto.getValue().getFirst().campos())
                .containsExactly(new CampoContexto("Monto", TipoCampo.NUMERO, List.of()));
    }

    @Test
    @DisplayName("Sin bloque destino le pasa a la IA todos los bloques del negocio")
    void executeShouldSendAllBloquesWhenNoDestino() {
        givenReadableLoteWithOtherBloque();
        givenReserva(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesar();

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(null));
        assertThat(contexto.getValue()).extracting(BloqueConocido::nombre).containsExactly("Ventas", "Gastos");
    }

    @Test
    @DisplayName("Le dice a la IA a qué tabla apunta cada campo de conexión")
    void executeShouldTellProviderWhichTableEachConexionPointsTo() {
        UUID clientesId = UUID.randomUUID();
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(
                new BloqueLectura(ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(
                        new CampoConocido(ProcesamientoTestData.CAMPO_ID, "Cliente", TipoCampo.RELACION,
                                List.of(), BloqueId.de(clientesId)),
                        new CampoConocido(CampoId.nuevo(), "Fiado", TipoCampo.RELACION, List.of(),
                                BloqueId.nuevo()))),
                new BloqueLectura(BloqueId.de(clientesId), "Clientes", List.of())));
        givenReserva(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesar();

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(null));
        assertThat(contexto.getValue().getFirst().campos()).containsExactly(
                new CampoContexto("Cliente", TipoCampo.RELACION, List.of(), "Clientes"),
                new CampoContexto("Fiado", TipoCampo.RELACION, List.of(), null));
    }

    @Test
    @DisplayName("Rechaza un lote sin fotos antes de usar archivos o IA")
    void executeShouldRejectEmptyPhotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of());

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).contains("entre 1 y 10");
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(archivoCuadernoPort, lectorCuadernoPort);
    }

    @Test
    @DisplayName("Rechaza más fotos que el máximo por lote")
    void executeShouldRejectTooManyPhotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(Collections.nCopies(11, FOTO));

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).contains("entre 1 y 10");
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("No invoca IA cuando la reserva diaria fue rechazada")
    void executeShouldRejectExhaustedQuota() {
        givenReadableLote();
        givenReserva(false);

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).isEqualTo(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("Conserva un fallo transitorio del lector para reintentar")
    void executeShouldMarkTransientFailure() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Timeout"));

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).isEqualTo("Timeout");
        assertThat(resultado.recuperable()).isTrue();
    }

    @Test
    @DisplayName("No reintenta un archivo inválido")
    void executeShouldMarkPermanentFailure() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave()))
                .thenThrow(new LecturaCuadernoException("Foto inválida", null, false));

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).isEqualTo("Foto inválida");
        assertThat(resultado.recuperable()).isFalse();
    }

    @Test
    @DisplayName("Oculta detalles técnicos de errores inesperados")
    void executeShouldHideUnexpectedFailure() {
        when(procesamientoRepository.fotos(lote)).thenThrow(new IllegalStateException("dato privado"));

        ResultadoIntento resultado = procesar();

        assertThat(resultado.error()).isEqualTo(ProcesarLote.ERROR_TEMPORAL);
        assertThat(resultado.recuperable()).isTrue();
    }

    @Test
    @DisplayName("Finaliza un resultado correcto con costo simulado y sin reintento")
    void executeShouldPersistSuccessfulResult() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(ProcesamientoTestData.lectura());
        givenReclamado(lote);

        procesarLote.execute();

        verify(procesamientoRepository).finalizar(eq(lote), any(), eq(BigDecimal.ZERO), eq(AHORA), isNull());
    }

    @Test
    @DisplayName("Programa un nuevo intento ante un error recuperable")
    void executeShouldRetryTransientError() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Timeout"));
        givenReclamado(lote);

        procesarLote.execute();

        verify(procesamientoRepository).finalizar(eq(lote), any(), isNull(), eq(AHORA), eq(AHORA.plusSeconds(30)));
    }

    @Test
    @DisplayName("Deja ERROR al alcanzar el máximo de intentos")
    void executeShouldStopWhenAttemptsExhausted() {
        lote = ProcesamientoTestData.lote(3);
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Timeout"));
        givenReclamado(lote);

        procesarLote.execute();

        verify(procesamientoRepository).finalizar(eq(lote), any(), isNull(), eq(AHORA), isNull());
    }

    @Test
    @DisplayName("Recupera reclamos vencidos usando la política de reintentos")
    void executeShouldFinalizeExpiredClaims() {
        when(procesamientoRepository.vencidos(AHORA)).thenReturn(List.of(lote));
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.empty());

        procesarLote.execute();

        ArgumentCaptor<ResultadoIntento> captor = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), captor.capture(), eq(null), eq(AHORA),
                eq(AHORA.plusSeconds(30)));
        assertThat(captor.getValue().error()).isEqualTo(ProcesarLote.RECLAMO_VENCIDO);
    }

    // --- arrange ---
    private void givenReadableLote() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(new BloqueLectura(
                ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(new CampoConocido(
                ProcesamientoTestData.CAMPO_ID, "Monto", TipoCampo.NUMERO, List.of())))));
    }

    private void givenReadableLoteWithOtherBloque() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(
                new BloqueLectura(ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(new CampoConocido(
                        ProcesamientoTestData.CAMPO_ID, "Monto", TipoCampo.NUMERO, List.of()))),
                new BloqueLectura(BloqueId.nuevo(), "Gastos", List.of())));
    }

    private void givenReservedLote() {
        givenReadableLote();
        givenReserva(true);
    }

    /** El control de plan admite el intento y delega la reserva diaria en el repositorio. */
    private void givenReserva(boolean entra) {
        when(controlPlanService.reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any()))
                .thenAnswer(invocacion -> invocacion.<BooleanSupplier>getArgument(3).getAsBoolean());
        when(procesamientoRepository.reservar(lote, 1, 100, AHORA)).thenReturn(entra);
    }

    private void givenReclamado(LoteTrabajo reclamado) {
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.of(reclamado));
    }

    // --- act ---
    private ResultadoIntento procesar() {
        givenReclamado(lote);
        procesarLote.execute();
        ArgumentCaptor<ResultadoIntento> resultado = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), resultado.capture(), any(), eq(AHORA), any());
        return resultado.getValue();
    }
}
