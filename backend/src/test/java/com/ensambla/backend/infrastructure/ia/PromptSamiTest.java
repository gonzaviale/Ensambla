package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptSamiTest {

    private final PromptSami prompt = new PromptSami(
            new ClassPathResource("ia/prompt-sami.md"), new ClassPathResource("ia/esquema-sami.json"));

    @Test
    @DisplayName("El contexto lista cada plantilla con sus columnas y tipos, y el texto de la persona")
    void peticionShouldListTemplatesAndText() {
        PeticionModeloTexto peticion = prompt.peticion(
                new ConsultaIaSami("vendo velas", List.of(), SamiPruebas.plantillas()));

        assertThat(peticion.contexto())
                .startsWith("Plantillas disponibles")
                .contains("- Pedidos: columnas Cliente (TEXTO), Entrega (FECHA)")
                .contains("vendo velas");
        assertThat(peticion.instrucciones()).contains("Sos Sami");
        assertThat(peticion.esquemaJson()).contains("additionalProperties");
    }

    @Test
    @DisplayName("Sin aclaraciones no agrega la sección")
    void peticionShouldOmitClarificationsWhenThereAreNone() {
        PeticionModeloTexto peticion = prompt.peticion(
                new ConsultaIaSami("vendo velas", List.of(), SamiPruebas.plantillas()));

        assertThat(peticion.contexto()).doesNotContain("Aclaraciones");
    }

    @Test
    @DisplayName("Con aclaraciones las agrega una por línea")
    void peticionShouldListClarifications() {
        PeticionModeloTexto peticion = prompt.peticion(new ConsultaIaSami(
                "vendo velas", List.of("uno → A", "dos → B"), SamiPruebas.plantillas()));

        assertThat(peticion.contexto()).contains("Aclaraciones que ya dio:\n- uno → A\n- dos → B\n");
    }

    @Test
    @DisplayName("Una plantilla sin bloques se lista con columnas vacías")
    void peticionShouldHandleTemplateWithoutBlocks() {
        PeticionModeloTexto peticion = prompt.peticion(new ConsultaIaSami("hola", List.of(), List.of(
                new com.ensambla.backend.domain.model.plantilla.PlantillaProyecto(
                        PlantillaId.de(java.util.UUID.randomUUID()), "Vacía", "",
                        new com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla(List.of())))));

        assertThat(peticion.contexto()).contains("- Vacía: columnas \n");
    }

    @Test
    @DisplayName("Si falta un recurso del prompt la aplicación no levanta")
    void constructorShouldFailWhenResourceIsMissing() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new PromptSami(
                        new ClassPathResource("ia/no-existe.md"), new ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalStateException.class);
    }
}
