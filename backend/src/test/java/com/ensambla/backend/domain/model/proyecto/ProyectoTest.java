package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProyectoTest {

    @Test
    @DisplayName("Crea un proyecto en blanco con el nombre normalizado y sin estructura")
    void enBlancoShouldNormalizeNameAndSetDates() {
        Proyecto result = Proyecto.enBlanco(NEGOCIO_ID, "  Mi negocio  ", null, AHORA);

        assertThat(result.getNombre()).isEqualTo("Mi negocio");
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getDescripcion()).isNull();
        assertThat(result.getPlantillaId()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getBloques()).isEmpty();
    }

    @Test
    @DisplayName("Copia los bloques y campos con orden y valores propios sin alterar la plantilla")
    void desdePlantillaShouldCopyStructure() {
        var plantilla = ProyectoTestData.plantilla();

        Proyecto result = Proyecto.desdePlantilla(NEGOCIO_ID, plantilla, AHORA);

        assertThat(result.getPlantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.getDescripcion()).isEqualTo("Mis productos");
        assertThat(result.getBloques()).extracting("nombre").containsExactly("Productos", "Vacío");
        var bloque = result.getBloques().getFirst();
        assertThat(bloque.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(bloque.getPosicion()).isZero();
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getCampos()).extracting("nombre").containsExactly("Nombre", "Rubro");
        var campo = bloque.getCampos().getFirst();
        assertThat(campo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campo.getPosicion()).isZero();
        assertThat(campo.getObligatorio()).isTrue();
        assertThat(campo.getOpciones()).isEmpty();
        assertThat(campo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campo.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getCampos().get(1).getValorInicial()).isEqualTo("Insumos");
        bloque.setNombre("Otra cosa");
        assertThat(plantilla.estructura().bloques().getFirst().nombre()).isEqualTo("Productos");
    }

    @Test
    @DisplayName("Renombra preservando la fecha de creación y actualizando la de modificación")
    void renombrarShouldPreserveCreationDate() {
        Proyecto proyecto = ProyectoTestData.proyecto();

        proyecto.renombrar("  Nuevo  ", AHORA.plusDays(1));

        assertThat(proyecto.getNombre()).isEqualTo("Nuevo");
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(proyecto.getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }
}

