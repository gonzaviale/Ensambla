package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.AccionCrearRegistro;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionDesconocida;
import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AccionInvalida;
import com.ensambla.backend.domain.model.aviso.AccionRegistrar;
import com.ensambla.backend.domain.model.aviso.AccionVerZona;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccionDeAvisoJsonTest {

    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Una acción de abrir se lee y se escribe con sus ids")
    void abrirShouldRoundTrip() {
        Map<String, Object> json = abrirJson();

        assertThat(AccionDeAvisoJson.leer(json))
                .isEqualTo(new AccionAbrirRegistro("Ver producto", PROYECTO_ID, BLOQUE_ID, REGISTRO_ID));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Una acción de registrar una entrada se lee y se escribe sin fila")
    void registrarShouldRoundTrip() {
        Map<String, Object> json = Map.of("tipo", "REGISTRAR", "texto", "Registrar entrada",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString());

        assertThat(AccionDeAvisoJson.leer(json))
                .isEqualTo(new AccionRegistrar("Registrar entrada", PROYECTO_ID, BLOQUE_ID));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Una acción de ver la zona se lee y se escribe con el producto")
    void verZonaShouldRoundTrip() {
        Map<String, Object> json = Map.of("tipo", "VER_ZONA", "texto", "Ver en Mi zona", "producto", "yerba");

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionVerZona("Ver en Mi zona", "yerba"));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Una acción de editar se lee y se escribe con la fila y los valores")
    void editarShouldRoundTrip() {
        Map<String, Object> json = Map.of("tipo", "EDITAR_REGISTRO", "texto", "Bajar a $1.300",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString(),
                "registroId", REGISTRO_ID.toString(), "valores", Map.of("price", 1300));

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionEditarRegistro("Bajar a $1.300",
                PROYECTO_ID, BLOQUE_ID, REGISTRO_ID, Map.of("price", 1300)));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Una acción de crear se lee y se escribe con la tabla y los valores")
    void crearShouldRoundTrip() {
        Map<String, Object> json = Map.of("tipo", "CREAR_REGISTRO", "texto", "Crear el combo",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString(),
                "valores", Map.of("name", "Combo"));

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(
                new AccionCrearRegistro("Crear el combo", PROYECTO_ID, BLOQUE_ID, Map.of("name", "Combo")));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Una acción de abrir sin ids se lee sin ellos y se escribe sin inventarlos")
    void abrirWithoutIdsShouldKeepThemAbsent() {
        AccionDeAviso accion = AccionDeAvisoJson.leer(Map.of("tipo", "ABRIR", "texto", "Ver producto"));

        assertThat(accion).isEqualTo(new AccionAbrirRegistro("Ver producto", null, null, null));
        assertThat(AccionDeAvisoJson.escribir(accion)).containsOnlyKeys("tipo", "texto");
    }

    @Test
    @DisplayName("Un id que no es un id se lee como ausente")
    void invalidIdsShouldBeReadAsAbsent() {
        AccionDeAviso accion = AccionDeAvisoJson.leer(
                Map.of("tipo", "ABRIR", "texto", "Ver producto", "bloqueId", "no-es-un-id"));

        assertThat(accion).isEqualTo(new AccionAbrirRegistro("Ver producto", null, null, null));
    }

    @Test
    @DisplayName("Un texto que no es texto se lee como ausente")
    void nonTextTextShouldBeReadAsAbsent() {
        AccionDeAviso accion = AccionDeAvisoJson.leer(Map.of("tipo", "VER_ZONA", "texto", 5, "producto", "yerba"));

        assertThat(accion).isEqualTo(new AccionVerZona(null, "yerba"));
    }

    @Test
    @DisplayName("Editar sin una fila válida queda como acción incompleta que dice qué falta")
    void editarWithoutRegistroShouldBeInvalid() {
        Map<String, Object> json = Map.of("tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id",
                "valores", Map.of("price", 1));

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionInvalida("EDITAR_REGISTRO",
                "La acción del aviso no indica qué registro cambiar", json));
    }

    @Test
    @DisplayName("Editar sin valores queda como acción incompleta que dice qué falta")
    void editarWithoutValoresShouldBeInvalid() {
        Map<String, Object> json = Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString());

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionInvalida("EDITAR_REGISTRO",
                "La acción del aviso no indica qué valores cambiar", json));
    }

    @Test
    @DisplayName("Crear sin una tabla válida queda como acción incompleta que dice qué falta")
    void crearWithoutBloqueShouldBeInvalid() {
        Map<String, Object> json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", "no-es-un-id",
                "valores", Map.of("name", "Combo"));

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionInvalida("CREAR_REGISTRO",
                "La acción del aviso no indica en qué tabla crear la fila", json));
    }

    @Test
    @DisplayName("Crear sin valores queda como acción incompleta que dice qué falta")
    void crearWithoutValoresShouldBeInvalid() {
        Map<String, Object> json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", BLOQUE_ID.toString());

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionInvalida("CREAR_REGISTRO",
                "La acción del aviso no indica qué valores cambiar", json));
    }

    @Test
    @DisplayName("Una acción incompleta se escribe tal como estaba guardada")
    void invalidActionShouldBeWrittenAsItWasStored() {
        Map<String, Object> json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", "no-es-un-id");

        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Un tipo de acción que no se conoce se conserva y se escribe igual")
    void unknownTypeShouldBeKept() {
        Map<String, Object> json = Map.of("tipo", "NUEVA_ACCION", "dato", 1);

        assertThat(AccionDeAvisoJson.leer(json)).isEqualTo(new AccionDesconocida("NUEVA_ACCION", json));
        assertThat(escribirLeyendo(json)).isEqualTo(json);
    }

    @Test
    @DisplayName("Algo que no es una acción se lee como acción desconocida")
    void nonMapShouldBeReadAsUnknown() {
        assertThat(AccionDeAvisoJson.leer("texto suelto")).isEqualTo(new AccionDesconocida("null", Map.of()));
    }

    @Test
    @DisplayName("Los valores con claves que no son texto se pasan a texto")
    void valoresWithNonTextKeysShouldUseTextKeys() {
        Map<Object, Object> valores = new HashMap<>();
        valores.put(7, "siete");

        AccionDeAviso accion = AccionDeAvisoJson.leer(Map.of("tipo", "CREAR_REGISTRO",
                "bloqueId", BLOQUE_ID.toString(), "valores", valores));

        assertThat(accion).isInstanceOfSatisfying(AccionCrearRegistro.class,
                crear -> assertThat(crear.valores()).containsEntry("7", "siete"));
    }

    @Test
    @DisplayName("Una lista de acciones se lee y se escribe en el mismo orden")
    void listsShouldKeepTheOrder() {
        List<Object> json = List.of(abrirJson(), Map.of("tipo", "VER_ZONA", "texto", "Ver", "producto", "yerba"));

        List<AccionDeAviso> acciones = AccionDeAvisoJson.leerTodas(json);

        assertThat(acciones).hasSize(2);
        assertThat(AccionDeAvisoJson.escribirTodas(acciones)).isEqualTo(json);
    }

    private static Map<String, Object> abrirJson() {
        return Map.of("tipo", "ABRIR", "texto", "Ver producto", "proyectoId", PROYECTO_ID.toString(),
                "bloqueId", BLOQUE_ID.toString(), "registroId", REGISTRO_ID.toString());
    }

    private static Map<String, Object> escribirLeyendo(Map<String, Object> json) {
        return AccionDeAvisoJson.escribir(AccionDeAvisoJson.leer(json));
    }
}
