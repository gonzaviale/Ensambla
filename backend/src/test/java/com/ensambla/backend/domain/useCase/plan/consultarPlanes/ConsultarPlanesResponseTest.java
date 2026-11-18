package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultarPlanesResponseTest {

    private ConsultaPlanes consulta;
    private ConsultarPlanesResponse respuesta;

    @Test
    @DisplayName("Mapea catálogo completo, suscripción efectiva, uso e historial de prueba")
    void mapeaCatalogoCompletoSuscripcionEfectivaUsoEHistorialDePrueba() {
        dadoUnaConsultaConHistorialDePrueba();

        cuandoSeMapeaLaConsulta();

        entoncesSeMapeanElCatalogoYLaSuscripcion();
        entoncesSeMapeaElUso();
        entoncesSeMapeaElHistorialDePrueba();
    }

    @Test
    @DisplayName("Un negocio que nunca inició prueba conserva historial nulo y elegibilidad")
    void unNegocioQueNuncaInicioPruebaConservaHistorialNuloYElegibilidad() {
        dadoUnaConsultaSinHistorialDePrueba();

        cuandoSeMapeaLaConsulta();

        entoncesNoHayHistorialYPuedeIniciarPrueba();
    }

    // --- arrange ---
    private void dadoUnaConsultaConHistorialDePrueba() {
        consulta = consulta(true);
    }

    private void dadoUnaConsultaSinHistorialDePrueba() {
        consulta = consulta(false);
    }

    // --- act ---
    private void cuandoSeMapeaLaConsulta() {
        respuesta = ConsultarPlanesResponse.de(consulta, AHORA);
    }

    // --- assert ---
    private void entoncesSeMapeanElCatalogoYLaSuscripcion() {
        assertThat(respuesta.consultadoEn()).isEqualTo(AHORA);
        assertThat(respuesta.planes()).hasSize(2);
        IntStream.range(0, consulta.planes().size())
                .forEach(indice -> entoncesPlanMatches(respuesta.planes().get(indice), consulta.planes().get(indice)));
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.suscripcion().inicioEn()).isEqualTo(consulta.suscripcion().getInicioEn());
        assertThat(respuesta.suscripcion().finEn()).isEqualTo(consulta.suscripcion().getFinEn());
    }

    private void entoncesSeMapeaElUso() {
        assertThat(respuesta.uso().proyectos()).isEqualTo(2);
        assertThat(respuesta.uso().registros()).isEqualTo(89);
        assertThat(respuesta.uso().paginasIa()).isEqualTo(6);
        assertThat(respuesta.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(respuesta.uso().periodoIa()).isEqualTo("MENSUAL");
        assertThat(respuesta.uso().objetivosIa()).isEqualTo(3);
        assertThat(respuesta.uso().limiteObjetivosIa()).isEqualTo(5);
        assertThat(respuesta.planes()).extracting(PlanItem::limiteObjetivosIaMensuales).containsExactly(5, 20);
        assertThat(respuesta.uso().inicioPeriodoIa()).isEqualTo(consulta.inicioPeriodoIa());
        assertThat(respuesta.uso().finPeriodoIa()).isEqualTo(consulta.finPeriodoIa());
    }

    private void entoncesSeMapeaElHistorialDePrueba() {
        assertThat(respuesta.prueba().inicioEn()).isEqualTo(consulta.prueba().inicioEn());
        assertThat(respuesta.prueba().finEn()).isEqualTo(consulta.prueba().finEn());
        assertThat(respuesta.prueba().paginasIaUtilizadas()).isEqualTo(8);
        assertThat(respuesta.prueba().limitePaginasIa()).isEqualTo(10);
    }

    private void entoncesNoHayHistorialYPuedeIniciarPrueba() {
        assertThat(respuesta.prueba()).isNull();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    private void entoncesPlanMatches(PlanItem actual, Plan esperado) {
        assertThat(actual.codigo()).isEqualTo(esperado.getCodigo().name());
        assertThat(actual.nombre()).isEqualTo(esperado.getNombre());
        assertThat(actual.precioConfigurado()).isEqualTo(esperado.getConfigurado());
        assertThat(actual.limitesConfigurados()).isEqualTo(esperado.getLimitesConfigurados());
        assertThat(actual.precioMensual()).isEqualTo(esperado.getPrecioMensual());
        assertThat(actual.moneda()).isEqualTo(esperado.getMoneda());
        assertThat(actual.limiteProyectos()).isEqualTo(esperado.getLimiteProyectos());
        assertThat(actual.limiteRegistros()).isEqualTo(esperado.getLimiteRegistros());
        assertThat(actual.limitePaginasIaMensuales()).isEqualTo(esperado.getLimitePaginasIaMensuales());
        assertThat(actual.funciones()).isEqualTo(esperado.getFunciones());
    }
}
