package com.ensambla.backend.domain.model.promocion;

import java.util.stream.Collectors;

/**
 * Un lugar donde se comparte una promoción. La base fija el flujo (armar el texto del canal y ajustarlo al largo que
 * admite) y cada canal pone su forma de escribir: su tono, su formato y su largo máximo.
 */
public abstract class CanalPromocion {

    private static final String PUNTOS_SUSPENSIVOS = "…";

    /** Identifica al canal en la API: «instagram», «estado», «clientes». */
    public abstract String clave();

    /** Cómo lo ve la persona. */
    public abstract String nombre();

    /** El texto de la promoción listo para publicar, que nunca pasa del largo que admite el canal. */
    public final String redactar(String negocio, Promocion promocion) {
        return ajustarAlLargo(armarTexto(negocio, promocion));
    }

    protected abstract String armarTexto(String negocio, Promocion promocion);

    protected abstract int largoMaximo();

    /** Un renglón por producto: «• Yerba 1 kg: $ 3.600». */
    protected static String renglonesDe(Promocion promocion) {
        return promocion.productos().stream()
                .map(producto -> "• " + producto.nombre() + ": " + Precio.formato(producto.precio()))
                .collect(Collectors.joining("\n"));
    }

    private String ajustarAlLargo(String texto) {
        return texto.length() <= largoMaximo()
                ? texto
                : texto.substring(0, largoMaximo() - PUNTOS_SUSPENSIVOS.length()).stripTrailing() + PUNTOS_SUSPENSIVOS;
    }
}
