package com.ensambla.backend.domain.model.promocion;

/** Un estado de WhatsApp: corto y directo, con el título en negrita. */
public class CanalEstadoWhatsapp extends CanalPromocion {

    private static final int LARGO_MAXIMO = 700;

    @Override
    public String clave() {
        return "estado";
    }

    @Override
    public String nombre() {
        return "Estado de WhatsApp";
    }

    @Override
    protected String armarTexto(String negocio, Promocion promocion) {
        return "*" + promocion.titulo() + "*\n" + promocion.gancho() + "\n\n" + renglonesDe(promocion)
                + "\n\n" + negocio;
    }

    @Override
    protected int largoMaximo() {
        return LARGO_MAXIMO;
    }
}
