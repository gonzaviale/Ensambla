package com.ensambla.backend.domain.model.sami.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ModelosTallerTest {

    private LlamadaHerramienta llamada;

    @Test
    @DisplayName("Una pieza sin datos, registros ni nombres los deja vacíos en vez de nulos")
    void unaPiezaSinListasLasDejaVacias() {
        entoncesEstaVacia(new PiezaTaller("e1", "inventory", "Inventario", null, null).campos());
        entoncesEstaVacia(new PiezaTaller("e1", "inventory", "Inventario", null, null, null).registros());
        entoncesEstaVacia(new PiezaTaller("e1", "inventory", "Inventario", null, null, null, null).nombres());
    }

    @Test
    @DisplayName("Un estado del taller sin piezas y una llamada sin argumentos quedan vacíos")
    void unEstadoSinPiezasYUnaLlamadaSinArgumentosQuedanVacios() {
        entoncesEstaVacia(new EstadoTaller("Sistema", "mosaic", null).piezas());
        entoncesNoTieneArgumentos(new LlamadaHerramienta("agregar_pieza", null));
    }

    @Test
    @DisplayName("Los argumentos de una llamada admiten valores nulos")
    void losArgumentosDeUnaLlamadaAdmitenValoresNulos() {
        cuandoSeLlamaConUnTipoNulo();

        entoncesElTipoEsNulo();
    }

    @Test
    @DisplayName("Los argumentos de una llamada no se pueden modificar")
    void losArgumentosDeUnaLlamadaNoSePuedenModificar() {
        cuandoSeLlamaConUnTipoNulo();

        entoncesNoSePuedenAgregarArgumentos();
    }

    @Test
    @DisplayName("Encuentra una herramienta por su nombre")
    void encuentraUnaHerramientaPorSuNombre() {
        entoncesLaHerramientaDeNombre("ofrecer_opciones", HerramientaTaller.OFRECER_OPCIONES);
        entoncesLaHerramientaDeNombreSeLlama("agregar_pieza");
    }

    @Test
    @DisplayName("Una herramienta que no existe no se encuentra")
    void unaHerramientaQueNoExisteNoSeEncuentra() {
        entoncesNoHayHerramienta("inventada");
    }

    @Test
    @DisplayName("Los vínculos no se suman como un dato más: sólo con conectar")
    void losVinculosNoSeSumanComoUnDatoMas() {
        entoncesLosTiposDeDatoNoIncluyenElVinculo();
    }

    @Test
    @DisplayName("Una consulta armada sin kit lo deja vacío")
    void unaConsultaArmadaSinKitLoDejaVacio() {
        entoncesNoTieneKit(new ConsultaTaller("hola", new EstadoTaller("S", "mosaic", null), List.of()));
    }

    private void cuandoSeLlamaConUnTipoNulo() {
        Map<String, Object> argumentos = new HashMap<>();
        argumentos.put("tipo", null);
        llamada = new LlamadaHerramienta("agregar_pieza", argumentos);
    }

    private void entoncesElTipoEsNulo() {
        assertThat(llamada.argumentos()).containsEntry("tipo", null);
    }

    private void entoncesNoSePuedenAgregarArgumentos() {
        assertThat(catchThrowable(() -> llamada.argumentos().put("otro", "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static void entoncesEstaVacia(List<?> lista) {
        assertThat(lista).isEmpty();
    }

    private static void entoncesNoTieneArgumentos(LlamadaHerramienta sinArgumentos) {
        assertThat(sinArgumentos.argumentos()).isEmpty();
    }

    private static void entoncesLaHerramientaDeNombre(String nombre, HerramientaTaller esperada) {
        assertThat(HerramientaTaller.deNombre(nombre)).contains(esperada);
    }

    private static void entoncesLaHerramientaDeNombreSeLlama(String nombre) {
        assertThat(HerramientaTaller.deNombre(nombre).orElseThrow().nombre()).isEqualTo(nombre);
    }

    private static void entoncesNoHayHerramienta(String nombre) {
        assertThat(HerramientaTaller.deNombre(nombre)).isEmpty();
    }

    private static void entoncesLosTiposDeDatoNoIncluyenElVinculo() {
        assertThat(CatalogoTaller.TIPOS_DE_CAMPO).contains("textarea", "time").doesNotContain(CatalogoTaller.TIPO_VINCULO);
    }

    private static void entoncesNoTieneKit(ConsultaTaller consulta) {
        assertThat(consulta.kit()).isNull();
    }
}