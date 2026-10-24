package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sami.ColumnaDiccionario;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PlantillaDiccionario;
import com.ensambla.backend.domain.model.sami.TerminoDiccionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretacionSamiColumnasTest {

    @Test
    @DisplayName("Una columna pensada para cualquier plantilla no se repite si la sugerida ya la trae")
    void interpretarShouldNotRepeatColumnThatAppliesToAnyTemplateWhenItIsAlreadyThere() {
        DiccionarioSami diccionario = new DiccionarioSami(
                List.of(new PlantillaDiccionario("Clientes", List.of(new TerminoDiccionario("agenda", 5)))),
                List.of(new ColumnaDiccionario("Teléfono", TipoCampo.TEXTO, null, List.of("telefono"), null)),
                null);

        InterpretacionSami result = new InterpretacionSamiService(diccionario).interpretar(
                "mi agenda con el teléfono", SamiPruebas.plantillas(), List.of());

        assertThat(result.plantilla()).isEqualTo("Clientes");
        assertThat(result.columnasExtra()).isEmpty();
    }
}
