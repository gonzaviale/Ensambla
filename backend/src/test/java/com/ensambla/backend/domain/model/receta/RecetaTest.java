package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecetaTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID HARINA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final UUID PRECIO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    @Test
    @DisplayName("Arma una receta válida y deja la unidad sin espacios en los bordes")
    void nuevaShouldBuildValidReceta() {
        Receta result = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(
                new LineaReceta(HARINA_ID, new BigDecimal("0.5"), " kg "),
                new LineaReceta(AZUCAR_ID, new BigDecimal("2"), "  ")), AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.getCampoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(result.getLineas()).extracting(LineaReceta::unidad).containsExactly("kg", null);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.esVacia()).isFalse();
        assertThat(result.insumoIds()).containsExactly(HARINA_ID, AZUCAR_ID);
    }

    @Test
    @DisplayName("Una receta sin líneas es vacía y no guarda la columna del precio")
    void nuevaShouldBeEmptyWithoutLines() {
        Receta result = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(), AHORA);

        assertThat(result.esVacia()).isTrue();
        assertThat(result.getCampoPrecioId()).isNull();
    }

    @Test
    @DisplayName("Con líneas pide la columna del precio")
    void nuevaShouldRequirePriceColumnWithLines() {
        List<LineaReceta> lineas = List.of(new LineaReceta(HARINA_ID, BigDecimal.ONE, null));

        assertThat(erroresDe(null, lineas)).containsEntry(
                "campoPrecioId", "Elegí la columna con el precio de los insumos");
    }

    @Test
    @DisplayName("Rechaza un insumo faltante, el propio producto y un insumo repetido")
    void nuevaShouldRejectMissingSelfAndRepeatedInsumo() {
        Map<String, String> errores = erroresDe(PRECIO_ID, List.of(
                new LineaReceta(null, BigDecimal.ONE, null),
                new LineaReceta(PRODUCTO_ID, BigDecimal.ONE, null),
                new LineaReceta(HARINA_ID, BigDecimal.ONE, null),
                new LineaReceta(HARINA_ID, BigDecimal.ONE, null)));

        assertThat(errores).containsExactlyInAnyOrderEntriesOf(Map.of(
                "lineas[0]", "Elegí el insumo",
                "lineas[1]", "Un producto no puede ser insumo de sí mismo",
                "lineas[3]", "Ese insumo está repetido"));
    }

    @Test
    @DisplayName("Rechaza cantidades ausentes, en cero, con muchos decimales o demasiado grandes")
    void nuevaShouldRejectInvalidQuantities() {
        assertThat(errorDeLinea(new LineaReceta(HARINA_ID, null, null))).isEqualTo("Escribí la cantidad");
        assertThat(errorDeLinea(new LineaReceta(HARINA_ID, BigDecimal.ZERO, null)))
                .isEqualTo("La cantidad tiene que ser mayor a cero");
        assertThat(errorDeLinea(new LineaReceta(HARINA_ID, new BigDecimal("0.0001"), null)))
                .isEqualTo("La cantidad admite hasta 3 decimales");
        assertThat(errorDeLinea(new LineaReceta(HARINA_ID, new BigDecimal("100000000000"), null)))
                .isEqualTo("La cantidad es demasiado grande");
    }

    @Test
    @DisplayName("Acepta la cantidad máxima y ceros de más en los decimales")
    void nuevaShouldAcceptMaximumQuantityAndTrailingZeros() {
        Receta result = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(
                new LineaReceta(HARINA_ID, Receta.CANTIDAD_MAXIMA, null),
                new LineaReceta(AZUCAR_ID, new BigDecimal("1.50000"), null)), AHORA);

        assertThat(result.getLineas()).hasSize(2);
    }

    @Test
    @DisplayName("Rechaza una unidad demasiado larga")
    void nuevaShouldRejectTooLongUnit() {
        assertThat(errorDeLinea(new LineaReceta(HARINA_ID, BigDecimal.ONE, "x".repeat(31))))
                .isEqualTo("La unidad puede tener hasta 30 caracteres");
    }

    @Test
    @DisplayName("Rechaza una receta con demasiados insumos")
    void nuevaShouldRejectTooManyLines() {
        List<LineaReceta> lineas = IntStream.range(0, 51)
                .mapToObj(n -> new LineaReceta(UUID.randomUUID(), BigDecimal.ONE, null))
                .toList();

        assertThat(erroresDe(PRECIO_ID, lineas)).containsEntry("lineas", "Una receta puede tener hasta 50 insumos");
    }

    @Test
    @DisplayName("Reemplazar conserva el id y la fecha de creación de la receta guardada")
    void reemplazarShouldKeepIdentityOfTheSavedOne() {
        Receta nueva = Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(), AHORA);
        UUID id = UUID.fromString("f0000000-0000-4000-8000-000000000001");
        OffsetDateTime antes = AHORA.minusDays(3);

        nueva.reemplazar(Receta.builder().id(id).creadoEn(antes).build());

        assertThat(nueva.getId()).isEqualTo(id);
        assertThat(nueva.getCreadoEn()).isEqualTo(antes);
        assertThat(nueva.getActualizadoEn()).isEqualTo(AHORA);
    }

    private static String errorDeLinea(LineaReceta linea) {
        return erroresDe(PRECIO_ID, List.of(linea)).get("lineas[0]");
    }

    private static Map<String, String> erroresDe(UUID campoPrecioId, List<LineaReceta> lineas) {
        try {
            Receta.nueva(NEGOCIO_ID, PRODUCTO_ID, campoPrecioId, lineas, AHORA);
            return Map.of();
        } catch (RegistroInvalidoException e) {
            return e.getErrores();
        }
    }

    @Test
    @DisplayName("Lanza una excepción con los errores cuando algo es inválido")
    void nuevaShouldThrowRegistroInvalido() {
        assertThatThrownBy(() -> Receta.nueva(
                NEGOCIO_ID, PRODUCTO_ID, null, List.of(new LineaReceta(HARINA_ID, BigDecimal.ONE, null)), AHORA))
                .isInstanceOf(RegistroInvalidoException.class);
    }
}
