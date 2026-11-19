package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PromptSamiTest {

    private final PromptSami prompt = new PromptSami(
            new ClassPathResource("ia/prompt-sami.md"), new ClassPathResource("ia/esquema-sami.json"));

    private PeticionModeloTexto peticion;
    private Throwable rechazo;

    @Test
    @DisplayName("El contexto lista cada plantilla con sus columnas y tipos, y el texto de la persona")
    void elContextoListaCadaPlantillaConSusColumnasYTiposYElTextoDeLaPersona() {
        cuandoSePideLaPeticionConLasPlantillasDePrueba(List.of());

        entoncesElContextoListaLasPlantillasYElTextoDeLaPersona();
    }

    @Test
    @DisplayName("Sin aclaraciones no agrega la sección")
    void sinAclaracionesNoAgregaLaSeccion() {
        cuandoSePideLaPeticionConLasPlantillasDePrueba(List.of());

        entoncesElContextoNoTieneAclaraciones();
    }

    @Test
    @DisplayName("Con aclaraciones las agrega una por línea")
    void conAclaracionesLasAgregaUnaPorLinea() {
        cuandoSePideLaPeticionConLasPlantillasDePrueba(List.of("uno → A", "dos → B"));

        entoncesElContextoListaLasAclaracionesUnaPorLinea();
    }

    @Test
    @DisplayName("Una plantilla sin bloques se lista con columnas vacías")
    void unaPlantillaSinBloquesSeListaConColumnasVacias() {
        cuandoSePideLaPeticionConUnaPlantillaSinBloques();

        entoncesLaPlantillaSeListaConColumnasVacias();
    }

    @Test
    @DisplayName("Si falta un recurso del prompt la aplicación no levanta")
    void siFaltaUnRecursoDelPromptLaAplicacionNoLevanta() {
        cuandoSeIntentaArmarElPromptConUnRecursoInexistente();

        entoncesSeRechazaElArranque();
    }

    // --- cuando ---

    private void cuandoSePideLaPeticionConLasPlantillasDePrueba(List<String> aclaraciones) {
        peticion = prompt.peticion(new ConsultaIaSami("vendo velas", aclaraciones, SamiPruebas.plantillas()));
    }

    private void cuandoSePideLaPeticionConUnaPlantillaSinBloques() {
        PlantillaProyecto vacia = new PlantillaProyecto(PlantillaId.de(UUID.randomUUID()), "Vacía", "",
                new EstructuraPlantilla(List.of()));
        peticion = prompt.peticion(new ConsultaIaSami("hola", List.of(), List.of(vacia)));
    }

    private void cuandoSeIntentaArmarElPromptConUnRecursoInexistente() {
        rechazo = catchThrowable(() -> new PromptSami(
                new ClassPathResource("ia/no-existe.md"), new ByteArrayResource(new byte[0])));
    }

    // --- entonces ---

    private void entoncesElContextoListaLasPlantillasYElTextoDeLaPersona() {
        assertThat(peticion.contexto())
                .startsWith("Plantillas disponibles")
                .contains("- Pedidos: columnas Cliente (TEXTO), Entrega (FECHA)")
                .contains("vendo velas");
        assertThat(peticion.instrucciones()).contains("Sos Sami");
        assertThat(peticion.esquemaJson()).contains("additionalProperties");
    }

    private void entoncesElContextoNoTieneAclaraciones() {
        assertThat(peticion.contexto()).doesNotContain("Aclaraciones");
    }

    private void entoncesElContextoListaLasAclaracionesUnaPorLinea() {
        assertThat(peticion.contexto()).contains("Aclaraciones que ya dio:\n- uno → A\n- dos → B\n");
    }

    private void entoncesLaPlantillaSeListaConColumnasVacias() {
        assertThat(peticion.contexto()).contains("- Vacía: columnas \n");
    }

    private void entoncesSeRechazaElArranque() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }
}
