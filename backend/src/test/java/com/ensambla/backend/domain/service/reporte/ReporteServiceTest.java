package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CLIENTES_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final UUID CANTIDAD_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID TOTAL_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final UUID CLIENTE_CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000003");
    private static final UUID NOMBRE_CLIENTE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000004");
    private static final UUID BETO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private CampoRepository campoRepository;

    @InjectMocks
    private ReporteService reporteService;

    private final FiltroReporte filtro = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, null, null);

    @Test
    @DisplayName("Genera las filas con las fórmulas calculadas y los totales de todas las que cumplen el filtro")
    void generarShouldReturnRowsWithFormulasAndTotalsOfAll() {
        Registro fila = Registro.builder().id(BETO_ID).valores(Map.of(CANTIDAD_ID.toString(), 4)).build();
        when(registroRepository.buscarFiltrados(filtro, 10)).thenReturn(List.of(fila));
        when(registroRepository.contarFiltrados(filtro)).thenReturn(3L);
        when(registroRepository.sumarNumericos(filtro, List.of(CANTIDAD_ID)))
                .thenReturn(Map.of(CANTIDAD_ID, new BigDecimal("9")));

        ReporteRegistros result = reporteService.generar(filtro, givenCampos(), 10);

        assertThat(result.filas()).singleElement().satisfies(calculada ->
                assertThat(calculada.getValores()).containsEntry(TOTAL_ID.toString(), new BigDecimal("8")));
        assertThat(result.totales().cantidad()).isEqualTo(3);
        assertThat(result.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("9");
        assertThat(result.truncado()).isFalse();
    }

    @Test
    @DisplayName("Avisa que está truncado cuando hay más filas que el límite")
    void generarShouldFlagTruncatedWhenThereAreMoreRowsThanTheLimit() {
        when(registroRepository.buscarFiltrados(filtro, 2)).thenReturn(List.of());
        when(registroRepository.contarFiltrados(filtro)).thenReturn(3L);
        when(registroRepository.sumarNumericos(filtro, List.of(CANTIDAD_ID))).thenReturn(Map.of());

        ReporteRegistros result = reporteService.generar(filtro, givenCampos(), 2);

        assertThat(result.truncado()).isTrue();
        assertThat(result.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Una cantidad igual al límite no está truncada")
    void generarShouldNotFlagTruncatedWhenCountEqualsTheLimit() {
        when(registroRepository.buscarFiltrados(filtro, 3)).thenReturn(List.of());
        when(registroRepository.contarFiltrados(filtro)).thenReturn(3L);
        when(registroRepository.sumarNumericos(filtro, List.of(CANTIDAD_ID))).thenReturn(Map.of());

        assertThat(reporteService.generar(filtro, givenCampos(), 3).truncado()).isFalse();
    }

    @Test
    @DisplayName("El nombre de las filas conectadas se pide una vez por tabla conectada")
    void etiquetasDeConexionesShouldQueryOncePerConnectedTable() {
        Campo cliente = Campo.builder().id(CLIENTE_CAMPO_ID).tipo(TipoCampo.RELACION)
                .bloqueRelacionadoId(CLIENTES_ID).build();
        List<Registro> filas = List.of(
                Registro.builder().valores(Map.of(CLIENTE_CAMPO_ID.toString(), BETO_ID.toString())).build(),
                Registro.builder().valores(Map.of(CLIENTE_CAMPO_ID.toString(), ANA_ID.toString())).build(),
                Registro.builder().valores(Map.of()).build());
        when(campoRepository.findByBloqueIdAndNegocioId(CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(NOMBRE_CLIENTE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build()));
        when(registroRepository.findAllByIdsAndBloque(Set.of(BETO_ID, ANA_ID), CLIENTES_ID, NEGOCIO_ID))
                .thenReturn(List.of(
                        Registro.builder().id(BETO_ID).valores(Map.of(NOMBRE_CLIENTE_ID.toString(), "Beto")).build(),
                        Registro.builder().id(ANA_ID).valores(Map.of(NOMBRE_CLIENTE_ID.toString(), "Ana")).build()));

        Map<UUID, String> result = reporteService.etiquetasDeConexiones(
                NEGOCIO_ID, List.of(cliente, givenCampos().get(0)), filas);

        assertThat(result).containsOnly(Map.entry(BETO_ID, "Beto"), Map.entry(ANA_ID, "Ana"));
    }

    @Test
    @DisplayName("Sin columnas de conexión no consulta nada")
    void etiquetasDeConexionesShouldNotQueryWithoutConnections() {
        Map<UUID, String> result = reporteService.etiquetasDeConexiones(NEGOCIO_ID, givenCampos(), List.of());

        assertThat(result).isEmpty();
        verify(registroRepository, never()).findAllByIdsAndBloque(any(), any(), any());
    }

    private static List<Campo> givenCampos() {
        return List.of(
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).posicion((short) 0).build(),
                Campo.builder().id(TOTAL_ID).tipo(TipoCampo.FORMULA).posicion((short) 1)
                        .formula("{" + CANTIDAD_ID + "} * 2").build());
    }
}
