package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterpretacionSamiRespuestasTest {

    private static final String TEXTO_AMBIGUO = "quiero un stock y llevar los pedidos";

    private InterpretacionSamiService service;

    @BeforeEach
    void setUp() {
        service = new InterpretacionSamiService(SamiPruebas.diccionarioReal());
    }

    @Test
    @DisplayName("Elegir una opción decide la plantilla y descarta a las que esa pregunta separaba")
    void interpretarShouldDecideTemplateWhenOptionIsChosen() {
        InterpretacionSami result = interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "stock"));

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.puntajes()).extracting("plantilla").containsExactly("Inventario");
        assertThat(result.confianza()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("La opción «ninguna de estas» descarta todas las plantillas de la pregunta")
    void interpretarShouldDiscardAllTemplatesWhenNoneOfThese() {
        InterpretacionSami result = interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "otra"));

        assertThat(result.plantilla()).isNull();
        assertThat(result.puntajes()).isEmpty();
    }

    @Test
    @DisplayName("Una respuesta posterior puede recuperar una plantilla que una anterior había descartado")
    void interpretarShouldLetLaterAnswerRecoverDiscardedTemplate() {
        InterpretacionSami result = interpretar("hola",
                new RespuestaSami("principal", "ventas"),
                new RespuestaSami("venta-o-stock", "stock"));

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.puntajes()).extracting("plantilla").containsExactly("Inventario");
    }

    @Test
    @DisplayName("Rechaza una respuesta a una pregunta que no existe")
    void interpretarShouldRejectUnknownQuestion() {
        assertThatThrownBy(() -> interpretar(TEXTO_AMBIGUO, new RespuestaSami("inventada", "x")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La pregunta inventada no existe");
    }

    @Test
    @DisplayName("Rechaza una opción que la pregunta no tiene")
    void interpretarShouldRejectUnknownOption() {
        assertThatThrownBy(() -> interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "inventada")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La opción inventada no existe en la pregunta principal");
    }

    private InterpretacionSami interpretar(String texto, RespuestaSami... respuestas) {
        return service.interpretar(texto, SamiPruebas.plantillas(), List.of(respuestas));
    }
}
