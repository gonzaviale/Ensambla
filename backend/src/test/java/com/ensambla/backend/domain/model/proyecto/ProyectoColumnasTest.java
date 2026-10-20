package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProyectoColumnasTest {

    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.desdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA);
    }

    @Test
    @DisplayName("Suma las columnas al final del primer bloque, con su tipo, opciones y posición")
    void agregarColumnasShouldAppendToFirstBlock() {
        proyecto.agregarColumnas(List.of(
                columna("  Saldo pendiente ", "NUMERO", null),
                columna("Forma de pago", "OPCION", List.of("Efectivo", "Tarjeta"))), AHORA);

        List<Campo> campos = proyecto.getBloques().getFirst().getCampos();
        assertThat(campos).extracting(Campo::getNombre)
                .containsExactly("Nombre", "Rubro", "Saldo pendiente", "Forma de pago");
        assertThat(campos.get(2).getTipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(campos.get(2).getPosicion()).isEqualTo((short) 2);
        assertThat(campos.get(2).getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campos.get(3).getOpciones()).containsExactly("Efectivo", "Tarjeta");
        assertThat(proyecto.getBloques().get(1).getCampos()).isEmpty();
    }

    @Test
    @DisplayName("Sin columnas no cambia nada, ni siquiera si el proyecto no tiene bloques")
    void agregarColumnasShouldDoNothingWithoutColumns() {
        Proyecto enBlanco = Proyecto.enBlanco(NEGOCIO_ID, "Mi negocio", null, AHORA);

        enBlanco.agregarColumnas(List.of(), AHORA);
        proyecto.agregarColumnas(List.of(), AHORA);

        assertThat(proyecto.getBloques().getFirst().getCampos()).hasSize(2);
    }

    @Test
    @DisplayName("Un proyecto sin bloques no tiene dónde agregar columnas")
    void agregarColumnasShouldFailWithoutBlocks() {
        Proyecto enBlanco = Proyecto.enBlanco(NEGOCIO_ID, "Mi negocio", null, AHORA);

        assertThatThrownBy(() -> enBlanco.agregarColumnas(List.of(columna("Saldo", "NUMERO", null)), AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La plantilla no tiene una tabla donde agregar columnas");
    }

    @Test
    @DisplayName("Rechaza una columna con el nombre de una que la plantilla ya trae, sin distinguir mayúsculas")
    void agregarColumnasShouldRejectNameOfExistingColumn() {
        assertThatThrownBy(() -> proyecto.agregarColumnas(List.of(columna(" nombre ", "TEXTO", null)), AHORA))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya hay una columna llamada \"nombre\"");
    }

    @Test
    @DisplayName("Rechaza dos columnas nuevas con el mismo nombre")
    void agregarColumnasShouldRejectRepeatedNewColumn() {
        List<ConfiguracionCampo> repetidas = List.of(columna("Saldo", "NUMERO", null), columna("SALDO", "TEXTO", null));

        assertThatThrownBy(() -> proyecto.agregarColumnas(repetidas, AHORA))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("No admite conexiones ni fórmulas y dice cuál de las columnas es")
    void agregarColumnasShouldRejectRelationAndFormula() {
        assertThatThrownBy(() -> proyecto.agregarColumnas(
                List.of(columna("Total", "NUMERO", null), columna("Cliente", "RELACION", null)), AHORA))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsOnlyKeys("columnas[1].tipo"));
        assertThatThrownBy(() -> proyecto.agregarColumnas(List.of(columna("Cuenta", "FORMULA", null)), AHORA))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsOnlyKeys("columnas[0].tipo"));
    }

    @Test
    @DisplayName("Los errores de una columna llevan su posición para ubicarla")
    void agregarColumnasShouldPrefixValidationErrorsWithPosition() {
        List<ConfiguracionCampo> columnas = List.of(
                columna("Saldo", "NUMERO", null),
                columna("Forma de pago", "OPCION", List.of("Efectivo", "efectivo")),
                columna(" ", "TEXTO", null));

        assertThatThrownBy(() -> proyecto.agregarColumnas(columnas.subList(1, 3), AHORA))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsOnlyKeys("columnas[0].opciones").doesNotContainKey("columnas[1].nombre"));
        assertThatThrownBy(() -> proyecto.agregarColumnas(
                List.of(columnas.getFirst(), columnas.getLast()), AHORA))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsOnlyKeys("columnas[1].nombre"));
    }

    private ConfiguracionCampo columna(String nombre, String tipo, List<String> opciones) {
        return new ConfiguracionCampo(nombre, tipo, false, null, opciones, null, null);
    }
}
