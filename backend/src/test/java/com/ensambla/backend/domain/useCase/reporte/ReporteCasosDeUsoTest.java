package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporte;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporte;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import com.ensambla.backend.infrastructure.reporte.ExportadorReporteXlsxAdapter;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PAGADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId CLIENTE_CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId NOMBRE_CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000005");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId ANA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NegocioService negocioService;
    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private CampoRepository campoRepository;

    private GenerarReporte generarReporte;
    private ExportarReporte exportarReporte;

    @BeforeEach
    void setUp() {
        ReporteService reporteService = new ReporteService(registroRepository);
        generarReporte = new GenerarReporte(
                negocioService, controlPlanService, RELOJ, bloqueService, campoService, reporteService);
        exportarReporte = new ExportarReporte(
                negocioService, controlPlanService, bloqueService, campoService, reporteService, registroRepository,
                campoRepository, new ExportadorReporteXlsxAdapter(), RELOJ);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Bloque.builder().id(BLOQUE_ID).nombre("Pedidos").build());
        lenient().when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(givenCampos());
    }

    @Test
    @DisplayName("Generar devuelve las primeras 50 filas del filtro y los totales de todas")
    void generarShouldReturnFirstRowsAndTotalsOfTheFilter() {
        givenReporte(60, new BigDecimal("125.5"));

        GenerarReporteResponse response = generarReporte.execute(request(null));

        ArgumentCaptor<FiltroReporte> filtro = ArgumentCaptor.forClass(FiltroReporte.class);
        verify(registroRepository).buscarFiltrados(filtro.capture(), anyInt());
        assertThat(filtro.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(filtro.getValue().texto()).isEqualTo("pan");
        assertThat(filtro.getValue().desde()).isEqualTo(Instant.parse("2026-10-01T03:00:00Z"));
        assertThat(response.filas()).hasSize(2);
        assertThat(response.totales().cantidad()).isEqualTo(60);
        assertThat(response.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("125.5");
        assertThat(response.truncado()).isTrue();
        assertThat(response.maximoFilas()).isEqualTo(50);
    }

    @Test
    @DisplayName("Generar rechaza un filtro inválido antes de consultar las filas")
    void generarShouldRejectInvalidFilterBeforeQuerying() {
        GenerarReporteRequest alReves = new GenerarReporteRequest(
                USUARIO_ID, BLOQUE_ID, null, LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01"), null, null);

        assertThatThrownBy(() -> generarReporte.execute(alReves)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Generar responde 'no existe' si el bloque es de otro negocio")
    void generarShouldPropagateNotFound() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("no existe"));

        assertThatThrownBy(() -> generarReporte.execute(request(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Los totales del archivo descargado coinciden con los de la pantalla y con el filtro")
    void exportedTotalsShouldMatchTheScreenTotals() throws IOException {
        givenReporte(2, new BigDecimal("125.5"));

        GenerarReporteResponse enPantalla = generarReporte.execute(request(null));
        ExportarReporteResponse archivo = exportarReporte.execute(request(null));

        List<Row> filas = leer(archivo.contenido());
        Row totales = filas.get(filas.size() - 1);
        assertThat(archivo.nombreArchivo()).isEqualTo("reporte-pedidos-20261013.xlsx");
        assertThat(filas).hasSize(1 + enPantalla.filas().size() + 1);
        assertThat(totales.getCellText(0)).isEqualTo("Total (" + enPantalla.totales().cantidad() + " filas)");
        assertThat(totales.getCellAsNumber(1)).get()
                .isEqualTo(enPantalla.totales().sumas().get(CANTIDAD_ID).stripTrailingZeros());
        assertThat(filas.get(1).getCellText(0)).isEqualTo("Pan");
        assertThat(filas.get(1).getCellText(2)).isEqualTo("Sí");
    }

    @Test
    @DisplayName("No descarga un archivo incompleto cuando hay más filas que el máximo")
    void exportarShouldRejectReportsOverTheMaximum() {
        when(registroRepository.buscarFiltrados(any(), anyInt())).thenReturn(List.of());
        when(registroRepository.contarFiltrados(any())).thenReturn(10_001L);
        when(registroRepository.sumarNumericos(any(FiltroReporte.class), any())).thenReturn(Map.of());

        assertThatThrownBy(() -> exportarReporte.execute(request(null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10000 filas");
    }

    @Test
    @DisplayName("El archivo nombra las filas conectadas pidiendo una vez cada tabla conectada")
    void exportarShouldNameConnectedRowsQueryingOncePerTable() throws IOException {
        Campo cliente = Campo.builder().id(CLIENTE_CAMPO_ID).nombre("Cliente").tipo(TipoCampo.RELACION)
                .bloqueRelacionadoId(CLIENTES_ID).posicion((short) 3).build();
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(givenCampos().get(0), cliente));
        when(registroRepository.buscarFiltrados(any(), anyInt())).thenReturn(List.of(
                givenFilaDe(BETO_ID.valor()), givenFilaDe(ANA_ID.valor()),
                Registro.builder().id(RegistroId.nuevo()).creadoEn(OffsetDateTime.parse("2026-10-12T12:00:00Z"))
                        .valores(Map.of()).build()));
        when(registroRepository.contarFiltrados(any())).thenReturn(3L);
        when(registroRepository.sumarNumericos(any(FiltroReporte.class), any())).thenReturn(Map.of());
        when(campoRepository.findByBloqueIdAndNegocioId(CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(NOMBRE_CLIENTE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build()));
        when(registroRepository.findAllByIdsAndBloque(Set.of(BETO_ID, ANA_ID), CLIENTES_ID, NEGOCIO_ID))
                .thenReturn(List.of(
                        Registro.builder().id(BETO_ID).valores(Map.of(NOMBRE_CLIENTE_ID.toString(), "Beto")).build(),
                        Registro.builder().id(ANA_ID).valores(Map.of(NOMBRE_CLIENTE_ID.toString(), "Ana")).build()));

        ExportarReporteResponse archivo = exportarReporte.execute(request(null));

        List<Row> filas = leer(archivo.contenido());
        assertThat(filas.get(1).getCellText(1)).isEqualTo("Beto");
        assertThat(filas.get(2).getCellText(1)).isEqualTo("Ana");
        verify(registroRepository).findAllByIdsAndBloque(Set.of(BETO_ID, ANA_ID), CLIENTES_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Sin columnas de conexión el archivo no consulta otras tablas")
    void exportarShouldNotQueryOtherTablesWithoutConnections() {
        givenReporte(2, new BigDecimal("125.5"));

        exportarReporte.execute(request(null));

        verify(registroRepository, never()).findAllByIdsAndBloque(any(), any(), any());
    }

    private static Registro givenFilaDe(UUID clienteId) {
        return Registro.builder().id(RegistroId.nuevo()).creadoEn(OffsetDateTime.parse("2026-10-12T12:00:00Z"))
                .valores(Map.of(CLIENTE_CAMPO_ID.toString(), clienteId.toString())).build();
    }

    private void givenReporte(long cantidad, BigDecimal suma) {
        List<Registro> filas = List.of(
                Registro.builder().id(RegistroId.nuevo()).creadoEn(OffsetDateTime.parse("2026-10-12T12:00:00Z"))
                        .valores(Map.of(NOMBRE_ID.toString(), "Pan", CANTIDAD_ID.toString(), 2.5,
                                PAGADO_ID.toString(), true)).build(),
                Registro.builder().id(RegistroId.nuevo()).creadoEn(OffsetDateTime.parse("2026-10-11T12:00:00Z"))
                        .valores(Map.of(NOMBRE_ID.toString(), "Torta", CANTIDAD_ID.toString(), 123,
                                PAGADO_ID.toString(), false)).build());
        when(registroRepository.buscarFiltrados(any(), anyInt())).thenReturn(filas);
        when(registroRepository.contarFiltrados(any())).thenReturn(cantidad);
        when(registroRepository.sumarNumericos(any(FiltroReporte.class), any()))
                .thenReturn(Map.of(CANTIDAD_ID, suma));
    }

    private static GenerarReporteRequest request(CampoId campoId) {
        return new GenerarReporteRequest(
                USUARIO_ID, BLOQUE_ID, "pan", LocalDate.parse("2026-10-01"), null, campoId, null);
    }

    private static List<Campo> givenCampos() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).nombre("Nombre").tipo(TipoCampo.TEXTO).posicion((short) 0).build(),
                Campo.builder().id(CANTIDAD_ID).nombre("Cantidad").tipo(TipoCampo.NUMERO).posicion((short) 1).build(),
                Campo.builder().id(PAGADO_ID).nombre("Pagado").tipo(TipoCampo.SI_NO).posicion((short) 2).build());
    }

    private static List<Row> leer(byte[] contenido) throws IOException {
        try (ReadableWorkbook libro = new ReadableWorkbook(new ByteArrayInputStream(contenido))) {
            return libro.getFirstSheet().read();
        }
    }
}
