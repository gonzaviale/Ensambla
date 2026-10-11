package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("Agrega el bloque al final, en el negocio y el proyecto, sin guardarlo todavía")
    void agregarBloqueShouldPlaceBloqueAfterTheLastOne() {
        Proyecto proyecto = ProyectoTestData.proyecto();
        proyecto.getBloques().getFirst().setPosicion((short) 4);

        Bloque result = proyecto.agregarBloque(" Clientes ", AHORA.plusDays(1));

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getPosicion()).isEqualTo((short) 5);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA.plusDays(1));
        assertThat(proyecto.getBloques()).hasSize(2);
    }

    @Test
    @DisplayName("El primer bloque de un proyecto en blanco queda en la posición cero")
    void agregarBloqueShouldStartAtZeroWhenProyectoIsEmpty() {
        Proyecto proyecto = Proyecto.enBlanco(NEGOCIO_ID, "Vacío", null, AHORA);

        Bloque result = proyecto.agregarBloque("Productos", AHORA);

        assertThat(result.getPosicion()).isZero();
    }

    @Test
    @DisplayName("Rechaza un bloque con el nombre de otro aunque cambien mayúsculas y espacios")
    void agregarBloqueShouldRejectRepeatedNameIgnoringCase() {
        Proyecto proyecto = ProyectoTestData.proyecto();

        assertThatThrownBy(() -> proyecto.agregarBloque("  PRODUCTOS ", AHORA))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya hay un bloque llamado \"PRODUCTOS\" en este proyecto");
    }

    @Test
    @DisplayName("Un bloque puede quedarse con su propio nombre al renombrarse")
    void validarNombreDeBloqueLibreShouldAllowTheSameBloque() {
        Proyecto proyecto = ProyectoTestData.proyecto();
        UUID productosId = proyecto.getBloques().getFirst().getId();

        assertThatCode(() -> proyecto.validarNombreDeBloqueLibre("productos", productosId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Renombrar con el nombre de otro bloque del proyecto se rechaza")
    void validarNombreDeBloqueLibreShouldRejectNameOfAnotherBloque() {
        Proyecto proyecto = ProyectoTestData.proyecto();
        UUID vacioId = proyecto.getBloques().get(1).getId();

        assertThatThrownBy(() -> proyecto.validarNombreDeBloqueLibre("Productos", vacioId))
                .isInstanceOf(DuplicateResourceException.class);
    }
}

