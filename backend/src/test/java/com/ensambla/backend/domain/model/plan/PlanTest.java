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
    void permiteElPrimerSistemaCuandoElLimiteEsUno() {
        dadoUnLimiteDeProyectos(1);

        cuandoSeValidanLosProyectos(0);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza el siguiente sistema al alcanzar el límite")
    void rechazaElSiguienteSistemaAlAlcanzarElLimite() {
        dadoUnLimiteDeProyectos(1);

        cuandoSeValidanLosProyectos(1);

        entoncesSeRechazaConMensaje("1 sistemas");
    }

    @Test
    @DisplayName("Un límite nulo permite sistemas sin límite comercial")
    void unLimiteNuloPermiteSistemasSinLimiteComercial() {
        dadoUnLimiteDeProyectos(null);

        cuandoSeValidanLosProyectos(10000);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Permite un lote que entra completo en el límite de registros")
    void permiteUnLoteQueEntraCompletoEnElLimiteDeRegistros() {
        dadoUnLimiteDeRegistros(1000L);

        cuandoSeValidanLosRegistros(998, 2);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza un lote que no entra completo en el límite de registros")
    void rechazaUnLoteQueNoEntraCompletoEnElLimiteDeRegistros() {
        dadoUnLimiteDeRegistros(1000L);

        cuandoSeValidanLosRegistros(998, 3);

        entoncesSeRechazaConMensaje("1000 registros");
    }

    @Test
    @DisplayName("Un lote vacío no exige eliminar registros cuando el negocio ya superó su cuota")
    void unLoteVacioNoExigeEliminarRegistrosCuandoElNegocioYaSuperoSuCuota() {
        dadoUnLimiteDeRegistros(1000L);

        cuandoSeValidanLosRegistros(1500, 0);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Un lote con filas rechaza cuando el negocio ya superó su cuota")
    void unLoteConFilasRechazaCuandoElNegocioYaSuperoSuCuota() {
        dadoUnLimiteDeRegistros(1000L);

        cuandoSeValidanLosRegistros(1500, 1);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de IA incluye páginas procesadas y reservadas")
    void laCuotaMensualDeIaIncluyePaginasProcesadasYReservadas() {
        dadoUnLimiteDePaginasIa(10);

        cuandoSeValidanLasPaginasIa(9, 1);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de IA rechaza lo que la supera")
    void laCuotaMensualDeIaRechazaLoQueLaSupera() {
        dadoUnLimiteDePaginasIa(10);

        cuandoSeValidanLasPaginasIa(9, 2);

        entoncesSeRechazaConMensaje("10 análisis de foto por mes");
    }

    @Test
    @DisplayName("No habilita límites comerciales pendientes de configuración")
    void noHabilitaLimitesComercialesPendientesDeConfiguracion() {
        dadoLimitesConfigurados(false);

        cuandoSeValidanLosLimitesConfigurados();

        entoncesSeRechazaConMensaje("Los límites del plan no están configurados");
    }

    @Test
    @DisplayName("No interpreta una configuración nula como un plan ilimitado")
    void noInterpretaUnaConfiguracionNulaComoUnPlanIlimitado() {
        dadoLimitesConfigurados(null);

        cuandoSeValidanLosLimitesConfigurados();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Permite usar capacidades aprobadas aunque el precio siga pendiente")
    void permiteUsarCapacidadesAprobadasAunqueElPrecioSigaPendiente() {
        dadoElPrecioPendiente();

        cuandoSeValidanLosLimitesConfigurados();

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Permite una función incluida explícitamente en el catálogo")
    void permiteUnaFuncionIncluidaExplicitamenteEnElCatalogo() {
        dadoFunciones(FuncionPlan.RECETAS_COSTOS);

        cuandoSeValidaLaFuncion(FuncionPlan.RECETAS_COSTOS);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una función que el catálogo no incluye")
    void rechazaUnaFuncionQueElCatalogoNoIncluye() {
        dadoFunciones(FuncionPlan.RECETAS_COSTOS);

        cuandoSeValidaLaFuncion(FuncionPlan.EXPORTAR_EXCEL);

        entoncesSeRechazaConMensaje("premium activo");
    }

    @Test
    @DisplayName("La cuota mensual de objetivos con IA deja guardar mientras queden")
    void laCuotaMensualDeObjetivosConIaDejaGuardarMientrasQueden() {
        dadoUnLimiteDeObjetivosIa(5);

        cuandoSeValidanLosObjetivosIa(4);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("La cuota mensual de objetivos con IA rechaza al agotarse y explica cómo seguir")
    void laCuotaMensualDeObjetivosConIaRechazaAlAgotarseYExplicaComoSeguir() {
        dadoUnLimiteDeObjetivosIa(5);

        cuandoSeValidanLosObjetivosIa(5);

        entoncesSeRechazaPorLimiteDeObjetivosIa();
    }

    @Test
    @DisplayName("Un plan sin límite de objetivos con IA nunca rechaza")
    void unPlanSinLimiteDeObjetivosConIaNuncaRechaza() {
        dadoUnLimiteDeObjetivosIa(null);

        cuandoSeValidanLosObjetivosIa(1_000);

        entoncesNoSeRechaza();
    }

    // --- arrange ---
    private void dadoUnLimiteDeProyectos(Integer limite) {
        plan = plan();
        plan.setLimiteProyectos(limite);
    }

    private void dadoUnLimiteDeRegistros(Long limite) {
        plan = plan();
        plan.setLimiteRegistros(limite);
    }

    private void dadoUnLimiteDePaginasIa(Integer limite) {
        plan = plan();
        plan.setLimitePaginasIaMensuales(limite);
    }

    private void dadoLimitesConfigurados(Boolean configurados) {
        plan = plan();
        plan.setLimitesConfigurados(configurados);
    }

    private void dadoElPrecioPendiente() {
        plan = plan();
        plan.setConfigurado(false);
    }

    private void dadoFunciones(FuncionPlan... funciones) {
        plan = plan();
        plan.setFunciones(List.of(funciones).stream().map(Enum::name).toList());
    }

    private void dadoUnLimiteDeObjetivosIa(Integer limite) {
        plan = plan();
        plan.setLimiteObjetivosIaMensuales(limite);
    }

    // --- act ---
    private void cuandoSeValidanLosProyectos(int existentes) {
        error = catchThrowable(() -> plan.validarProyectos(existentes));
    }

    private void cuandoSeValidanLosRegistros(int existentes, int nuevos) {
        error = catchThrowable(() -> plan.validarRegistros(existentes, nuevos));
    }

    private void cuandoSeValidanLasPaginasIa(int usadas, int nuevas) {
        error = catchThrowable(() -> plan.validarPaginasIa(usadas, nuevas));
    }

    private void cuandoSeValidanLosLimitesConfigurados() {
        error = catchThrowable(plan::validarLimitesConfigurados);
    }

    private void cuandoSeValidaLaFuncion(FuncionPlan funcion) {
        error = catchThrowable(() -> plan.validarFuncion(funcion));
    }

    private void cuandoSeValidanLosObjetivosIa(int usados) {
        error = catchThrowable(() -> plan.validarObjetivosIa(usados));
    }

    // --- assert ---
    private void entoncesNoSeRechaza() {
        assertThat(error).isNull();
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesSeRechazaConMensaje(String fragmento) {
        assertThat(error).isInstanceOf(RestriccionPlanException.class).hasMessageContaining(fragmento);
    }

    private void entoncesSeRechazaPorLimiteDeObjetivosIa() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos con Sami")
                .hasMessageContaining("«Planes»");
        assertThat(((RestriccionPlanException) error).getCodigo()).isEqualTo("LIMITE_OBJETIVOS_IA");
    }
}
