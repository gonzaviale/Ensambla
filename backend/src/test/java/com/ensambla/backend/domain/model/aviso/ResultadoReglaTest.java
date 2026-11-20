package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoReglaTest {

    private static final RegistroId MENOR = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId MAYOR = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    private String entidad;
    private String otraEntidad;
    private Optional<RegistroId> registro;
    private String huella;
    private String otraHuella;

    @Test
    @DisplayName("La entidad de un registro lleva el prefijo registro")
    void laEntidadDeUnRegistroLlevaElPrefijoRegistro() {
        cuandoSeArmaLaEntidadDelRegistro();

        entoncesLaEntidadEs("registro:" + MENOR);
    }

    @Test
    @DisplayName("La entidad de un combo es la misma sin importar el orden de los productos")
    void laEntidadDeUnComboEsLaMismaSinImportarElOrdenDeLosProductos() {
        cuandoSeArmaLaEntidadDelComboEnAmbosOrdenes();

        entoncesAmbasEntidadesSon("combo:" + MENOR + ":" + MAYOR);
    }

    @Test
    @DisplayName("La entidad de un producto de la zona es su nombre sin tildes ni mayúsculas")
    void laEntidadDeUnProductoDeLaZonaEsSuNombreSinTildesNiMayusculas() {
        cuandoSeArmaLaEntidadDeZonaDeDosEscrituras("Café La Virginia 250 g", "CAFE LA VIRGINIA 250 G");

        entoncesAmbasEntidadesCoinciden();
    }

    @Test
    @DisplayName("La entidad de un producto de la zona no apunta a un registro")
    void laEntidadDeUnProductoDeLaZonaNoApuntaAUnRegistro() {
        cuandoSeArmaLaEntidadDeZona("Café");

        entoncesLaEntidadEmpiezaCon("zona:");
        entoncesNoApuntaAUnRegistro();
    }

    @Test
    @DisplayName("La entidad de un combo no apunta a un producto solo")
    void laEntidadDeUnComboNoApuntaAUnProductoSolo() {
        cuandoSeArmaLaEntidadDelComboEnAmbosOrdenes();

        entoncesNoApuntaAUnRegistro();
    }

    @Test
    @DisplayName("Del texto de una entidad de registro se recupera el registro")
    void delTextoDeUnaEntidadDeRegistroSeRecuperaElRegistro() {
        cuandoSeLeeElRegistroDe("registro:" + MENOR);

        entoncesSeRecuperaElRegistro();
    }

    @Test
    @DisplayName("Una entidad ausente no tiene registro")
    void unaEntidadAusenteNoTieneRegistro() {
        cuandoSeLeeElRegistroDe(null);

        entoncesNoHayRegistro();
    }

    @Test
    @DisplayName("Una entidad que no es un registro no tiene registro")
    void unaEntidadQueNoEsUnRegistroNoTieneRegistro() {
        cuandoSeLeeElRegistroDe("zona:yerba");

        entoncesNoHayRegistro();
    }

    @Test
    @DisplayName("Una entidad de registro con un id inválido no tiene registro")
    void unaEntidadDeRegistroConUnIdInvalidoNoTieneRegistro() {
        cuandoSeLeeElRegistroDe("registro:no-es-un-uuid");

        entoncesNoHayRegistro();
    }

    @Test
    @DisplayName("La huella empieza con el tipo y la entidad")
    void laHuellaEmpiezaConElTipoYLaEntidad() {
        cuandoSeCalculaLaHuellaDeStockConCantidad(BigDecimal.TWO);

        entoncesLaHuellaEmpiezaCon("STOCK:registro:1:");
    }

    @Test
    @DisplayName("La huella no depende del orden de los datos ni de ceros decimales")
    void laHuellaNoDependeDelOrdenDeLosDatosNiDeCerosDecimales() {
        cuandoSeCalculaLaHuellaDeLosMismosDatosEnOtroOrdenYConCerosDeMas();

        entoncesLasHuellasCoinciden();
    }

    @Test
    @DisplayName("La huella cambia cuando cambian los datos de origen")
    void laHuellaCambiaCuandoCambianLosDatosDeOrigen() {
        cuandoSeCalculaLaHuellaConCantidadesDistintas();

        entoncesLasHuellasDifieren();
    }

    // --- cuando ---

    private void cuandoSeArmaLaEntidadDelRegistro() {
        entidad = ResultadoRegla.entidadDeRegistro(MENOR);
    }

    private void cuandoSeArmaLaEntidadDelComboEnAmbosOrdenes() {
        entidad = ResultadoRegla.entidadDeCombo(MENOR, MAYOR);
        otraEntidad = ResultadoRegla.entidadDeCombo(MAYOR, MENOR);
    }

    private void cuandoSeArmaLaEntidadDeZona(String nombre) {
        entidad = ResultadoRegla.entidadDeZona(nombre);
    }

    private void cuandoSeArmaLaEntidadDeZonaDeDosEscrituras(String nombre, String otroNombre) {
        entidad = ResultadoRegla.entidadDeZona(nombre);
        otraEntidad = ResultadoRegla.entidadDeZona(otroNombre);
    }

    private void cuandoSeLeeElRegistroDe(String texto) {
        registro = ResultadoRegla.registroDe(texto);
    }

    private void cuandoSeCalculaLaHuellaDeStockConCantidad(BigDecimal cantidad) {
        huella = unResultado(Map.of("cantidad", cantidad)).huellaPara(TipoAviso.STOCK);
    }

    private void cuandoSeCalculaLaHuellaDeLosMismosDatosEnOtroOrdenYConCerosDeMas() {
        Map<String, Object> uno = new LinkedHashMap<>();
        uno.put("cantidad", new BigDecimal("2.0"));
        uno.put("minimo", 5);
        Map<String, Object> otro = new LinkedHashMap<>();
        otro.put("minimo", 5);
        otro.put("cantidad", new BigDecimal("2"));
        huella = unResultado(uno).huellaPara(TipoAviso.STOCK);
        otraHuella = unResultado(otro).huellaPara(TipoAviso.STOCK);
    }

    private void cuandoSeCalculaLaHuellaConCantidadesDistintas() {
        huella = unResultado(Map.of("cantidad", new BigDecimal("2"))).huellaPara(TipoAviso.STOCK);
        otraHuella = unResultado(Map.of("cantidad", new BigDecimal("1"))).huellaPara(TipoAviso.STOCK);
    }

    // --- entonces ---

    private void entoncesLaEntidadEs(String esperada) {
        assertThat(entidad).isEqualTo(esperada);
    }

    private void entoncesLaEntidadEmpiezaCon(String prefijo) {
        assertThat(entidad).startsWith(prefijo);
    }

    private void entoncesAmbasEntidadesSon(String esperada) {
        assertThat(entidad).isEqualTo(esperada);
        assertThat(otraEntidad).isEqualTo(esperada);
    }

    private void entoncesAmbasEntidadesCoinciden() {
        assertThat(entidad).isEqualTo(otraEntidad);
    }

    private void entoncesNoApuntaAUnRegistro() {
        assertThat(ResultadoRegla.registroDe(entidad)).isEmpty();
    }

    private void entoncesSeRecuperaElRegistro() {
        assertThat(registro).contains(MENOR);
    }

    private void entoncesNoHayRegistro() {
        assertThat(registro).isEmpty();
    }

    private void entoncesLaHuellaEmpiezaCon(String prefijo) {
        assertThat(huella).startsWith(prefijo);
    }

    private void entoncesLasHuellasCoinciden() {
        assertThat(huella).isEqualTo(otraHuella);
    }

    private void entoncesLasHuellasDifieren() {
        assertThat(huella).isNotEqualTo(otraHuella);
    }

    // --- datos ---

    private static ResultadoRegla unResultado(Map<String, Object> datos) {
        return new ResultadoRegla(null, "registro:1", (short) 2, "titulo", "detalle", datos, List.of(), List.of());
    }
}
