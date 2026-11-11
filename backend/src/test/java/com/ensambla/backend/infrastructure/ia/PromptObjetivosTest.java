package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static org.assertj.core.api.Assertions.assertThat;

class PromptObjetivosTest {

    private final PromptObjetivos prompt = new PromptObjetivos(
            new ClassPathResource("ia/prompt-objetivos.md"), new ClassPathResource("ia/esquema-objetivos.json"));

    @Test
    @DisplayName("El código de cada mini-objetivo es su lugar en la lista, empezando en m1")
    void codigoShouldBeThePositionInTheList() {
        assertThat(PromptObjetivos.codigo(0)).isEqualTo("m1");
        assertThat(PromptObjetivos.codigo(2)).isEqualTo("m3");
    }

    @Test
    @DisplayName("El pedido lleva las instrucciones, el esquema y cada candidato con sus números y su texto de reserva")
    void peticionShouldCarryInstructionsSchemaAndEveryCandidate() {
        PeticionModeloTexto result = prompt.peticion(List.of(
                candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180")));

        assertThat(result.instrucciones()).contains("hasta 3", "NO calcules");
        assertThat(result.esquemaJson()).contains("\"minis\"", "\"codigo\"");
        assertThat(result.contexto()).contains(
                "m1: tipo SUBIR_PRECIO, sobre Yerba 1 kg. Hoy vale 12600.00 y la meta es 14183.00. "
                        + "Suma cerca de $6786.00 por mes. Texto de reserva: Subile $250 a Yerba 1 kg.",
                "m2: tipo MEJORAR_MARGEN, sobre el negocio. Hoy vale 35.71 y la meta es 37.71. "
                        + "Suma cerca de $6180 por mes.");
    }
}
