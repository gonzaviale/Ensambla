package com.ensambla.backend.domain.model.sami.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoYKitsTallerTest {

    private static final ComponenteCatalogo CLIENTES = new ComponenteCatalogo("clients", "Clientes", "cliente",
            "Reuní los datos", List.of(new CampoComponente("name", "Nombre", "text", true, null)));
    private static final ComponenteCatalogo FORMULARIO =
            new ComponenteCatalogo("form", "Formulario", "registro", "A medida", null);
    private static final PiezaKit TURNOS = new PiezaKit("form", "Turnos",
            List.of(new CampoNuevo("Hora", "time", null)), List.of(Map.of("Hora", "10:30")));
    private static final PiezaKit CLIENTES_DEL_KIT = new PiezaKit("clients", "Mis clientas", null, null);
    private static final KitRubro PELUQUERIA = new KitRubro("peluqueria", "Peluquería", null,
            List.of(TURNOS, CLIENTES_DEL_KIT), null, new OpcionesTaller("¿Vendés productos?", List.of("Sí", "No")));
    private static final CatalogoComponentes CATALOGO = new CatalogoComponentes("aviso", List.of(CLIENTES, FORMULARIO));
    private static final KitsRubro KITS = new KitsRubro(List.of(PELUQUERIA));

    @Test
    @DisplayName("Busca un componente por su tipo")
    void buscaUnComponentePorSuTipo() {
        entoncesElCatalogoTiene("form", FORMULARIO);
    }

    @Test
    @DisplayName("Un componente que no existe no se encuentra")
    void unComponenteQueNoExisteNoSeEncuentra() {
        entoncesElCatalogoNoTiene("turnos");
    }

    @Test
    @DisplayName("Un catálogo sin componentes queda vacío en vez de nulo")
    void unCatalogoSinComponentesQuedaVacio() {
        entoncesEstaVacia(new CatalogoComponentes(null, null).componentes());
    }

    @Test
    @DisplayName("La tabla a medida se puede repetir")
    void laTablaAMedidaSePuedeRepetir() {
        entoncesSePuedeRepetir(FORMULARIO, true);
    }

    @Test
    @DisplayName("Un componente de fábrica no se puede repetir")
    void unComponenteDeFabricaNoSePuedeRepetir() {
        entoncesSePuedeRepetir(CLIENTES, false);
    }

    @Test
    @DisplayName("Los datos de fábrica pasan al taller con su etiqueta y su tipo")
    void losDatosDeFabricaPasanAlTallerConSuEtiquetaYTipo() {
        entoncesLosCamposDelTallerSon(CLIENTES, new CampoTaller("Nombre", "text"));
    }

    @Test
    @DisplayName("Un dato de fábrica sin opciones las deja vacías, y un componente sin datos no trae ninguno")
    void unDatoSinOpcionesYUnComponenteSinDatosQuedanVacios() {
        entoncesEstaVacia(CLIENTES.campos().getFirst().opciones());
        entoncesEstaVacia(FORMULARIO.campos());
    }

    @Test
    @DisplayName("El dato que apunta a un componente se llama como una fila, con mayúscula")
    void elDatoQueApuntaAUnComponenteSeLlamaComoUnaFila() {
        entoncesLaEtiquetaDeVinculoEs(CLIENTES, "Cliente");
    }

    @Test
    @DisplayName("Un dato nuevo con opciones las manda en sus argumentos")
    void unDatoNuevoConOpcionesLasManda() {
        entoncesLosArgumentosSon(new CampoNuevo("Cera", "select", List.of("Soja", "Coco")),
                Map.entry("etiqueta", "Cera"), Map.entry("tipo", "select"), Map.entry("opciones", List.of("Soja", "Coco")));
    }

    @Test
    @DisplayName("Un dato nuevo sin opciones no las manda")
    void unDatoNuevoSinOpcionesNoLasManda() {
        entoncesLosArgumentosSon(new CampoNuevo("Aroma", "text", null),
                Map.entry("etiqueta", "Aroma"), Map.entry("tipo", "text"));
    }

    @Test
    @DisplayName("Un dato nuevo se convierte en un campo del taller")
    void unDatoNuevoSeConvierteEnUnCampoDelTaller() {
        entoncesElCampoEs(new CampoNuevo("Aroma", "text", null), new CampoTaller("Aroma", "text"));
    }

    @Test
    @DisplayName("Una pieza de kit se convierte en los argumentos de agregar_pieza")
    void unaPiezaDeKitSeConvierteEnLosArgumentosDeAgregarPieza() {
        entoncesLaPiezaPideAgregar(TURNOS,
                Map.entry("tipo", "form"),
                Map.entry("nombre", "Turnos"),
                Map.entry("campos", List.of(Map.of("etiqueta", "Hora", "tipo", "time"))),
                Map.entry("ejemplos", List.of(Map.of("Hora", "10:30"))));
    }

    @Test
    @DisplayName("Una pieza de kit sin datos ni ejemplos los deja vacíos")
    void unaPiezaDeKitSinDatosNiEjemplosLosDejaVacios() {
        entoncesEstaVacia(CLIENTES_DEL_KIT.campos());
        entoncesEstaVacia(CLIENTES_DEL_KIT.ejemplos());
    }

    @ParameterizedTest
    @CsvSource({"Turnos,Turnos", "Mis clientas,clients", "Inexistente,Inexistente"})
    @DisplayName("En un kit, la tabla a medida se nombra por su nombre y el resto por su tipo")
    void enUnKitLaTablaAMedidaSeNombraPorSuNombre(String pieza, String referencia) {
        entoncesLaReferenciaDe(pieza, referencia);
    }

    @Test
    @DisplayName("Un kit sin términos ni conexiones los deja vacíos")
    void unKitSinTerminosNiConexionesLosDejaVacios() {
        entoncesEstaVacia(PELUQUERIA.terminos());
        entoncesEstaVacia(PELUQUERIA.conexiones());
    }

    @Test
    @DisplayName("Un kit sin piezas y una lista sin kits quedan vacíos")
    void unKitSinPiezasYUnaListaSinKitsQuedanVacios() {
        entoncesEstaVacia(new KitRubro("x", "X", null, null, null, null).piezas());
        entoncesEstaVacia(new KitsRubro(null).kits());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PELUQUERIA", "peluquería"})
    @DisplayName("Encuentra un kit por id o por nombre sin importar mayúsculas")
    void encuentraUnKitPorIdOPorNombre(String buscado) {
        entoncesSeEncuentraLaPeluqueriaCon(buscado);
    }

    @Test
    @DisplayName("Un kit que no existe no se encuentra")
    void unKitQueNoExisteNoSeEncuentra() {
        entoncesNoSeEncuentraElKit("velas");
    }

    private static void entoncesElCatalogoTiene(String tipo, ComponenteCatalogo componente) {
        assertThat(CATALOGO.buscar(tipo)).contains(componente);
    }

    private static void entoncesElCatalogoNoTiene(String tipo) {
        assertThat(CATALOGO.buscar(tipo)).isEmpty();
    }

    private static void entoncesEstaVacia(List<?> lista) {
        assertThat(lista).isEmpty();
    }

    private static void entoncesSePuedeRepetir(ComponenteCatalogo componente, boolean repetible) {
        assertThat(componente.repetible()).isEqualTo(repetible);
    }

    private static void entoncesLosCamposDelTallerSon(ComponenteCatalogo componente, CampoTaller... campos) {
        assertThat(componente.camposTaller()).containsExactly(campos);
    }

    private static void entoncesLaEtiquetaDeVinculoEs(ComponenteCatalogo componente, String etiqueta) {
        assertThat(componente.etiquetaDeVinculo()).isEqualTo(etiqueta);
    }

    @SafeVarargs
    private static void entoncesLosArgumentosSon(CampoNuevo campo, Map.Entry<String, Object>... argumentos) {
        assertThat(campo.comoArgumentos()).containsExactly(argumentos);
    }

    private static void entoncesElCampoEs(CampoNuevo campo, CampoTaller esperado) {
        assertThat(campo.comoCampo()).isEqualTo(esperado);
    }

    @SafeVarargs
    private static void entoncesLaPiezaPideAgregar(PiezaKit pieza, Map.Entry<String, Object>... argumentos) {
        assertThat(pieza.comoArgumentos()).containsExactly(argumentos);
    }

    private static void entoncesLaReferenciaDe(String pieza, String referencia) {
        assertThat(PELUQUERIA.referenciaDe(pieza)).isEqualTo(referencia);
    }

    private static void entoncesSeEncuentraLaPeluqueriaCon(String buscado) {
        assertThat(KITS.buscar(buscado)).contains(PELUQUERIA);
    }

    private static void entoncesNoSeEncuentraElKit(String buscado) {
        assertThat(KITS.buscar(buscado)).isEmpty();
    }
}