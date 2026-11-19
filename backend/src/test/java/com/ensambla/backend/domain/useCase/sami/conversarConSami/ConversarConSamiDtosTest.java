package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.OpcionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.PreguntaSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarConSamiDtosTest {

    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();
    private static final PreguntaSami SIN_PREGUNTA = null;

    private List<RespuestaSami> respuestas;
    private ConversarConSamiResponse respuesta;

    @Test
    @DisplayName("Convierte las respuestas del DTO a las del dominio")
    void convierteLasRespuestasDelDtoALasDelDominio() {
        cuandoLaPersonaResponde(new RespuestaSamiItem("principal", "stock"));

        entoncesLasRespuestasSon(new RespuestaSami("principal", "stock"));
    }

    @Test
    @DisplayName("Un paso de pregunta lleva la pregunta con sus opciones y no la propuesta")
    void unPasoDePreguntaLlevaLaPreguntaYNoLaPropuesta() {
        cuandoSeRespondeElPaso(new PasoConversacionSami(TipoPaso.PREGUNTA, unaPregunta(), unaInterpretacion(), 1));

        entoncesEsDeTipo("PREGUNTA");
        entoncesPreguntaConLasOpciones("principal", "Lo que vendo");
        entoncesNoTraePropuesta();
        entoncesLlevaPreguntasHechas(1);
    }

    @Test
    @DisplayName("Un paso de propuesta lleva plantilla, columnas y confianza con origen algoritmo")
    void unPasoDePropuestaLlevaPlantillaColumnasYConfianza() {
        cuandoSeRespondeElPaso(new PasoConversacionSami(TipoPaso.PROPUESTA, SIN_PREGUNTA, unaInterpretacion(), 0));

        entoncesEsDeTipo("PROPUESTA");
        entoncesNoTraePregunta();
        entoncesProponeLaPlantillaDeVentasCon("ALGORITMO");
    }

    @Test
    @DisplayName("Un paso de respaldo con IA no lleva ni pregunta ni propuesta")
    void unPasoDeRespaldoConIaNoLlevaPreguntaNiPropuesta() {
        cuandoSeRespondeElPaso(new PasoConversacionSami(TipoPaso.RESPALDO_IA, SIN_PREGUNTA, unaInterpretacion(), 3));

        entoncesEsDeTipo("RESPALDO_IA");
        entoncesNoTraePregunta();
        entoncesNoTraePropuesta();
    }

    @Test
    @DisplayName("Una interpretación suelta se arma como propuesta con el origen indicado")
    void unaInterpretacionSueltaSeArmaComoPropuesta() {
        cuandoSeRespondeLaInterpretacion(unaInterpretacion(), PropuestaSamiItem.ORIGEN_IA, 2);

        entoncesEsDeTipo("PROPUESTA");
        entoncesNoTraePregunta();
        entoncesLaPropuestaVieneDe("IA");
        entoncesLlevaPreguntasHechas(2);
    }

    private static PreguntaSami unaPregunta() {
        return new PreguntaSami("principal", "¿Qué querés anotar?", List.of(new OpcionSami("ventas", "Lo que vendo")));
    }

    private static InterpretacionSami unaInterpretacion() {
        return new InterpretacionSami(PLANTILLA_ID, "Ventas",
                List.of(new ColumnaSugerida("Saldo pendiente", TipoCampo.NUMERO, List.of())), 0.9, List.of());
    }

    private void cuandoLaPersonaResponde(RespuestaSamiItem... items) {
        respuestas = new ConversarConSamiRequest(null, null, List.of(items)).respuestasSami();
    }

    private void cuandoSeRespondeElPaso(PasoConversacionSami paso) {
        respuesta = ConversarConSamiResponse.de(paso);
    }

    private void cuandoSeRespondeLaInterpretacion(InterpretacionSami interpretacion, String origen, int preguntas) {
        respuesta = ConversarConSamiResponse.de(interpretacion, origen, preguntas);
    }

    private void entoncesLasRespuestasSon(RespuestaSami... esperadas) {
        assertThat(respuestas).containsExactly(esperadas);
    }

    private void entoncesEsDeTipo(String tipo) {
        assertThat(respuesta.tipo()).isEqualTo(tipo);
    }

    private void entoncesPreguntaConLasOpciones(String id, String... opciones) {
        assertThat(respuesta.pregunta().id()).isEqualTo(id);
        assertThat(respuesta.pregunta().opciones()).extracting("texto").containsExactly((Object[]) opciones);
    }

    private void entoncesNoTraePregunta() {
        assertThat(respuesta.pregunta()).isNull();
    }

    private void entoncesNoTraePropuesta() {
        assertThat(respuesta.propuesta()).isNull();
    }

    private void entoncesLlevaPreguntasHechas(int preguntas) {
        assertThat(respuesta.preguntasHechas()).isEqualTo(preguntas);
    }

    private void entoncesProponeLaPlantillaDeVentasCon(String origen) {
        assertThat(respuesta.propuesta().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(respuesta.propuesta().plantilla()).isEqualTo("Ventas");
        assertThat(respuesta.propuesta().columnas()).extracting("nombre", "tipo", "opciones")
                .containsExactly(Tuple.tuple("Saldo pendiente", "NUMERO", List.of()));
        assertThat(respuesta.propuesta().confianza()).isEqualTo(0.9);
        entoncesLaPropuestaVieneDe(origen);
    }

    private void entoncesLaPropuestaVieneDe(String origen) {
        assertThat(respuesta.propuesta().origen()).isEqualTo(origen);
    }
}
