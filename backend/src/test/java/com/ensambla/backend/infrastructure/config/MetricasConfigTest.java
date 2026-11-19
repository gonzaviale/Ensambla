package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.InterpreteDePedidos;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.SEMANA;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDeLaSemana;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetricasConfigTest {

    private static final String UNA_BASE_INVALIDA = """
            {"base": [{"nombre": "Mal", "fuente": "GASTOS", "calculo": "UNIDADES"}]}
            """;

    private static final String UNA_DEL_KIOSCO_INVALIDA = """
            {"porRubro": {"KIOSCO": [{"nombre": "Mal", "fuente": "VENTAS", "calculo": "SUMA",
                                      "agrupar": "CATEGORIA"}]}}
            """;

    private static final String UNA_BASE_SIN_FIADO = """
            {"base": [{"nombre": "Ventas de la semana", "fuente": "VENTAS", "calculo": "SUMA",
                       "periodo": "SEMANA"}]}
            """;

    private static final String UNA_BASE_FIADA = """
            {"base": [{"nombre": "Fiado", "fuente": "VENTAS", "calculo": "SUMA", "periodo": "SEMANA",
                       "soloFiado": true}]}
            """;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final MetricasConfig config = new MetricasConfig();

    private CatalogoMetricas catalogo;

    private Throwable rechazo;

    @Test
    @DisplayName("Carga el catálogo con métricas para todos y para cada rubro del comercio")
    void cargaElCatalogoConMetricasParaTodosYParaCadaRubro() {
        cuandoSeCargaElCatalogoDeLaAplicacion();

        entoncesTieneMetricasParaTodosYParaLosRubrosDelComercio();
    }

    @Test
    @DisplayName("Crea el motor de métricas")
    void creaElMotorDeMetricas() {
        entoncesCreaElMotor();
    }

    @Test
    @DisplayName("Crea el intérprete de pedidos con el diccionario de la aplicación")
    void creaElInterpreteDePedidos() {
        entoncesCreaElInterprete();
    }

    @Test
    @DisplayName("Una receta del archivo que no dice si es fiada mira todas las ventas")
    void unaRecetaQueNoDiceSiEsFiadaMiraTodasLasVentas() {
        cuandoSeCarga(UNA_BASE_SIN_FIADO);

        entoncesLaBaseEs(lasVentasDeLaSemana());
    }

    @Test
    @DisplayName("Una receta del archivo marcada como fiada mira sólo las ventas fiadas")
    void unaRecetaMarcadaComoFiadaMiraSoloLasFiadas() {
        cuandoSeCarga(UNA_BASE_FIADA);

        entoncesLaBaseEs(unaReceta("Fiado", VENTAS, SUMA, NINGUNA, SEMANA, true));
    }

    @Test
    @DisplayName("No levanta si el catálogo no se puede leer")
    void noLevantaSiElCatalogoNoSePuedeLeer() {
        cuandoSeIntentaCargar(new ClassPathResource("metricas/no-existe.json"));

        entoncesNoLevantaPorque(IllegalStateException.class, "archivo de métricas");
    }

    @Test
    @DisplayName("No levanta si una métrica base del catálogo no es válida")
    void noLevantaSiUnaMetricaBaseNoEsValida() {
        cuandoSeIntentaCargar(recurso(UNA_BASE_INVALIDA));

        entoncesNoLevantaPorque(BusinessRuleException.class, "unidades");
    }

    @Test
    @DisplayName("No levanta si una métrica de un rubro no es válida")
    void noLevantaSiUnaMetricaDeUnRubroNoEsValida() {
        cuandoSeIntentaCargar(recurso(UNA_DEL_KIOSCO_INVALIDA));

        entoncesNoLevantaPorque(BusinessRuleException.class, "repartir");
    }

    private void cuandoSeCargaElCatalogoDeLaAplicacion() {
        catalogo = config.catalogoMetricas(objectMapper);
    }

    private void cuandoSeCarga(String json) {
        catalogo = MetricasConfig.cargar(recurso(json), objectMapper);
    }

    private void cuandoSeIntentaCargar(Resource recurso) {
        rechazo = catchThrowable(() -> MetricasConfig.cargar(recurso, objectMapper));
    }

    private void entoncesTieneMetricasParaTodosYParaLosRubrosDelComercio() {
        assertThat(catalogo.base()).isNotEmpty();
        assertThat(catalogo.porRubro()).containsOnlyKeys(RubroNegocio.ALMACEN, RubroNegocio.KIOSCO,
                RubroNegocio.MINIMERCADO, RubroNegocio.DIETETICA, RubroNegocio.PAPELERIA);
        assertThat(catalogo.paraRubro(RubroNegocio.OTRO)).isEqualTo(catalogo.base());
        assertThat(catalogo.paraRubro(RubroNegocio.KIOSCO)).hasSizeGreaterThan(catalogo.base().size());
    }

    private void entoncesCreaElMotor() {
        assertThat(config.motorMetricas()).isInstanceOf(MotorMetricas.class);
    }

    private void entoncesLaBaseEs(DefinicionMetrica receta) {
        assertThat(catalogo.base()).containsExactly(receta);
    }

    private void entoncesNoLevantaPorque(Class<? extends Throwable> tipo, String motivo) {
        assertThat(rechazo).isInstanceOf(tipo).hasMessageContaining(motivo);
    }

    private static ByteArrayResource recurso(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }

    private void entoncesCreaElInterprete() {
        assertThat(config.interpreteDePedidos(objectMapper)).isInstanceOf(InterpreteDePedidos.class);
    }
}
