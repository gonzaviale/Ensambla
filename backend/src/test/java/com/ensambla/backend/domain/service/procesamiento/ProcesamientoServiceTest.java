package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesamientoServiceTest {

    private static final FotoLectura FOTO = new FotoLectura("foto.jpg", "image/jpeg");

    @Mock
    private ProcesamientoRepository procesamientoRepository;
    @Mock
    private ReservaIaService reservaIaService;

    @Mock
    private ArchivoCuadernoPort archivoCuadernoPort;

    @Mock
    private LectorCuadernoPort lectorCuadernoPort;

    @Mock
    private ConexionesLecturaService conexionesLecturaService;

    private ProcesamientoService procesamientoService;
    private LoteTrabajo lote;

    @BeforeEach
    void setUp() {
        lote = ProcesamientoTestData.lote(1);
        procesamientoService = new ProcesamientoService(procesamientoRepository, archivoCuadernoPort,
                lectorCuadernoPort, new PropuestaLecturaService(), conexionesLecturaService,
                ProcesamientoTestData.politica(), reservaIaService);
    }

    @Test
    @DisplayName("Reclama trabajo con la vigencia configurada")
    void reclamarShouldUseConfiguredLease() {
        when(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).thenReturn(Optional.of(lote));

        Optional<LoteTrabajo> resultado = procesamientoService.reclamar(AHORA);

        assertThat(resultado).contains(lote);
    }

    @Test
    @DisplayName("Lee archivos, reserva consumo y llama al proveedor en ese orden")
    void leerShouldReadAndReserveBeforeProvider() {
        givenReadableLote();
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(ProcesamientoTestData.lectura());
        when(conexionesLecturaService.resolver(eq(NEGOCIO_ID), any(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.propuesta().lectura().tipoRegistro())
                .isEqualTo(ProcesamientoTestData.lectura().tipoRegistro());
        assertThat(resultado.error()).isNull();
        assertThat(resultado.recuperable()).isFalse();
        var orden = inOrder(archivoCuadernoPort, procesamientoRepository, reservaIaService, lectorCuadernoPort);
        orden.verify(archivoCuadernoPort).leer(NEGOCIO_ID, FOTO.clave());
        orden.verify(reservaIaService).reservar(lote, 1, 100, AHORA);
        orden.verify(lectorCuadernoPort).leer(any(), any(), any());
    }

    @Test
    @DisplayName("Con bloque destino le pasa a la IA sólo ese bloque, con tipos y opciones")
    void leerShouldSendOnlyDestinoBlockToProvider() {
        lote = new LoteTrabajo(LOTE_ID, NEGOCIO_ID, ProcesamientoTestData.BLOQUE_ID, lote.reclamoId(), 1);
        givenReadableLoteWithOtherBloque();
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesamientoService.leer(lote, AHORA);

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(ProcesamientoTestData.BLOQUE_ID));
        assertThat(contexto.getValue()).extracting(BloqueConocido::nombre).containsExactly("Ventas");
        assertThat(contexto.getValue().getFirst().campos())
                .containsExactly(new CampoContexto("Monto", TipoCampo.NUMERO, List.of()));
    }

    @Test
    @DisplayName("Sin bloque destino le pasa a la IA todos los bloques del negocio")
    void leerShouldSendAllBloquesWhenNoDestino() {
        givenReadableLoteWithOtherBloque();
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesamientoService.leer(lote, AHORA);

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(null));
        assertThat(contexto.getValue()).extracting(BloqueConocido::nombre).containsExactly("Ventas", "Gastos");
    }

    @Test
    @DisplayName("Devuelve la propuesta con las conexiones ya resueltas a filas")
    void leerShouldReturnPropuestaWithResolvedConexiones() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(ProcesamientoTestData.lectura());
        PropuestaLectura resuelta = new PropuestaLectura(ProcesamientoTestData.lectura(), Map.of());
        when(conexionesLecturaService.resolver(eq(NEGOCIO_ID), any(), any())).thenReturn(resuelta);

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.propuesta()).isSameAs(resuelta);
    }

    @Test
    @DisplayName("Le dice a la IA a qué tabla apunta cada campo de conexión")
    void leerShouldTellProviderWhichTableEachConexionPointsTo() {
        UUID clientesId = UUID.randomUUID();
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(List.of(
                new BloqueLectura(ProcesamientoTestData.BLOQUE_ID, "Ventas", List.of(
                        new CampoConocido(ProcesamientoTestData.CAMPO_ID, "Cliente", TipoCampo.RELACION,
                                List.of(), clientesId),
                        new CampoConocido(UUID.randomUUID(), "Fiado", TipoCampo.RELACION, List.of(),
                                UUID.randomUUID()))),
                new BloqueLectura(clientesId, "Clientes", List.of())));
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Corte"));

        procesamientoService.leer(lote, AHORA);

        ArgumentCaptor<List<BloqueConocido>> contexto = ArgumentCaptor.forClass(List.class);
        verify(lectorCuadernoPort).leer(any(), contexto.capture(), eq(null));
        assertThat(contexto.getValue().getFirst().campos()).containsExactly(
                new CampoContexto("Cliente", TipoCampo.RELACION, List.of(), "Clientes"),
                new CampoContexto("Fiado", TipoCampo.RELACION, List.of(), null));
    }

    @Test
    @DisplayName("Rechaza un lote sin fotos antes de usar archivos o IA")
    void leerShouldRejectEmptyPhotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of());

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).contains("entre 1 y 10");
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(archivoCuadernoPort, lectorCuadernoPort);
    }

    @Test
    @DisplayName("Rechaza más fotos que el máximo por lote")
    void leerShouldRejectTooManyPhotos() {
        when(procesamientoRepository.fotos(lote)).thenReturn(Collections.nCopies(11, FOTO));

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).contains("entre 1 y 10");
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("No invoca IA cuando la reserva diaria fue rechazada")
    void leerShouldRejectExhaustedQuota() {
        givenReadableLote();
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(false);

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).isEqualTo(ProcesamientoService.LIMITE_AGOTADO);
        assertThat(resultado.recuperable()).isFalse();
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("Conserva un fallo transitorio del lector para reintentar")
    void leerShouldMarkTransientFailure() {
        givenReservedLote();
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Timeout"));

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).isEqualTo("Timeout");
        assertThat(resultado.recuperable()).isTrue();
    }

    @Test
    @DisplayName("No reintenta un archivo inválido")
    void leerShouldMarkPermanentFailure() {
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave()))
                .thenThrow(new LecturaCuadernoException("Foto inválida", null, false));

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).isEqualTo("Foto inválida");
        assertThat(resultado.recuperable()).isFalse();
    }

    @Test
    @DisplayName("Oculta detalles técnicos de errores inesperados")
    void leerShouldHideUnexpectedFailure() {
        when(procesamientoRepository.fotos(lote)).thenThrow(new IllegalStateException("dato privado"));

        ResultadoIntento resultado = procesamientoService.leer(lote, AHORA);

        assertThat(resultado.error()).isEqualTo(ProcesamientoService.ERROR_TEMPORAL);
        assertThat(resultado.recuperable()).isTrue();
    }

    @Test
    @DisplayName("Finaliza un resultado correcto con costo simulado y sin reintento")
    void finalizarShouldPersistSuccessfulResult() {
        ResultadoIntento resultado = new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false);

        procesamientoService.finalizar(lote, resultado, AHORA);

        verify(procesamientoRepository).finalizar(lote, resultado, BigDecimal.ZERO, AHORA, null);
    }

    @Test
    @DisplayName("Programa un nuevo intento ante un error recuperable")
    void finalizarShouldRetryTransientError() {
        ResultadoIntento resultado = new ResultadoIntento(null, "Timeout", true);

        procesamientoService.finalizar(lote, resultado, AHORA);

        verify(procesamientoRepository).finalizar(lote, resultado, null, AHORA, AHORA.plusSeconds(30));
    }

    @Test
    @DisplayName("Deja ERROR al alcanzar el máximo de intentos")
    void finalizarShouldStopWhenAttemptsExhausted() {
        LoteTrabajo ultimo = ProcesamientoTestData.lote(3);
        ResultadoIntento resultado = new ResultadoIntento(null, "Timeout", true);

        procesamientoService.finalizar(ultimo, resultado, AHORA);

        verify(procesamientoRepository).finalizar(ultimo, resultado, null, AHORA, null);
    }

    @Test
    @DisplayName("Recupera reclamos vencidos usando la política de reintentos")
    void recuperarShouldFinalizeExpiredClaims() {
        when(procesamientoRepository.vencidos(AHORA)).thenReturn(List.of(lote));

        procesamientoService.recuperar(AHORA);

        ArgumentCaptor<ResultadoIntento> captor = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), captor.capture(), eq(null), eq(AHORA),
                eq(AHORA.plusSeconds(30)));
        assertThat(captor.getValue().error()).isEqualTo(ProcesamientoService.RECLAMO_VENCIDO);
    }

    @Test
    @DisplayName("Devuelve el estado del lote del negocio")
    void obtenerShouldReturnOwnLote() {
        EstadoLectura estado = givenEstado();
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(estado));

        EstadoLectura resultado = procesamientoService.obtener(NEGOCIO_ID, LOTE_ID);

        assertThat(resultado).isEqualTo(estado);
    }

    @Test
    @DisplayName("Un lote ajeno o inexistente responde como no encontrado")
    void obtenerShouldRejectMissingLote() {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> procesamientoService.obtener(NEGOCIO_ID, LOTE_ID))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Lote no encontrado");
    }

    @Test
    @DisplayName("Consulta el consumo del día del negocio")
    void consumoShouldUseCurrentDate() {
        ConsumoDiario esperado = givenConsumo(5, 2);
        when(procesamientoRepository.consumo(NEGOCIO_ID, AHORA.toLocalDate())).thenReturn(esperado);

        ConsumoDiario resultado = procesamientoService.consumo(NEGOCIO_ID, AHORA);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Reencola un lote propio cuando hay disponibilidad")
    void reintentarShouldRequeueOwnError() {
        givenRetry(5, 2);
        when(procesamientoRepository.reencolar(NEGOCIO_ID, LOTE_ID, AHORA)).thenReturn(true);

        procesamientoService.reintentar(NEGOCIO_ID, LOTE_ID, AHORA);

        verify(procesamientoRepository).reencolar(NEGOCIO_ID, LOTE_ID, AHORA);
    }

    @Test
    @DisplayName("Rechaza reintentar si no queda disponibilidad")
    void reintentarShouldRejectExhaustedQuota() {
        givenRetry(90, 10);

        assertThatThrownBy(() -> procesamientoService.reintentar(NEGOCIO_ID, LOTE_ID, AHORA))
                .isInstanceOf(BusinessRuleException.class).hasMessage(ProcesamientoService.LIMITE_AGOTADO);
        verify(procesamientoRepository, never()).reencolar(any(), any(), any());
    }

    @Test
    @DisplayName("Rechaza reintentar un estado distinto a ERROR")
    void reintentarShouldRejectWrongState() {
        givenRetry(0, 0);
        when(procesamientoRepository.reencolar(NEGOCIO_ID, LOTE_ID, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> procesamientoService.reintentar(NEGOCIO_ID, LOTE_ID, AHORA))
                .hasMessageContaining("estado ERROR");
    }

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
                new BloqueLectura(UUID.randomUUID(), "Gastos", List.of())));
    }

    private void givenReservedLote() {
        givenReadableLote();
        when(reservaIaService.reservar(lote, 1, 100, AHORA)).thenReturn(true);
    }

    private void givenRetry(int paginas, int reservadas) {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(givenEstado()));
        when(procesamientoRepository.consumo(NEGOCIO_ID, AHORA.toLocalDate()))
                .thenReturn(givenConsumo(paginas, reservadas));
    }

    private EstadoLectura givenEstado() {
        return new EstadoLectura(LOTE_ID, EstadoLote.ERROR, "Error", 3, null);
    }

    private ConsumoDiario givenConsumo(int paginas, int reservadas) {
        return new ConsumoDiario(AHORA.toLocalDate(), paginas, reservadas, 0, 0, null, false);
    }
}
