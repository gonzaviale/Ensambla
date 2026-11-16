package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ensambla.backend.support.CartelesDePrueba.contrasteDe;
import static com.ensambla.backend.support.CartelesDePrueba.kitCon;
import static com.ensambla.backend.support.CartelesDePrueba.textosDe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La prueba de calidad de la historia: 30 carteles en 5 rubros con marcas muy distintas (5 rubros × 3 variantes × 2
 * hojas) y se mide que todos se lean, que ninguno se salga de la zona segura y que sean variados entre sí.
 */
class VariedadYArmoniaDeCartelesTest {

    private record Rubro(String nombre, String principal, String secundario, TipoDeCartel tipo, ContenidoDeCartel contenido) {
    }

    private static final List<Rubro> RUBROS = List.of(
            new Rubro("almacén", "#00506A", "#EE8000", TipoDeCartel.PROMO_PRECIO, contenido("Don Rufino",
                    "Aceite a precio de vecino", "$2.000", "Antes $2.500 · Girasol 900 ml")),
            new Rubro("panadería", "#B5651D", "#F4C95D", TipoDeCartel.DOS_POR_UNO, contenido("La Espiga",
                    "Sábado de medialunas dobles", "2×1", "Llevás 2 docenas y pagás 1")),
            new Rubro("kiosco", "#4EB2D4", "#F85F5A", TipoDeCartel.COMBO, contenido("Esquina",
                    "Combo recreo", "$2.500", "Alfajor + gaseosa + chicle")),
            new Rubro("ferretería", "#263443", "#EBCE32", TipoDeCartel.NOVEDAD, contenido("El Tornillo",
                    "Llegó el taladro de 650 W", "$78.000", "Ya disponible en el local")),
            new Rubro("peluquería", "#7B2D5B", "#EBCE32", TipoDeCartel.HORARIOS, contenido("Corte&Color",
                    "Te esperamos", "9 a 19", "Lunes a sábado")));

    private static final List<FormatoDeCartel> HOJAS = List.of(FormatoDeCartel.A4, FormatoDeCartel.A5);

    private List<ComposicionDeCartel> carteles;

    @Test
    @DisplayName("Se generan 30 carteles: 5 rubros, 3 variantes y 2 hojas")
    void seGeneranTreintaCarteles() {
        whenSeGeneranLosTreintaCarteles();

        assertThat(carteles).hasSize(30);
    }

    @Test
    @DisplayName("Todo texto de los 30 carteles se lee: contraste de al menos 4,5 a 1")
    void todosLosTextosSeLeen() {
        whenSeGeneranLosTreintaCarteles();

        assertThat(carteles).allSatisfy(cartel -> assertThat(textosDe(cartel))
                .allSatisfy(texto -> assertThat(contrasteDe(texto, cartel)).isGreaterThanOrEqualTo(4.5)));
    }

    @Test
    @DisplayName("Ningún texto de los 30 carteles se sale de la zona segura de impresión")
    void ningunTextoSeSaleDeLaZonaSegura() {
        whenSeGeneranLosTreintaCarteles();

        assertThat(carteles).allSatisfy(cartel -> assertThat(textosDe(cartel)).allSatisfy(texto -> {
            int margen = cartel.formato().margen();
            assertThat(texto.x()).isBetween((double) margen, (double) cartel.formato().ancho() - margen);
            assertThat(texto.y()).isLessThanOrEqualTo((double) cartel.formato().alto() - margen);
            assertThat(texto.tamanio()).isGreaterThanOrEqualTo(cartel.formato().tamanioMinimoDeTexto());
        }));
    }

    @Test
    @DisplayName("Las tres variantes de una misma marca son composiciones distintas")
    void lasVariantesDeUnaMarcaSonDistintas() {
        whenSeGeneranLosTreintaCarteles();

        assertThat(huellasDeLasVariantesDelRubro(0)).hasSize(3);
    }

    @Test
    @DisplayName("Dos negocios de rubros distintos nunca dan el mismo cartel: 5 marcas, 5 resultados distintos por variante")
    void losNegociosNoDanElMismoCartel() {
        whenSeGeneranLosTreintaCarteles();

        Set<String> huellas = carteles.stream()
                .filter(cartel -> cartel.formato() == FormatoDeCartel.A4)
                .filter(cartel -> cartel.variante() == VarianteDeComposicion.BLOQUE)
                .map(this::huellaDe)
                .collect(Collectors.toSet());
        assertThat(huellas).hasSize(5);
    }

    private List<String> huellasDeLasVariantesDelRubro(int indiceDelRubro) {
        return carteles.stream()
                .skip((long) indiceDelRubro * 6)
                .limit(6)
                .filter(cartel -> cartel.formato() == FormatoDeCartel.A4)
                .map(this::huellaDe)
                .distinct()
                .toList();
    }

    /** Lo que hace distinto a un cartel: dónde caen sus piezas y de qué color son. */
    private String huellaDe(ComposicionDeCartel cartel) {
        return cartel.fondo() + "|" + cartel.elementos().stream()
                .map(elemento -> elemento.getClass().getSimpleName() + elemento.hashCode())
                .collect(Collectors.joining(","));
    }

    private void whenSeGeneranLosTreintaCarteles() {
        carteles = new ArrayList<>();
        for (Rubro rubro : RUBROS) {
            KitDeMarca kit = kitCon(rubro.principal(), rubro.secundario());
            for (FormatoDeCartel hoja : HOJAS) {
                carteles.addAll(OpcionesDeCartel.armar(kit, rubro.tipo(), hoja, rubro.contenido()));
            }
        }
    }

    private static ContenidoDeCartel contenido(String negocio, String titular, String protagonista, String detalle) {
        return new ContenidoDeCartel(negocio, titular, "Esta semana en " + negocio, protagonista, detalle, List.of());
    }
}
