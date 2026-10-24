package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.OpcionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.PreguntaSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarConSamiDtosTest {

    private static final UUID PLANTILLA_ID = UUID.randomUUID();

    @Test
    @DisplayName("Convierte las respuestas del DTO a las del dominio")
    void deShouldMapItems() {
        List<RespuestaSami> result = new ConversarConSamiRequest(
                null, null, List.of(new RespuestaSamiItem("principal", "stock"))).respuestasSami();

        assertThat(result).containsExactly(new RespuestaSami("principal", "stock"));
    }

    @Test
    @DisplayName("Un paso de pregunta lleva la pregunta con sus opciones y no la propuesta")
    void deShouldMapQuestionStep() {
        PreguntaSami pregunta = new PreguntaSami("principal", "¿Qué querés anotar?",
                List.of(new OpcionSami("ventas", "Lo que vendo")));

        ConversarConSamiResponse result = ConversarConSamiResponse.de(
                new PasoConversacionSami(TipoPaso.PREGUNTA, pregunta, interpretacion(), 1));

        assertThat(result.tipo()).isEqualTo("PREGUNTA");
        assertThat(result.pregunta().id()).isEqualTo("principal");
        assertThat(result.pregunta().opciones()).extracting("texto").containsExactly("Lo que vendo");
        assertThat(result.propuesta()).isNull();
        assertThat(result.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un paso de propuesta lleva plantilla, columnas y confianza con origen algoritmo")
    void deShouldMapProposalStep() {
        ConversarConSamiResponse result = ConversarConSamiResponse.de(
                new PasoConversacionSami(TipoPaso.PROPUESTA, null, interpretacion(), 0));

        assertThat(result.tipo()).isEqualTo("PROPUESTA");
        assertThat(result.pregunta()).isNull();
        assertThat(result.propuesta().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.propuesta().plantilla()).isEqualTo("Ventas");
        assertThat(result.propuesta().columnas()).extracting("nombre", "tipo", "opciones")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Saldo pendiente", "NUMERO", List.of()));
        assertThat(result.propuesta().confianza()).isEqualTo(0.9);
        assertThat(result.propuesta().origen()).isEqualTo("ALGORITMO");
    }

    @Test
    @DisplayName("Un paso de respaldo con IA no lleva ni pregunta ni propuesta")
    void deShouldMapAiFallbackStep() {
        ConversarConSamiResponse result = ConversarConSamiResponse.de(
                new PasoConversacionSami(TipoPaso.RESPALDO_IA, null, interpretacion(), 3));

        assertThat(result.tipo()).isEqualTo("RESPALDO_IA");
        assertThat(result.pregunta()).isNull();
        assertThat(result.propuesta()).isNull();
    }

    @Test
    @DisplayName("Una interpretación suelta se arma como propuesta con el origen indicado")
    void deShouldMapInterpretationWithGivenOrigin() {
        ConversarConSamiResponse result = ConversarConSamiResponse.de(interpretacion(), PropuestaSamiItem.ORIGEN_IA, 2);

        assertThat(result.tipo()).isEqualTo("PROPUESTA");
        assertThat(result.pregunta()).isNull();
        assertThat(result.propuesta().origen()).isEqualTo("IA");
        assertThat(result.preguntasHechas()).isEqualTo(2);
    }

    private InterpretacionSami interpretacion() {
        return new InterpretacionSami(PLANTILLA_ID, "Ventas",
                List.of(new ColumnaSugerida("Saldo pendiente", TipoCampo.NUMERO, List.of())), 0.9, List.of());
    }
}
