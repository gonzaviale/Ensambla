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
    private static final String SIN_REGISTRO = "La acción del aviso no indica qué registro cambiar";
    private static final String SIN_TABLA = "La acción del aviso no indica en qué tabla crear la fila";
    private static final String SIN_VALORES = "La acción del aviso no indica qué valores cambiar";

    private Object json;
    private List<Object> jsonDeLista;
    private AccionDeAviso accion;
    private Map<String, Object> escrito;
    private List<AccionDeAviso> acciones;
    private List<Object> listaEscrita;

    @Test
    @DisplayName("Una acción de abrir se lee y se escribe con sus ids")
    void unaAccionDeAbrirSeLeeYSeEscribeConSusIds() {
        dadoElJsonDeAbrirUnProducto();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeComoYSeEscribeIgual(
                new AccionAbrirRegistro("Ver producto", PROYECTO_ID, BLOQUE_ID, REGISTRO_ID));
    }

    @Test
    @DisplayName("Una acción de registrar una entrada se lee y se escribe sin fila")
    void unaAccionDeRegistrarUnaEntradaSeLeeYSeEscribeSinFila() {
        dadoElJsonDeRegistrarUnaEntrada();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeComoYSeEscribeIgual(new AccionRegistrar("Registrar entrada", PROYECTO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("Una acción de ver la zona se lee y se escribe con el producto")
    void unaAccionDeVerLaZonaSeLeeYSeEscribeConElProducto() {
        dadoElJsonDeVerYerbaEnLaZona();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeComoYSeEscribeIgual(new AccionVerZona("Ver en Mi zona", "yerba"));
    }

    @Test
    @DisplayName("Una acción de editar se lee y se escribe con la fila y los valores")
    void unaAccionDeEditarSeLeeYSeEscribeConLaFilaYLosValores() {
        dadoElJsonDeBajarElPrecio();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeComoYSeEscribeIgual(new AccionEditarRegistro("Bajar a $1.300",
                PROYECTO_ID, BLOQUE_ID, REGISTRO_ID, Map.of("price", 1300)));
    }

    @Test
    @DisplayName("Una acción de crear se lee y se escribe con la tabla y los valores")
    void unaAccionDeCrearSeLeeYSeEscribeConLaTablaYLosValores() {
        dadoElJsonDeCrearUnCombo();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeComoYSeEscribeIgual(
                new AccionCrearRegistro("Crear el combo", PROYECTO_ID, BLOQUE_ID, Map.of("name", "Combo")));
    }

    @Test
    @DisplayName("Una acción de abrir sin ids se lee sin ellos y se escribe sin inventarlos")
    void unaAccionDeAbrirSinIdsSeLeeSinEllosYSeEscribeSinInventarlos() {
        dadoElJsonDeAbrirUnProductoSinIds();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeLeeSinIdsYSeEscribeSoloConTipoYTexto();
    }

    @Test
    @DisplayName("Un id inválido se lee como ausente")
    void unIdInvalidoSeLeeComoAusente() {
        dadoElJsonDeAbrirUnProductoConUnBloqueQueNoEsUnId();

        cuandoSeLee();

        entoncesLaAccionLeidaEs(new AccionAbrirRegistro("Ver producto", null, null, null));
    }

    @Test
    @DisplayName("Un texto que no es texto se lee como ausente")
    void unTextoQueNoEsTextoSeLeeComoAusente() {
        dadoElJsonDeVerLaZonaConUnTextoNumerico();

        cuandoSeLee();

        entoncesLaAccionLeidaEs(new AccionVerZona(null, "yerba"));
    }

    @Test
    @DisplayName("Editar sin una fila válida queda como acción incompleta que dice qué falta")
    void editarSinUnaFilaValidaQuedaComoAccionIncompletaQueDiceQueFalta() {
        dadoElJsonDeEditarConUnaFilaQueNoEsUnId();

        cuandoSeLee();

        entoncesLaAccionLeidaEsIncompletaPorque("EDITAR_REGISTRO", SIN_REGISTRO);
    }

    @Test
    @DisplayName("Editar sin valores queda como acción incompleta que dice qué falta")
    void editarSinValoresQuedaComoAccionIncompletaQueDiceQueFalta() {
        dadoElJsonDeEditarSinValores();

        cuandoSeLee();

        entoncesLaAccionLeidaEsIncompletaPorque("EDITAR_REGISTRO", SIN_VALORES);
    }

    @Test
    @DisplayName("Crear sin una tabla válida queda como acción incompleta que dice qué falta")
    void crearSinUnaTablaValidaQuedaComoAccionIncompletaQueDiceQueFalta() {
        dadoElJsonDeCrearConUnaTablaQueNoEsUnId();

        cuandoSeLee();

        entoncesLaAccionLeidaEsIncompletaPorque("CREAR_REGISTRO", SIN_TABLA);
    }

    @Test
    @DisplayName("Crear sin valores queda como acción incompleta que dice qué falta")
    void crearSinValoresQuedaComoAccionIncompletaQueDiceQueFalta() {
        dadoElJsonDeCrearSinValores();

        cuandoSeLee();

        entoncesLaAccionLeidaEsIncompletaPorque("CREAR_REGISTRO", SIN_VALORES);
    }

    @Test
    @DisplayName("Una acción incompleta se escribe tal como estaba guardada")
    void unaAccionIncompletaSeEscribeTalComoEstabaGuardada() {
        dadoElJsonDeCrearConUnaTablaQueNoEsUnIdYSinValores();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeEscribeIgualQueElJson();
    }

    @Test
    @DisplayName("Un tipo de acción que no se conoce se conserva y se escribe igual")
    void unTipoDeAccionQueNoSeConoceSeConservaYSeEscribeIgual() {
        dadoElJsonDeUnaAccionNueva();

        cuandoSeLeeYSeVuelveAEscribir();

        entoncesSeConservaComoAccionDesconocidaYSeEscribeIgual();
    }

    @Test
    @DisplayName("Algo que no es una acción se lee como acción desconocida")
    void algoQueNoEsUnaAccionSeLeeComoAccionDesconocida() {
        dadoUnTextoSueltoEnLugarDeUnaAccion();

        cuandoSeLee();

        entoncesLaAccionLeidaEs(new AccionDesconocida("null", Map.of()));
    }

    @Test
    @DisplayName("Los valores con claves que no son texto se pasan a texto")
    void losValoresConClavesQueNoSonTextoSePasanATexto() {
        dadoElJsonDeCrearConUnValorDeClaveNumerica();

        cuandoSeLee();

        entoncesLosValoresTienenClavesDeTexto();
    }

    @Test
    @DisplayName("Una lista de acciones se lee y se escribe en el mismo orden")
    void unaListaDeAccionesSeLeeYSeEscribeEnElMismoOrden() {
        dadoUnaListaConAbrirYVerLaZona();

        cuandoSeLeenYSeVuelvenAEscribirTodas();

        entoncesQuedanDosAccionesEnElMismoOrden();
    }

    // --- dado ---

    private void dadoElJsonDeAbrirUnProducto() {
        json = elJsonDeAbrirUnProducto();
    }

    private void dadoElJsonDeRegistrarUnaEntrada() {
        json = Map.of("tipo", "REGISTRAR", "texto", "Registrar entrada",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString());
    }

    private void dadoElJsonDeVerYerbaEnLaZona() {
        json = elJsonDeVerYerbaEnLaZona();
    }

    private void dadoElJsonDeBajarElPrecio() {
        json = Map.of("tipo", "EDITAR_REGISTRO", "texto", "Bajar a $1.300",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString(),
                "registroId", REGISTRO_ID.toString(), "valores", Map.of("price", 1300));
    }

    private void dadoElJsonDeCrearUnCombo() {
        json = Map.of("tipo", "CREAR_REGISTRO", "texto", "Crear el combo",
                "proyectoId", PROYECTO_ID.toString(), "bloqueId", BLOQUE_ID.toString(),
                "valores", Map.of("name", "Combo"));
    }

    private void dadoElJsonDeAbrirUnProductoSinIds() {
        json = Map.of("tipo", "ABRIR", "texto", "Ver producto");
    }

    private void dadoElJsonDeAbrirUnProductoConUnBloqueQueNoEsUnId() {
        json = Map.of("tipo", "ABRIR", "texto", "Ver producto", "bloqueId", "no-es-un-id");
    }

    private void dadoElJsonDeVerLaZonaConUnTextoNumerico() {
        json = Map.of("tipo", "VER_ZONA", "texto", 5, "producto", "yerba");
    }

    private void dadoElJsonDeEditarConUnaFilaQueNoEsUnId() {
        json = Map.of("tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id", "valores", Map.of("price", 1));
    }

    private void dadoElJsonDeEditarSinValores() {
        json = Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString());
    }

    private void dadoElJsonDeCrearConUnaTablaQueNoEsUnId() {
        json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", "no-es-un-id", "valores", Map.of("name", "Combo"));
    }

    private void dadoElJsonDeCrearSinValores() {
        json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", BLOQUE_ID.toString());
    }

    private void dadoElJsonDeCrearConUnaTablaQueNoEsUnIdYSinValores() {
        json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", "no-es-un-id");
    }

    private void dadoElJsonDeUnaAccionNueva() {
        json = Map.of("tipo", "NUEVA_ACCION", "dato", 1);
    }

    private void dadoUnTextoSueltoEnLugarDeUnaAccion() {
        json = "texto suelto";
    }

    private void dadoElJsonDeCrearConUnValorDeClaveNumerica() {
        Map<Object, Object> valores = new HashMap<>();
        valores.put(7, "siete");
        json = Map.of("tipo", "CREAR_REGISTRO", "bloqueId", BLOQUE_ID.toString(), "valores", valores);
    }

    private void dadoUnaListaConAbrirYVerLaZona() {
        jsonDeLista = List.of(elJsonDeAbrirUnProducto(), elJsonDeVerYerbaEnLaZona());
    }

    // --- cuando ---

    private void cuandoSeLee() {
        accion = AccionDeAvisoJson.leer(json);
    }

    private void cuandoSeLeeYSeVuelveAEscribir() {
        cuandoSeLee();
        escrito = AccionDeAvisoJson.escribir(accion);
    }

    private void cuandoSeLeenYSeVuelvenAEscribirTodas() {
        acciones = AccionDeAvisoJson.leerTodas(jsonDeLista);
        listaEscrita = AccionDeAvisoJson.escribirTodas(acciones);
    }

    // --- entonces ---

    private void entoncesLaAccionLeidaEs(AccionDeAviso esperada) {
        assertThat(accion).isEqualTo(esperada);
    }

    private void entoncesSeEscribeIgualQueElJson() {
        assertThat(escrito).isEqualTo(json);
    }

    private void entoncesSeLeeComoYSeEscribeIgual(AccionDeAviso esperada) {
        entoncesLaAccionLeidaEs(esperada);
        entoncesSeEscribeIgualQueElJson();
    }

    private void entoncesSeLeeSinIdsYSeEscribeSoloConTipoYTexto() {
        entoncesLaAccionLeidaEs(new AccionAbrirRegistro("Ver producto", null, null, null));
        assertThat(escrito).containsOnlyKeys("tipo", "texto");
    }

    private void entoncesLaAccionLeidaEsIncompletaPorque(String tipo, String motivo) {
        entoncesLaAccionLeidaEs(new AccionInvalida(tipo, motivo, comoMapa(json)));
    }

    private void entoncesSeConservaComoAccionDesconocidaYSeEscribeIgual() {
        entoncesLaAccionLeidaEs(new AccionDesconocida("NUEVA_ACCION", comoMapa(json)));
        entoncesSeEscribeIgualQueElJson();
    }

    private void entoncesLosValoresTienenClavesDeTexto() {
        assertThat(accion).isInstanceOfSatisfying(AccionCrearRegistro.class,
                crear -> assertThat(crear.valores()).containsEntry("7", "siete"));
    }

    private void entoncesQuedanDosAccionesEnElMismoOrden() {
        assertThat(acciones).hasSize(2);
        assertThat(listaEscrita).isEqualTo(jsonDeLista);
    }

    // --- apoyo ---

    private static Map<String, Object> elJsonDeAbrirUnProducto() {
        return Map.of("tipo", "ABRIR", "texto", "Ver producto", "proyectoId", PROYECTO_ID.toString(),
                "bloqueId", BLOQUE_ID.toString(), "registroId", REGISTRO_ID.toString());
    }

    private static Map<String, Object> elJsonDeVerYerbaEnLaZona() {
        return Map.of("tipo", "VER_ZONA", "texto", "Ver en Mi zona", "producto", "yerba");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoMapa(Object json) {
        return (Map<String, Object>) json;
    }
}
