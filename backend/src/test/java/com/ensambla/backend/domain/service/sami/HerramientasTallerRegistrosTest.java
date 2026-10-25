package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.FilaTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** editar_registro y quitar_registro: cambiar o sacar un registro que Sami ve en la pieza. */
class HerramientasTallerRegistrosTest {

    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                    new CampoTaller("Precio", "currency")), List.of(
                    new FilaTaller("f1", List.of("Yerba", "24", "3600")),
                    new FilaTaller("f2", List.of("Fideos", "30", "1500")),
                    new FilaTaller("f3", List.of("Arroz", "5", "1000")),
                    new FilaTaller("f4", List.of("Arroz", "8", "1100")))),
            new PiezaTaller("e2", "clients", "Clientes", null, List.of(
                    new CampoTaller("Nombre", "text"), new CampoTaller("Teléfono", "tel")))));

    private HerramientasTallerService service;

    @BeforeEach
    void setUp() {
        service = new HerramientasTallerService(SamiPruebas.catalogoReal(), SamiPruebas.kitsReales());
    }

    @Test
    @DisplayName("Cambia un dato de un registro nombrado por su dato principal y viaja con su id")
    void editarRegistroShouldChangeRowFoundByName() {
        ValidacionTaller result = validar(editar("Inventario", " yerba ", Map.of("cantidad", 30)));

        assertThat(result.acciones()).containsExactly(new AccionTaller("editar_registro", Map.of(
                "pieza", "e1", "registro", "f1", "cambios", Map.of("Cantidad", 30))));
    }

    @Test
    @DisplayName("Encuentra el registro por su id aunque haya dos con el mismo nombre")
    void editarRegistroShouldFindRowByIdWhenNameIsRepeated() {
        ValidacionTaller result = validar(editar("e1", "f4", Map.of("Precio", 1200)));

        assertThat(result.acciones()).hasSize(1);
        assertThat(result.acciones().getFirst().argumentos()).containsEntry("registro", "f4");
    }

    @Test
    @DisplayName("No adivina cuando el nombre se repite ni cuando el registro no existe")
    void registrosShouldBeDiscardedWhenAmbiguousOrMissing() {
        ValidacionTaller result = validar(
                editar("e1", "Arroz", Map.of("Precio", 1200)),
                quitar("e1", "Arroz"),
                quitar("e1", "Harina"),
                quitar("nada", "Yerba"));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(4);
    }

    @Test
    @DisplayName("Descarta los cambios con etiquetas inexistentes o valores inválidos")
    void editarRegistroShouldDiscardInvalidChanges() {
        ValidacionTaller result = validar(
                editar("e1", "Yerba", Map.of("Inventado", 1)),
                editar("e1", "Yerba", Map.of("Precio", List.of(1))),
                editar("e1", "Yerba", "texto"));

        assertThat(result.acciones()).isEmpty();
        assertThat(result.descartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Un valor vacío borra el dato, pero no el principal, que nombra al registro")
    void editarRegistroShouldClearDataButNotThePrimaryOne() {
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("Producto", "");
        cambios.put("Precio", null);

        ValidacionTaller result = validar(editar("e1", "Yerba", cambios));

        assertThat(result.acciones().getFirst().argumentos())
                .containsEntry("cambios", Map.of("Precio", ""));
    }

    @Test
    @DisplayName("Quita un registro y no se lo puede volver a nombrar en el mismo turno")
    void quitarRegistroShouldRemoveRowAndForgetIt() {
        ValidacionTaller result = validar(quitar("Inventario", "Fideos"), quitar("Inventario", "Fideos"));

        assertThat(result.acciones()).containsExactly(
                new AccionTaller("quitar_registro", Map.of("pieza", "e1", "registro", "f2")));
        assertThat(result.descartadas()).isEqualTo(1);
        assertThat(result.piezas().getFirst().filas()).extracting(FilaTaller::id).containsExactly("f1", "f3", "f4");
    }

    @Test
    @DisplayName("Después de editar el nombre de un registro, las llamadas siguientes lo nombran con el nuevo")
    void editarRegistroShouldKeepTrackOfTheNewName() {
        ValidacionTaller result = validar(
                editar("e1", "Yerba", Map.of("Producto", "Yerba mate")),
                quitar("e1", "Yerba mate"));

        assertThat(result.acciones()).hasSize(2);
        assertThat(result.acciones().get(1).argumentos()).containsEntry("registro", "f1");
    }

    @Test
    @DisplayName("Una pieza sin filas a la vista no admite cambiar ni quitar registros")
    void registrosShouldBeDiscardedWhenThePieceHasNoVisibleRows() {
        ValidacionTaller result = validar(quitar("Clientes", "Lucía"), editar("Clientes", "Lucía", Map.of("Teléfono", "1")));

        assertThat(result.acciones()).isEmpty();
    }

    private ValidacionTaller validar(LlamadaHerramienta... llamadas) {
        return service.validar(List.of(llamadas), ESTADO);
    }

    private LlamadaHerramienta editar(String pieza, String registro, Object cambios) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("pieza", pieza);
        argumentos.put("registro", registro);
        argumentos.put("cambios", cambios);
        return new LlamadaHerramienta("editar_registro", argumentos);
    }

    private LlamadaHerramienta quitar(String pieza, String registro) {
        return new LlamadaHerramienta("quitar_registro", Map.of("pieza", pieza, "registro", registro));
    }
}
