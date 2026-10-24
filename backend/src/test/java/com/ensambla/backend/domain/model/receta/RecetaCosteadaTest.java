package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecetaCosteadaTest {

    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId AZUCAR_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final RegistroId LEVADURA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000004");
    private static final RegistroId BORRADO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000005");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("El costo es la suma de cantidad por precio de cada insumo, sin redondear")
    void deShouldSumQuantityTimesPrice() {
        Receta receta = givenReceta(
                new LineaReceta(HARINA_ID, new BigDecimal("0.5"), "kg"),
                new LineaReceta(AZUCAR_ID, new BigDecimal("2"), null));

        RecetaCosteada result = RecetaCosteada.de(receta, BLOQUE_ID, List.of(
                givenInsumo(HARINA_ID.valor(), "Harina", 1200), givenInsumo(AZUCAR_ID.valor(), "Azúcar", 850.5)),
                        givenCampos());

        assertThat(result.costo()).isEqualByComparingTo("2301");
        assertThat(result.incompleta()).isFalse();
        assertThat(result.campoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(result.bloqueInsumosId()).isEqualTo(BLOQUE_ID);
        assertThat(result.lineas()).extracting(RecetaCosteada.Linea::insumo).containsExactly("Harina", "Azúcar");
        assertThat(result.lineas().get(0).subtotal()).isEqualByComparingTo("600");
        assertThat(result.lineas().get(0).precioUnitario()).isEqualByComparingTo("1200");
        assertThat(result.lineas().get(0).unidad()).isEqualTo("kg");
        assertThat(result.lineas().get(1).subtotal()).isEqualByComparingTo("1701");
    }

    @Test
    @DisplayName("Un insumo sin precio no suma y deja la receta incompleta")
    void deShouldMarkIncompleteWhenAPriceIsMissing() {
        Registro sinPrecio = Registro.builder()
                .id(LEVADURA_ID)
                .valores(Map.of(NOMBRE_ID.toString(), "Levadura", PRECIO_ID.toString(), "no es número"))
                .build();
        Receta receta = givenReceta(
                new LineaReceta(HARINA_ID, BigDecimal.ONE, null), new LineaReceta(LEVADURA_ID, BigDecimal.ONE, null));

        RecetaCosteada result = RecetaCosteada.de(
                receta, BLOQUE_ID, List.of(givenInsumo(HARINA_ID.valor(), "Harina", 100), sinPrecio), givenCampos());

        assertThat(result.costo()).isEqualByComparingTo("100");
        assertThat(result.incompleta()).isTrue();
        assertThat(result.lineas().get(1).insumo()).isEqualTo("Levadura");
        assertThat(result.lineas().get(1).precioUnitario()).isNull();
        assertThat(result.lineas().get(1).subtotal()).isNull();
    }

    @Test
    @DisplayName("Un insumo que ya no existe se avisa como eliminado y deja la receta incompleta")
    void deShouldFlagDeletedInsumo() {
        Receta receta = givenReceta(new LineaReceta(BORRADO_ID, BigDecimal.ONE, null));

        RecetaCosteada result = RecetaCosteada.de(receta, BLOQUE_ID, List.of(), givenCampos());

        assertThat(result.costo()).isEqualByComparingTo("0");
        assertThat(result.incompleta()).isTrue();
        assertThat(result.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.insumo()).isEqualTo("Insumo eliminado");
            assertThat(linea.subtotal()).isNull();
        });
    }

    @Test
    @DisplayName("La receta vacía cuesta cero y no está incompleta")
    void vaciaShouldCostZero() {
        RecetaCosteada result = RecetaCosteada.vacia();

        assertThat(result.lineas()).isEmpty();
        assertThat(result.costo()).isEqualByComparingTo("0");
        assertThat(result.incompleta()).isFalse();
        assertThat(result.campoPrecioId()).isNull();
        assertThat(result.bloqueInsumosId()).isNull();
    }

    private static Receta givenReceta(LineaReceta... lineas) {
        return Receta.builder().campoPrecioId(PRECIO_ID).lineas(List.of(lineas)).build();
    }

    private static Registro givenInsumo(UUID id, String nombre, Number precio) {
        return Registro.builder()
                .id(RegistroId.de(id))
                .valores(Map.of(NOMBRE_ID.toString(), nombre, PRECIO_ID.toString(), precio))
                .build();
    }

    private static List<Campo> givenCampos() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build(),
                Campo.builder().id(PRECIO_ID).tipo(TipoCampo.NUMERO).posicion((short) 1).build());
    }
}
