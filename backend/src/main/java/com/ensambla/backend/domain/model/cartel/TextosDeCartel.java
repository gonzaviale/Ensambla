package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lo que dice el cartel y el texto para copiar en redes. Sólo puede decir números que existen en los datos reales, no
 * lleva marcado ni enlaces y respeta los largos máximos; si no, se descarta.
 */
public record TextosDeCartel(TipoDeCartel plantilla, String titular, String bajada, String textoPosteo) {

    public static final int MAXIMO_TEXTO_POSTEO = 280;

    public TextosDeCartel {
        titular = exigirTexto(titular, ContenidoDeCartel.MAXIMO_TITULAR, "titular");
        bajada = exigirTexto(bajada, ContenidoDeCartel.MAXIMO_BAJADA, "bajada");
        textoPosteo = exigirTexto(textoPosteo, MAXIMO_TEXTO_POSTEO, "texto del posteo");
    }

    /** Los textos que se usan sin IA, armados con los datos reales. */
    public static TextosDeCartel deReserva(DatosDePromocion datos, String negocio) {
        TipoDeCartel tipo = datos.tiposPosibles().getFirst();
        String titular = tipo.titularDeReservaPara(datos.primerProducto());
        String bajada = tipo.bajadaDeReserva();
        return new TextosDeCartel(tipo, titular, bajada, titular + " " + bajada + ". ¡Te esperamos en " + negocio + "!");
    }

    public void exigirCoherentesCon(DatosDePromocion datos) {
        exigirPlantillaPosible(datos);
        exigirSinMarcado();
        exigirNumerosReales(datos.numerosPermitidos());
    }

    private void exigirPlantillaPosible(DatosDePromocion datos) {
        if (!datos.tiposPosibles().contains(plantilla)) {
            throw new BusinessRuleException("La plantilla elegida no sirve para esta promoción");
        }
    }

    private void exigirSinMarcado() {
        if (textos().anyMatch(texto -> texto.matches("(?s).*(<|>|@|https?:|www\\.).*"))) {
            throw new BusinessRuleException("El texto no puede llevar marcado ni enlaces");
        }
    }

    private void exigirNumerosReales(Set<BigDecimal> permitidos) {
        if (textos().anyMatch(texto -> !permitidos.containsAll(NumerosDeTexto.en(texto)))) {
            throw new BusinessRuleException("El texto dice un número que no coincide con los datos");
        }
    }

    private Stream<String> textos() {
        return Stream.of(titular, bajada, textoPosteo);
    }

    private static String exigirTexto(String texto, int maximo, String nombre) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty() || limpio.length() > maximo) {
            throw new BusinessRuleException("El " + nombre + " tiene que tener entre 1 y " + maximo + " caracteres");
        }
        return limpio;
    }
}
