package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId TOTAL_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Mock
    private RegistroRepository registroRepository;

    @InjectMocks
    private ReporteService reporteService;

    private final FiltroReporte filtro = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, null, null);

    private ReporteRegistros reporte;

    @Test
    @DisplayName("Genera las filas con las fórmulas calculadas y los totales de todas las que cumplen el filtro")
    void generaLasFilasConLasFormulasCalculadasYLosTotalesDeTodasLasQueCumplenElFiltro() {
        dadoUnaFilaDeCuatroUnidadesYTresFilasQueCumplenElFiltro();

        cuandoSeGeneraElReporteConLimiteDe(10);

        entoncesLaFilaTraeLaFormulaCalculadaYLosTotalesSonDeTodas();
    }

    @Test
    @DisplayName("Avisa que está truncado cuando hay más filas que el límite")
    void avisaQueEstaTruncadoCuandoHayMasFilasQueElLimite() {
        dadoQueHayTresFilasQueCumplenElFiltroYNingunaSeLee(2);

        cuandoSeGeneraElReporteConLimiteDe(2);

        entoncesElReporteEstaTruncadoYSumaCero();
    }

    @Test
    @DisplayName("Una cantidad igual al límite no está truncada")
    void unaCantidadIgualAlLimiteNoEstaTruncada() {
        dadoQueHayTresFilasQueCumplenElFiltroYNingunaSeLee(3);

        cuandoSeGeneraElReporteConLimiteDe(3);

        entoncesElReporteNoEstaTruncado();
    }

    // --- dado ---

    private void dadoUnaFilaDeCuatroUnidadesYTresFilasQueCumplenElFiltro() {
        Registro fila = Registro.builder().id(BETO_ID).valores(Map.of(CANTIDAD_ID.toString(), 4)).build();
        when(registroRepository.buscarFiltrados(filtro, 10)).thenReturn(List.of(fila));
        when(registroRepository.contarFiltrados(filtro)).thenReturn(3L);
        when(registroRepository.sumarNumericos(filtro, List.of(CANTIDAD_ID)))
                .thenReturn(Map.of(CANTIDAD_ID, new BigDecimal("9")));
    }

    private void dadoQueHayTresFilasQueCumplenElFiltroYNingunaSeLee(int limite) {
        when(registroRepository.buscarFiltrados(filtro, limite)).thenReturn(List.of());
        when(registroRepository.contarFiltrados(filtro)).thenReturn(3L);
        when(registroRepository.sumarNumericos(filtro, List.of(CANTIDAD_ID))).thenReturn(Map.of());
    }

    // --- cuando ---

    private void cuandoSeGeneraElReporteConLimiteDe(int limite) {
        reporte = reporteService.generar(filtro, unosCampos(), limite);
    }

    // --- entonces ---

    private void entoncesLaFilaTraeLaFormulaCalculadaYLosTotalesSonDeTodas() {
        assertThat(reporte.filas()).singleElement().satisfies(calculada ->
                assertThat(calculada.getValores()).containsEntry(TOTAL_ID.toString(), new BigDecimal("8")));
        assertThat(reporte.totales().cantidad()).isEqualTo(3);
        assertThat(reporte.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("9");
        assertThat(reporte.truncado()).isFalse();
    }

    private void entoncesElReporteEstaTruncadoYSumaCero() {
        assertThat(reporte.truncado()).isTrue();
        assertThat(reporte.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("0");
    }

    private void entoncesElReporteNoEstaTruncado() {
        assertThat(reporte.truncado()).isFalse();
    }

    // --- datos ---

    private static List<Campo> unosCampos() {
        return List.of(
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).posicion((short) 0).build(),
                Campo.builder().id(TOTAL_ID).tipo(TipoCampo.FORMULA).posicion((short) 1)
                        .formula("{" + CANTIDAD_ID + "} * 2").build());
    }
}
