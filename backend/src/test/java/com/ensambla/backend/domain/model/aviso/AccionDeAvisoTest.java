package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AccionDeAvisoTest {

    private final AplicadorQueAnota aplicador = new AplicadorQueAnota();

    private AccionDeAviso accion;
    private List<AccionDeAviso> atajos;
    private Throwable error;

    @Test
    @DisplayName("Editar una fila cambia datos y le pide al aplicador que la edite")
    void editarUnaFilaCambiaDatosYLePideAlAplicadorQueLaEdite() {
        dadoUnaAccionDeEditarElPrecioDeUnaFila();

        cuandoSeAplicaLaAccion();

        entoncesCambiaDatosYElAplicadorAnota("editar");
    }

    @Test
    @DisplayName("Crear una fila cambia datos y le pide al aplicador que la cree")
    void crearUnaFilaCambiaDatosYLePideAlAplicadorQueLaCree() {
        dadoUnaAccionDeCrearUnCombo();

        cuandoSeAplicaLaAccion();

        entoncesCambiaDatosYElAplicadorAnota("crear");
    }

    @Test
    @DisplayName("Una acción incompleta cambia datos pero, al aplicarla, dice qué le falta")
    void unaAccionIncompletaCambiaDatosPeroAlAplicarlaDiceQueLeFalta() {
        dadoUnaAccionDeEditarQueNoIndicaLaFila();

        cuandoSeIntentaAplicarLaAccion();

        entoncesCambiaDatosPeroSeRechazaPorque("No indica la fila");
    }

    @Test
    @DisplayName("Las acciones que solo llevan a una pantalla no cambian datos ni piden nada al aplicador")
    void lasAccionesQueSoloLlevanAUnaPantallaNoCambianDatosNiLePidenNadaAlAplicador() {
        dadoLasAccionesQueSoloLlevanAUnaPantalla();

        cuandoSeAplicanTodasLasAcciones();

        entoncesNingunaCambiaDatosYElAplicadorNoAnotaNada();
    }

    private void dadoUnaAccionDeEditarElPrecioDeUnaFila() {
        accion = new AccionEditarRegistro("Bajar", null, null, null, Map.of("price", 1));
    }

    private void dadoUnaAccionDeCrearUnCombo() {
        accion = new AccionCrearRegistro("Crear", null, null, Map.of("name", "Combo"));
    }

    private void dadoUnaAccionDeEditarQueNoIndicaLaFila() {
        accion = new AccionInvalida("EDITAR_REGISTRO", "No indica la fila", Map.of());
    }

    private void dadoLasAccionesQueSoloLlevanAUnaPantalla() {
        atajos = List.of(
                new AccionAbrirRegistro("Ver producto", null, null, null),
                new AccionRegistrar("Registrar entrada", null, null),
                new AccionVerZona("Ver en Mi zona", "yerba"),
                new AccionDesconocida("NUEVA", Map.of()));
    }

    private void cuandoSeAplicaLaAccion() {
        accion.aplicarEn(aplicador);
    }

    private void cuandoSeIntentaAplicarLaAccion() {
        error = catchThrowable(this::cuandoSeAplicaLaAccion);
    }

    private void cuandoSeAplicanTodasLasAcciones() {
        atajos.forEach(atajo -> atajo.aplicarEn(aplicador));
    }

    private void entoncesCambiaDatosYElAplicadorAnota(String anotacion) {
        assertThat(accion.modificaDatos()).isTrue();
        assertThat(aplicador.anotado).containsExactly(anotacion);
    }

    private void entoncesCambiaDatosPeroSeRechazaPorque(String motivo) {
        assertThat(accion.modificaDatos()).isTrue();
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesNingunaCambiaDatosYElAplicadorNoAnotaNada() {
        assertThat(atajos).noneMatch(AccionDeAviso::modificaDatos);
        assertThat(aplicador.anotado).isEmpty();
    }

    private static final class AplicadorQueAnota implements AplicadorDeAcciones {

        private final List<String> anotado = new ArrayList<>();

        @Override
        public void editar(AccionEditarRegistro accion) {
            anotado.add("editar");
        }

        @Override
        public void crear(AccionCrearRegistro accion) {
            anotado.add("crear");
        }
    }
}
