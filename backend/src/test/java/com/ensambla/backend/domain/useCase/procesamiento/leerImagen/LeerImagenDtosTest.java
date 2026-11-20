package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenRequest.CampoItem;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

class LeerImagenDtosTest {

    private static final double UMBRAL_CONFIANZA = 0.8;

    private ImagenCuaderno imagen;
    private BloqueDeImagen bloque;
    private LeerImagenItem item;
    private Throwable rechazo;

    @Test
    @DisplayName("Arma la imagen con el tipo detectado")
    void armaLaImagenConElTipoDetectado() {
        LeerImagenRequest pedido = unPedido(ImagenTestData.png(), unCampo("importe", "Monto", "NUMERO"));

        cuandoSeArmaLaImagen(pedido);

        entoncesLaImagenEsUnPngConElContenidoDelPedido(pedido);
    }

    @Test
    @DisplayName("No arma una imagen inválida")
    void noArmaUnaImagenInvalida() {
        cuandoSeIntentaArmarLaImagen(unPedido(new byte[]{1, 2, 3}, unCampo("importe", "Monto", "NUMERO")));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Arma el bloque con el tipo de cada campo y sus opciones")
    void armaElBloqueConElTipoDeCadaCampoYSusOpciones() {
        cuandoSeArmaElBloque(unPedido(ImagenTestData.jpeg(), unCampo("importe", "Monto", "NUMERO"),
                new CampoItem("estado", "Estado", "OPCION", List.of("Pagó", "Debe"))));

        entoncesElBloqueTieneLosCamposConSuTipoYSusOpciones();
    }

    @Test
    @DisplayName("No arma un bloque sin campos")
    void noArmaUnBloqueSinCampos() {
        cuandoSeIntentaArmarElBloque(unPedido(ImagenTestData.jpeg()));

        entoncesSeRechazaPorFaltaDeDatos();
    }

    @Test
    @DisplayName("Marca como dudoso el dato que no llega al umbral de confianza y devuelve las columnas nuevas")
    void marcaComoDudosoElDatoQueNoLlegaAlUmbralDeConfianzaYDevuelveLasColumnasNuevas() {
        cuandoSeArmaElItemDeUnaLecturaConUnDatoDudoso();

        entoncesElDatoDudosoVaMarcadoYSeDevuelvenLasColumnasNuevasYLosAvisos();
    }

    // --- dado ---
    private LeerImagenRequest unPedido(byte[] contenido, CampoItem... campos) {
        return new LeerImagenRequest(UsuarioId.nuevo(), contenido, "Ventas", List.of(campos), true);
    }

    private CampoItem unCampo(String clave, String nombre, String tipo) {
        return new CampoItem(clave, nombre, tipo, null);
    }

    // --- cuando ---
    private void cuandoSeArmaLaImagen(LeerImagenRequest pedido) {
        imagen = pedido.aImagenCuaderno();
    }

    private void cuandoSeIntentaArmarLaImagen(LeerImagenRequest pedido) {
        rechazo = catchThrowable(() -> cuandoSeArmaLaImagen(pedido));
    }

    private void cuandoSeArmaElBloque(LeerImagenRequest pedido) {
        bloque = pedido.aBloqueDeImagen();
    }

    private void cuandoSeIntentaArmarElBloque(LeerImagenRequest pedido) {
        rechazo = catchThrowable(() -> cuandoSeArmaElBloque(pedido));
    }

    private void cuandoSeArmaElItemDeUnaLecturaConUnDatoDudoso() {
        LecturaImagen lectura = new LecturaImagen(
                List.of(Map.of("importe", new DatoLeido("1500", 0.95), "cliente", new DatoLeido("Ana", 0.3)),
                        Map.of("importe", new DatoLeido(null, 0.8))),
                List.of(new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of("A"))),
                List.of("Página torcida"));
        item = LeerImagenItem.de(lectura, UMBRAL_CONFIANZA);
    }

    // --- entonces ---
    private void entoncesLaImagenEsUnPngConElContenidoDelPedido(LeerImagenRequest pedido) {
        assertThat(imagen.contenido()).isEqualTo(pedido.contenido());
        assertThat(imagen.tipoContenido()).isEqualTo("image/png");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesElBloqueTieneLosCamposConSuTipoYSusOpciones() {
        assertThat(bloque.nombre()).isEqualTo("Ventas");
        assertThat(bloque.campos()).extracting("clave", "nombre", "tipo", "opciones").containsExactly(
                tuple("importe", "Monto", TipoCampo.NUMERO, List.of()),
                tuple("estado", "Estado", TipoCampo.OPCION, List.of("Pagó", "Debe")));
    }

    private void entoncesSeRechazaPorFaltaDeDatos() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining("no tiene datos");
    }

    private void entoncesElDatoDudosoVaMarcadoYSeDevuelvenLasColumnasNuevasYLosAvisos() {
        assertThat(item.filas()).hasSize(2);
        assertThat(item.filas().get(0).get("importe")).isEqualTo(new LeerImagenItem.DatoItem("1500", false));
        assertThat(item.filas().get(0).get("cliente")).isEqualTo(new LeerImagenItem.DatoItem("Ana", true));
        assertThat(item.filas().get(1).get("importe")).isEqualTo(new LeerImagenItem.DatoItem(null, false));
        assertThat(item.camposNuevos()).containsExactly(
                new LeerImagenItem.CampoItem("telefono", "Teléfono", "TEXTO", List.of("A")));
        assertThat(item.advertencias()).containsExactly("Página torcida");
    }
}
