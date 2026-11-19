package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ensambla.backend.support.RecetasDePrueba.AHORA;
import static com.ensambla.backend.support.RecetasDePrueba.AZUCAR_ID;
import static com.ensambla.backend.support.RecetasDePrueba.HARINA_ID;
import static com.ensambla.backend.support.RecetasDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRECIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRODUCTO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.unaLinea;
import static com.ensambla.backend.support.RecetasDePrueba.unasLineasConInsumosDistintos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RecetaTest {

    private static final RecetaId ID_GUARDADA = RecetaId.nuevo();
    private static final OffsetDateTime CREADA_ANTES = AHORA.minusDays(3);

    private Receta receta;
    private Receta guardada;
    private Map<String, String> errores;
    private Throwable rechazo;

    @Test
    @DisplayName("Una receta válida se arma y deja la unidad sin espacios en los bordes")
    void unaRecetaValidaSeArmaYDejaLaUnidadSinEspaciosEnLosBordes() {
        cuandoSeArmaLaReceta(PRECIO_ID, List.of(
                unaLinea(HARINA_ID, "0.5", " kg "),
                unaLinea(AZUCAR_ID, "2", "  ")));

        entoncesLaRecetaTieneLosDatosPedidosYLasUnidadesLimpias();
    }

    @Test
    @DisplayName("Una receta sin líneas es vacía y no guarda la columna del precio")
    void unaRecetaSinLineasEsVaciaYNoGuardaLaColumnaDelPrecio() {
        cuandoSeArmaLaReceta(PRECIO_ID, List.of());

        entoncesLaRecetaEsVaciaYNoTieneColumnaDelPrecio();
    }

    @Test
    @DisplayName("Con líneas se pide la columna del precio")
    void conLineasSePideLaColumnaDelPrecio() {
        cuandoSeIntentaArmarLaReceta(null, List.of(unaLinea(HARINA_ID, "1", null)));

        entoncesSeRechazaConElError("campoPrecioId", "Elegí la columna con el precio de los insumos");
    }

    @Test
    @DisplayName("Se rechaza un insumo faltante, el propio producto y un insumo repetido")
    void seRechazaUnInsumoFaltanteElPropioProductoYUnInsumoRepetido() {
        cuandoSeIntentaArmarLaReceta(PRECIO_ID, List.of(
                unaLinea(null, "1", null),
                unaLinea(PRODUCTO_ID, "1", null),
                unaLinea(HARINA_ID, "1", null),
                unaLinea(HARINA_ID, "1", null)));

        entoncesSeRechazaConLosErrores(Map.of(
                "lineas[0]", "Elegí el insumo",
                "lineas[1]", "Un producto no puede ser insumo de sí mismo",
                "lineas[3]", "Ese insumo está repetido"));
    }

    @Test
    @DisplayName("Se rechaza una cantidad ausente")
    void seRechazaUnaCantidadAusente() {
        cuandoSeIntentaArmarLaRecetaConLaLinea(new LineaReceta(HARINA_ID, null, null));

        entoncesLaLineaSeRechazaPorque("Escribí la cantidad");
    }

    @Test
    @DisplayName("Se rechaza una cantidad en cero")
    void seRechazaUnaCantidadEnCero() {
        cuandoSeIntentaArmarLaRecetaConLaLinea(new LineaReceta(HARINA_ID, BigDecimal.ZERO, null));

        entoncesLaLineaSeRechazaPorque("La cantidad tiene que ser mayor a cero");
    }

    @Test
    @DisplayName("Se rechaza una cantidad con más de tres decimales")
    void seRechazaUnaCantidadConMasDeTresDecimales() {
        cuandoSeIntentaArmarLaRecetaConLaLinea(unaLinea(HARINA_ID, "0.0001", null));

        entoncesLaLineaSeRechazaPorque("La cantidad admite hasta 3 decimales");
    }

    @Test
    @DisplayName("Se rechaza una cantidad demasiado grande")
    void seRechazaUnaCantidadDemasiadoGrande() {
        cuandoSeIntentaArmarLaRecetaConLaLinea(unaLinea(HARINA_ID, "100000000000", null));

        entoncesLaLineaSeRechazaPorque("La cantidad es demasiado grande");
    }

    @Test
    @DisplayName("Se acepta la cantidad máxima y los ceros de más en los decimales")
    void seAceptaLaCantidadMaximaYLosCerosDeMasEnLosDecimales() {
        cuandoSeArmaLaReceta(PRECIO_ID, List.of(
                new LineaReceta(HARINA_ID, Receta.CANTIDAD_MAXIMA, null),
                unaLinea(AZUCAR_ID, "1.50000", null)));

        entoncesLaRecetaTieneLineas(2);
    }

    @Test
    @DisplayName("Se rechaza una unidad demasiado larga")
    void seRechazaUnaUnidadDemasiadoLarga() {
        cuandoSeIntentaArmarLaRecetaConLaLinea(unaLinea(HARINA_ID, "1", "x".repeat(31)));

        entoncesLaLineaSeRechazaPorque("La unidad puede tener hasta 30 caracteres");
    }

    @Test
    @DisplayName("Se rechaza una receta con demasiados insumos")
    void seRechazaUnaRecetaConDemasiadosInsumos() {
        cuandoSeIntentaArmarLaReceta(PRECIO_ID, unasLineasConInsumosDistintos(51));

        entoncesSeRechazaConElError("lineas", "Una receta puede tener hasta 50 insumos");
    }

    @Test
    @DisplayName("Una receta inválida lanza una excepción con los errores")
    void unaRecetaInvalidaLanzaUnaExcepcionConLosErrores() {
        cuandoSeIntentaArmarLaReceta(null, List.of(unaLinea(HARINA_ID, "1", null)));

        entoncesSeRechazaPorDatosInvalidos();
    }

    @Test
    @DisplayName("Reemplazar conserva el id y la fecha de creación de la receta guardada")
    void reemplazarConservaElIdYLaFechaDeCreacionDeLaRecetaGuardada() {
        dadoUnaRecetaNuevaSinLineas();
        dadoUnaRecetaGuardadaCreadaAntes();

        cuandoLaRecetaNuevaReemplazaALaGuardada();

        entoncesLaRecetaNuevaConservaLaIdentidadDeLaGuardada();
    }

    @Test
    @DisplayName("Una receta vacía de un producto no tiene líneas ni se guardó")
    void unaRecetaVaciaDeUnProductoNoTieneLineasNiSeGuardo() {
        cuandoSeArmaUnaRecetaVaciaDelProducto();

        entoncesLaRecetaEsVaciaYSoloConoceAlNegocioYAlProducto();
    }

    @Test
    @DisplayName("Los insumos que ya no existen se marcan en la línea que los usa")
    void losInsumosQueYaNoExistenSeMarcanEnLaLineaQueLosUsa() {
        dadoUnaRecetaDeHarinaYAzucar();

        cuandoSePidenLosErroresDeInsumosInexistentes(Set.of(HARINA_ID));

        entoncesSoloLaSegundaLineaTieneElError("Ese insumo ya no existe");
    }

    @Test
    @DisplayName("Si todos los insumos existen no hay errores de insumos")
    void siTodosLosInsumosExistenNoHayErroresDeInsumos() {
        dadoUnaRecetaDeHarinaYAzucar();

        cuandoSePidenLosErroresDeInsumosInexistentes(Set.of(HARINA_ID, AZUCAR_ID));

        entoncesNoHayErrores();
    }

    // --- dado ---
    private void dadoUnaRecetaNuevaSinLineas() {
        receta = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(), AHORA);
    }

    private void dadoUnaRecetaGuardadaCreadaAntes() {
        guardada = Receta.builder().id(ID_GUARDADA).creadoEn(CREADA_ANTES).build();
    }

    private void dadoUnaRecetaDeHarinaYAzucar() {
        receta = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID,
                List.of(unaLinea(HARINA_ID, "1", null), unaLinea(AZUCAR_ID, "1", null)), AHORA);
    }

    // --- cuando ---
    private void cuandoSeArmaLaReceta(CampoId campoPrecioId, List<LineaReceta> lineas) {
        receta = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, campoPrecioId, lineas, AHORA);
    }

    private void cuandoSeIntentaArmarLaReceta(CampoId campoPrecioId, List<LineaReceta> lineas) {
        rechazo = catchThrowable(() -> cuandoSeArmaLaReceta(campoPrecioId, lineas));
    }

    private void cuandoSeIntentaArmarLaRecetaConLaLinea(LineaReceta linea) {
        cuandoSeIntentaArmarLaReceta(PRECIO_ID, List.of(linea));
    }

    private void cuandoLaRecetaNuevaReemplazaALaGuardada() {
        receta.reemplazar(guardada);
    }

    private void cuandoSeArmaUnaRecetaVaciaDelProducto() {
        receta = Receta.vacia(NEGOCIO_ID, PRODUCTO_ID);
    }

    private void cuandoSePidenLosErroresDeInsumosInexistentes(Set<RegistroId> existentes) {
        errores = receta.erroresDeInsumosInexistentes(existentes);
    }

    // --- entonces ---
    private void entoncesLaRecetaTieneLosDatosPedidosYLasUnidadesLimpias() {
        assertThat(receta.getId()).isNull();
        assertThat(receta.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(receta.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(receta.getCampoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(receta.getLineas()).extracting(LineaReceta::unidad).containsExactly("kg", null);
        assertThat(receta.getCreadoEn()).isEqualTo(AHORA);
        assertThat(receta.esVacia()).isFalse();
        assertThat(receta.insumoIds()).containsExactly(HARINA_ID, AZUCAR_ID);
    }

    private void entoncesLaRecetaEsVaciaYNoTieneColumnaDelPrecio() {
        assertThat(receta.esVacia()).isTrue();
        assertThat(receta.getCampoPrecioId()).isNull();
    }

    private void entoncesLaRecetaTieneLineas(int cantidad) {
        assertThat(receta.getLineas()).hasSize(cantidad);
    }

    private void entoncesSeRechazaConElError(String clave, String mensaje) {
        assertThat(erroresDelRechazo()).containsEntry(clave, mensaje);
    }

    private void entoncesSeRechazaConLosErrores(Map<String, String> esperados) {
        assertThat(erroresDelRechazo()).containsExactlyInAnyOrderEntriesOf(esperados);
    }

    private void entoncesLaLineaSeRechazaPorque(String mensaje) {
        assertThat(erroresDelRechazo()).containsEntry("lineas[0]", mensaje);
    }

    private void entoncesSeRechazaPorDatosInvalidos() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
    }

    private void entoncesLaRecetaNuevaConservaLaIdentidadDeLaGuardada() {
        assertThat(receta.getId()).isEqualTo(ID_GUARDADA);
        assertThat(receta.getCreadoEn()).isEqualTo(CREADA_ANTES);
        assertThat(receta.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaRecetaEsVaciaYSoloConoceAlNegocioYAlProducto() {
        assertThat(receta.esVacia()).isTrue();
        assertThat(receta.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(receta.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(receta.getId()).isNull();
    }

    private void entoncesSoloLaSegundaLineaTieneElError(String mensaje) {
        assertThat(errores).containsOnly(Map.entry("lineas[1]", mensaje));
    }

    private void entoncesNoHayErrores() {
        assertThat(errores).isEmpty();
    }

    private Map<String, String> erroresDelRechazo() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
        return ((RegistroInvalidoException) rechazo).getErrores();
    }
}
