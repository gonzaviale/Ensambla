package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlanTest {

    private Plan plan;
    private Throwable error;

    @Test
    @DisplayName("Permite el primer sistema cuando el límite es uno")
    void validarProyectosShouldAllowTheFirstOne() {
        givenUnLimiteDeProyectos(1);

        whenSeValidanLosProyectos(0);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza el siguiente sistema al alcanzar el límite")
    void validarProyectosShouldRejectTheNextOneAtTheLimit() {
        givenUnLimiteDeProyectos(1);

        whenSeValidanLosProyectos(1);

        thenSeRechazaConMensaje("1 sistemas");
    }

    @Test
    @DisplayName("Un límite nulo permite sistemas sin límite comercial")
    void validarProyectosShouldAllowUnlimitedConfiguredPlan() {
        givenUnLimiteDeProyectos(null);

        whenSeValidanLosProyectos(10000);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Permite un lote que entra completo en el límite de registros")
    void validarRegistrosShouldAllowABatchThatFits() {
        givenUnLimiteDeRegistros(1000L);

        whenSeValidanLosRegistros(998, 2);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza un lote que no entra completo en el límite de registros")
    void validarRegistrosShouldRejectABatchThatDoesNotFit() {
        givenUnLimiteDeRegistros(1000L);

        whenSeValidanLosRegistros(998, 3);

        thenSeRechazaConMensaje("1000 registros");
    }

    @Test
    @DisplayName("Un lote vacío no exige eliminar registros cuando el negocio ya superó su cuota")
    void validarRegistrosShouldAllowEmptyBatchAboveLimit() {
        givenUnLimiteDeRegistros(1000L);

        whenSeValidanLosRegistros(1500, 0);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Un lote con filas rechaza cuando el negocio ya superó su cuota")
    void validarRegistrosShouldRejectANonEmptyBatchAboveLimit() {
        givenUnLimiteDeRegistros(1000L);

        whenSeValidanLosRegistros(1500, 1);

        thenSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de IA incluye páginas procesadas y reservadas")
    void validarPaginasIaShouldAllowWhatFitsTheMonthlyAllowance() {
        givenUnLimiteDePaginasIa(10);

        whenSeValidanLasPaginasIa(9, 1);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de IA rechaza lo que la supera")
    void validarPaginasIaShouldRejectWhatExceedsTheMonthlyAllowance() {
        givenUnLimiteDePaginasIa(10);

        whenSeValidanLasPaginasIa(9, 2);

        thenSeRechazaConMensaje("10 análisis de foto por mes");
    }

    @Test
    @DisplayName("No habilita límites comerciales pendientes de configuración")
    void validarLimitesConfiguradosShouldFailClosedWhenFalse() {
        givenLimitesConfigurados(false);

        whenSeValidanLosLimitesConfigurados();

        thenSeRechazaConMensaje("Los límites del plan no están configurados");
    }

    @Test
    @DisplayName("No interpreta una configuración nula como un plan ilimitado")
    void validarLimitesConfiguradosShouldFailClosedWhenNull() {
        givenLimitesConfigurados(null);

        whenSeValidanLosLimitesConfigurados();

        thenSeRechaza();
    }

    @Test
    @DisplayName("Permite usar capacidades aprobadas aunque el precio siga pendiente")
    void validarLimitesConfiguradosShouldAllowCapabilitiesWithoutPrice() {
        givenElPrecioPendiente();

        whenSeValidanLosLimitesConfigurados();

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Permite una función incluida explícitamente en el catálogo")
    void validarFuncionShouldAllowAnEnabledFeature() {
        givenFunciones(FuncionPlan.RECETAS_COSTOS);

        whenSeValidaLaFuncion(FuncionPlan.RECETAS_COSTOS);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una función que el catálogo no incluye")
    void validarFuncionShouldRejectAFeatureThatIsNotIncluded() {
        givenFunciones(FuncionPlan.RECETAS_COSTOS);

        whenSeValidaLaFuncion(FuncionPlan.EXPORTAR_EXCEL);

        thenSeRechazaConMensaje("premium activo");
    }

    @Test
    @DisplayName("La cuota mensual de objetivos con IA deja guardar mientras queden")
    void validarObjetivosIaShouldAllowWhileThereAreLeft() {
        givenUnLimiteDeObjetivosIa(5);

        whenSeValidanLosObjetivosIa(4);

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de objetivos con IA rechaza al agotarse y explica cómo seguir")
    void validarObjetivosIaShouldRejectWhenTheAllowanceIsSpent() {
        givenUnLimiteDeObjetivosIa(5);

        whenSeValidanLosObjetivosIa(5);

        thenSeRechazaPorLimiteDeObjetivosIa();
    }

    @Test
    @DisplayName("Un plan sin límite de objetivos con IA nunca rechaza")
    void validarObjetivosIaShouldAllowEverythingWithoutLimit() {
        givenUnLimiteDeObjetivosIa(null);

        whenSeValidanLosObjetivosIa(1_000);

        thenNoSeRechaza();
    }

    // --- arrange ---
    private void givenUnLimiteDeProyectos(Integer limite) {
        plan = plan();
        plan.setLimiteProyectos(limite);
    }

    private void givenUnLimiteDeRegistros(Long limite) {
        plan = plan();
        plan.setLimiteRegistros(limite);
    }

    private void givenUnLimiteDePaginasIa(Integer limite) {
        plan = plan();
        plan.setLimitePaginasIaMensuales(limite);
    }

    private void givenLimitesConfigurados(Boolean configurados) {
        plan = plan();
        plan.setLimitesConfigurados(configurados);
    }

    private void givenElPrecioPendiente() {
        plan = plan();
        plan.setConfigurado(false);
    }

    private void givenFunciones(FuncionPlan... funciones) {
        plan = plan();
        plan.setFunciones(List.of(funciones).stream().map(Enum::name).toList());
    }

    private void givenUnLimiteDeObjetivosIa(Integer limite) {
        plan = plan();
        plan.setLimiteObjetivosIaMensuales(limite);
    }

    // --- act ---
    private void whenSeValidanLosProyectos(int existentes) {
        error = catchThrowable(() -> plan.validarProyectos(existentes));
    }

    private void whenSeValidanLosRegistros(int existentes, int nuevos) {
        error = catchThrowable(() -> plan.validarRegistros(existentes, nuevos));
    }

    private void whenSeValidanLasPaginasIa(int usadas, int nuevas) {
        error = catchThrowable(() -> plan.validarPaginasIa(usadas, nuevas));
    }

    private void whenSeValidanLosLimitesConfigurados() {
        error = catchThrowable(plan::validarLimitesConfigurados);
    }

    private void whenSeValidaLaFuncion(FuncionPlan funcion) {
        error = catchThrowable(() -> plan.validarFuncion(funcion));
    }

    private void whenSeValidanLosObjetivosIa(int usados) {
        error = catchThrowable(() -> plan.validarObjetivosIa(usados));
    }

    // --- assert ---
    private void thenNoSeRechaza() {
        assertThat(error).isNull();
    }

    private void thenSeRechaza() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void thenSeRechazaConMensaje(String fragmento) {
        assertThat(error).isInstanceOf(RestriccionPlanException.class).hasMessageContaining(fragmento);
    }

    private void thenSeRechazaPorLimiteDeObjetivosIa() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos con Sami")
                .hasMessageContaining("«Planes»");
        assertThat(((RestriccionPlanException) error).getCodigo()).isEqualTo("LIMITE_OBJETIVOS_IA");
    }
}
