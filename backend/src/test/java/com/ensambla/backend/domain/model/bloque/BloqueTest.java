package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BloqueTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private Bloque bloque;
    private Campo precio;
    private Campo campoResultante;
    private Throwable rechazo;

    @Test
    @DisplayName("Un bloque nuevo tiene el nombre normalizado, no tiene campos y sus dos fechas son iguales")
    void unBloqueNuevoTieneElNombreNormalizadoSinCamposYConLasDosFechasIguales() {
        cuandoSeCreaUnBloqueNuevoLlamado("  Clientes  ");

        entoncesElBloqueNuevoSeLlamaClientesSinCamposYConLasDosFechasIguales();
    }

    @Test
    @DisplayName("Renombrar conserva la fecha de creación y actualiza la de modificación")
    void renombrarConservaLaFechaDeCreacionYActualizaLaDeModificacion() {
        dadoUnBloqueLlamado("Clientes");

        cuandoSeRenombraComo("  Compradores ");

        entoncesElBloqueSeLlamaCompradoresYSoloCambiaLaFechaDeModificacion();
    }

    @Test
    @DisplayName("El campo nuevo va después del último y pertenece al bloque y al negocio del bloque")
    void elCampoNuevoVaDespuesDelUltimoEnElBloqueYElNegocioDelBloque() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeAgregaElCampo(" Stock ");

        entoncesElCampoQuedaDespuesDelUltimoConElNombreStock();
    }

    @Test
    @DisplayName("El primer campo de un bloque vacío va en la posición cero")
    void elPrimerCampoDeUnBloqueVacioVaEnLaPosicionCero() {
        dadoUnBloqueSinCampos();

        cuandoSeAgregaElCampo("Producto");

        entoncesElCampoQuedaEnLaPosicion(0);
    }

    @Test
    @DisplayName("Un nombre que ya usa otro campo se rechaza sin distinguir mayúsculas ni espacios")
    void unNombreQueYaUsaOtroCampoSeRechazaSinDistinguirMayusculasNiEspacios() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeIntentaAgregarElCampo("  PRECIO ");

        entoncesSeRechazaPorNombreRepetido("PRECIO");
    }

    @Test
    @DisplayName("Una configuración inválida se informa antes que un nombre repetido")
    void unaConfiguracionInvalidaSeInformaAntesQueUnNombreRepetido() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeIntentaAgregarUnCampoDeTipoInvalidoLlamado("Precio");

        entoncesSeRechazaPorConfiguracionInvalida();
    }

    @Test
    @DisplayName("Editar permite que el campo conserve su propio nombre con otras mayúsculas")
    void editarPermiteQueElCampoConserveSuPropioNombreConOtrasMayusculas() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeEditaElPrecioLlamado("precio");

        entoncesElCampoEditadoSeLlamaPrecioYSeActualizoUnaHoraDespues();
    }

    @Test
    @DisplayName("Editar con el nombre de otro campo se rechaza y no modifica el campo")
    void editarConElNombreDeOtroCampoSeRechazaYNoModificaElCampo() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeIntentaEditarElPrecioLlamado("Producto");

        entoncesSeRechazaPorNombreRepetido("Producto");
        entoncesElPrecioSigueIgual();
    }

    @Test
    @DisplayName("Editar con una configuración inválida no modifica el campo")
    void editarConUnaConfiguracionInvalidaNoModificaElCampo() {
        dadoUnBloqueConProductoYPrecio();

        cuandoSeIntentaEditarElPrecioConUnaOpcionSinOpciones();

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesElPrecioSigueIgual();
    }

    private void dadoUnBloqueLlamado(String nombre) {
        bloque = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, nombre, (short) 0, AHORA);
    }

    private void dadoUnBloqueSinCampos() {
        bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
    }

    private void dadoUnBloqueConProductoYPrecio() {
        Campo producto = unCampoDelBloque("Producto", 0);
        precio = unCampoDelBloque("Precio", 3);
        bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).campos(List.of(producto, precio)).build();
    }

    private Campo unCampoDelBloque(String nombre, int posicion) {
        return unCampoNumerico(nombre).ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) posicion, AHORA);
    }

    private void cuandoSeCreaUnBloqueNuevoLlamado(String nombre) {
        bloque = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, nombre, (short) 3, AHORA);
    }

    private void cuandoSeRenombraComo(String nombre) {
        bloque.renombrar(nombre, AHORA.plusHours(1));
    }

    private void cuandoSeAgregaElCampo(String nombre) {
        campoResultante = bloque.agregarCampo(unaConfiguracionDeTexto(nombre), AHORA);
    }

    private void cuandoSeIntentaAgregarElCampo(String nombre) {
        rechazo = catchThrowable(() -> bloque.agregarCampo(unaConfiguracionDeTexto(nombre), AHORA));
    }

    private void cuandoSeIntentaAgregarUnCampoDeTipoInvalidoLlamado(String nombre) {
        rechazo = catchThrowable(() -> bloque.agregarCampo(unaConfiguracionDeTipoInvalido(nombre), AHORA));
    }

    private void cuandoSeEditaElPrecioLlamado(String nombre) {
        campoResultante = bloque.editarCampo(precio, unaConfiguracionDeTexto(nombre), AHORA.plusHours(1));
    }

    private void cuandoSeIntentaEditarElPrecioLlamado(String nombre) {
        rechazo = catchThrowable(() -> bloque.editarCampo(precio, unaConfiguracionDeTexto(nombre), AHORA));
    }

    private void cuandoSeIntentaEditarElPrecioConUnaOpcionSinOpciones() {
        rechazo = catchThrowable(() -> bloque.editarCampo(precio, unaOpcionSinOpciones("Costo"), AHORA));
    }

    private void entoncesElBloqueNuevoSeLlamaClientesSinCamposYConLasDosFechasIguales() {
        assertThat(bloque.getId()).isNull();
        assertThat(bloque.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(bloque.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(bloque.getNombre()).isEqualTo("Clientes");
        assertThat(bloque.getPosicion()).isEqualTo((short) 3);
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getCampos()).isEmpty();
    }

    private void entoncesElBloqueSeLlamaCompradoresYSoloCambiaLaFechaDeModificacion() {
        assertThat(bloque.getNombre()).isEqualTo("Compradores");
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    private void entoncesElCampoQuedaDespuesDelUltimoConElNombreStock() {
        assertThat(campoResultante.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campoResultante.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(campoResultante.getNombre()).isEqualTo("Stock");
        assertThat(campoResultante.getPosicion()).isEqualTo((short) 4);
        assertThat(campoResultante.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElCampoQuedaEnLaPosicion(int posicion) {
        assertThat(campoResultante.getPosicion()).isEqualTo((short) posicion);
    }

    private void entoncesElCampoEditadoSeLlamaPrecioYSeActualizoUnaHoraDespues() {
        assertThat(campoResultante.getNombre()).isEqualTo("precio");
        assertThat(campoResultante.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    private void entoncesSeRechazaPorNombreRepetido(String nombre) {
        assertThat(rechazo)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya hay un campo llamado \"" + nombre + "\" en este bloque");
    }

    private void entoncesSeRechazaPorConfiguracionInvalida() {
        assertThat(rechazo).isInstanceOf(CampoInvalidoException.class);
    }

    private void entoncesElPrecioSigueIgual() {
        assertThat(precio.getNombre()).isEqualTo("Precio");
        assertThat(precio.getTipo()).isEqualTo(TipoCampo.NUMERO);
    }

    private ConfiguracionCampo unaConfiguracionDeTexto(String nombre) {
        return new ConfiguracionCampo(nombre, "TEXTO", null, null, null, null, null);
    }

    private ConfiguracionCampo unaConfiguracionDeTipoInvalido(String nombre) {
        return new ConfiguracionCampo(nombre, "ARCHIVO", null, null, null, null, null);
    }

    private ConfiguracionCampo unaOpcionSinOpciones(String nombre) {
        return new ConfiguracionCampo(nombre, "OPCION", null, null, List.of(), null, null);
    }
}
