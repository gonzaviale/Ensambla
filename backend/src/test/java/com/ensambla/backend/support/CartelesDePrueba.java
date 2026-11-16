package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.ContenidoDeCartel;
import com.ensambla.backend.domain.model.cartel.ElementoDeCartel;
import com.ensambla.backend.domain.model.cartel.PaletaDeCartel;
import com.ensambla.backend.domain.model.cartel.Rectangulo;
import com.ensambla.backend.domain.model.cartel.TextoDeCartel;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

/** Kits, contenidos y comprobaciones de legibilidad de carteles, compartidos por los tests de carteles. */
public final class CartelesDePrueba {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");

    private CartelesDePrueba() {
    }

    public static KitDeMarca kitCon(String principal, String secundario) {
        KitDeMarca kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
        kit.personalizar(ColorHex.de(principal), ColorHex.de(secundario), Tipografia.POPPINS, TonoDeMarca.CERCANO, AHORA);
        return kit;
    }

    public static ContenidoDeCartel promoDeAceite() {
        return new ContenidoDeCartel("Don Rufino", "Aceite a precio de vecino", "20 % menos hasta el 22/11", "$2.000",
                "Antes $2.500 · Girasol 900 ml", List.of());
    }

    public static ContenidoDeCartel listaDe(int cantidad) {
        List<String> lineas = IntStream.rangeClosed(1, cantidad)
                .mapToObj(numero -> "Producto " + numero + " · $" + numero * 100)
                .toList();
        return new ContenidoDeCartel("Don Rufino", "Lista de precios", "", "", "", lineas);
    }

    /** El color sobre el que cae un texto: el último rectángulo que lo contiene o, si ninguno, el fondo. */
    public static ColorHex superficieBajo(TextoDeCartel texto, ComposicionDeCartel composicion) {
        ColorHex superficie = composicion.fondo();
        for (ElementoDeCartel elemento : composicion.elementos()) {
            if (elemento instanceof Rectangulo rectangulo && contiene(rectangulo, texto)) {
                superficie = rectangulo.color();
            }
        }
        return superficie;
    }

    public static double contrasteDe(TextoDeCartel texto, ComposicionDeCartel composicion) {
        return texto.color().contrasteCon(superficieBajo(texto, composicion));
    }

    public static List<TextoDeCartel> textosDe(ComposicionDeCartel composicion) {
        return composicion.elementos().stream()
                .filter(TextoDeCartel.class::isInstance)
                .map(TextoDeCartel.class::cast)
                .toList();
    }

    public static boolean cumpleElContrasteMinimo(ComposicionDeCartel composicion) {
        return textosDe(composicion).stream()
                .allMatch(texto -> contrasteDe(texto, composicion) >= PaletaDeCartel.CONTRASTE_MINIMO);
    }

    private static boolean contiene(Rectangulo rectangulo, TextoDeCartel texto) {
        return texto.x() >= rectangulo.zona().x() && texto.x() <= rectangulo.zona().x() + rectangulo.zona().ancho()
                && texto.y() >= rectangulo.zona().y() && texto.y() <= rectangulo.zona().y() + rectangulo.zona().alto();
    }
}
