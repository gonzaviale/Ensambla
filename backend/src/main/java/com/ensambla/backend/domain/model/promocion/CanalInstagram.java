package com.ensambla.backend.domain.model.promocion;

/** Una publicación de Instagram: título, gancho, la lista, una invitación a pasar por el local y etiquetas. */
public class CanalInstagram extends CanalPromocion {

    private static final int LARGO_MAXIMO = 2200;
    private static final String ETIQUETAS = "#ComercioDeBarrio #CompraLocal #HechoEnElBarrio";

    @Override
    public String clave() {
        return "instagram";
    }

    @Override
    public String nombre() {
        return "Instagram";
    }

    @Override
    protected String armarTexto(String negocio, Promocion promocion) {
        return promocion.titulo() + "\n" + promocion.gancho() + "\n\n" + renglonesDe(promocion)
                + "\n\nTe esperamos en " + negocio + ".\n\n" + ETIQUETAS;
    }

    @Override
    protected int largoMaximo() {
        return LARGO_MAXIMO;
    }
}
