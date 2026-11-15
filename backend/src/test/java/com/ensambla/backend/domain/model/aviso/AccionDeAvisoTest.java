package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccionDeAvisoTest {

    private final AplicadorQueAnota aplicador = new AplicadorQueAnota();

    @Test
    @DisplayName("Editar una fila cambia datos y le pide al aplicador que la edite")
    void editarShouldChangeDataThroughTheApplier() {
        AccionEditarRegistro accion = new AccionEditarRegistro("Bajar", null, null, null, Map.of("price", 1));

        accion.aplicarEn(aplicador);

        assertThat(accion.modificaDatos()).isTrue();
        assertThat(aplicador.anotado).containsExactly("editar");
    }

    @Test
    @DisplayName("Crear una fila cambia datos y le pide al aplicador que la cree")
    void crearShouldChangeDataThroughTheApplier() {
        AccionCrearRegistro accion = new AccionCrearRegistro("Crear", null, null, Map.of("name", "Combo"));

        accion.aplicarEn(aplicador);

        assertThat(accion.modificaDatos()).isTrue();
        assertThat(aplicador.anotado).containsExactly("crear");
    }

    @Test
    @DisplayName("Una acción incompleta cambia datos pero al aplicarla dice qué le falta")
    void invalidShouldExplainWhatIsMissing() {
        AccionInvalida accion = new AccionInvalida("EDITAR_REGISTRO", "No indica la fila", Map.of());

        assertThat(accion.modificaDatos()).isTrue();
        assertThatThrownBy(() -> accion.aplicarEn(aplicador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No indica la fila");
    }

    @Test
    @DisplayName("Las acciones que solo llevan a una pantalla no cambian datos ni piden nada al aplicador")
    void shortcutsShouldNotChangeAnything() {
        List<AccionDeAviso> atajos = List.of(
                new AccionAbrirRegistro("Ver producto", null, null, null),
                new AccionRegistrar("Registrar entrada", null, null),
                new AccionVerZona("Ver en Mi zona", "yerba"),
                new AccionDesconocida("NUEVA", Map.of()));

        atajos.forEach(atajo -> atajo.aplicarEn(aplicador));

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
