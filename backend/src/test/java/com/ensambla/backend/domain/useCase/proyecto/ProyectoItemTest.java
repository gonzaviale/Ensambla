package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;

class ProyectoItemTest {

    private Proyecto proyecto;
    private ProyectoItem respuesta;

    @Test
    @DisplayName("Mapea todos los datos y el orden de los bloques y campos")
    void laRespuestaCopiaLosDatosDelProyectoYElOrdenDeSusBloquesYCampos() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeArmaLaRespuestaDelProyecto();

        entoncesCopiaLosDatosDelProyecto();
        entoncesCopiaElPrimerBloqueConSusDosCampos();
        entoncesCopiaElPrimerCampoDelPrimerBloque();
        entoncesElSegundoBloqueNoTieneCampos();
    }

    // --- dado ---
    private void dadoUnProyectoConProductosYUnBloqueVacio() {
        proyecto = proyecto();
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuestaDelProyecto() {
        respuesta = ProyectoItem.de(proyecto);
    }

    // --- entonces ---
    private void entoncesCopiaLosDatosDelProyecto() {
        assertThat(respuesta.id()).isEqualTo(PROYECTO_ID);
        assertThat(respuesta.nombre()).isEqualTo(proyecto.getNombre());
        assertThat(respuesta.descripcion()).isEqualTo(proyecto.getDescripcion());
        assertThat(respuesta.plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(respuesta.creadoEn()).isEqualTo(AHORA);
        assertThat(respuesta.actualizadoEn()).isEqualTo(AHORA);
        assertThat(respuesta.bloques()).hasSize(2);
    }

    private void entoncesCopiaElPrimerBloqueConSusDosCampos() {
        ProyectoItem.BloqueItem bloque = respuesta.bloques().getFirst();
        assertThat(bloque.id()).isEqualTo(proyecto.getBloques().getFirst().getId());
        assertThat(bloque.nombre()).isEqualTo("Productos");
        assertThat(bloque.posicion()).isZero();
        assertThat(bloque.campos()).hasSize(2);
    }

    private void entoncesCopiaElPrimerCampoDelPrimerBloque() {
        ProyectoItem.CampoItem campo = respuesta.bloques().getFirst().campos().getFirst();
        assertThat(campo.id()).isEqualTo(proyecto.getBloques().getFirst().getCampos().getFirst().getId());
        assertThat(campo.nombre()).isEqualTo("Nombre");
        assertThat(campo.tipo()).isEqualTo("TEXTO");
        assertThat(campo.obligatorio()).isTrue();
        assertThat(campo.valorInicial()).isNull();
        assertThat(campo.opciones()).isEmpty();
        assertThat(campo.bloqueRelacionadoId()).isNull();
        assertThat(campo.posicion()).isZero();
    }

    private void entoncesElSegundoBloqueNoTieneCampos() {
        assertThat(respuesta.bloques().get(1).campos()).isEmpty();
    }
}
