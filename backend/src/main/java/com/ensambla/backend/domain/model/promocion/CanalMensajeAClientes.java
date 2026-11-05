package com.ensambla.backend.domain.model.promocion;

/** Un mensaje personal para mandarle a los clientes por WhatsApp. */
public class CanalMensajeAClientes extends CanalPromocion {

    private static final int LARGO_MAXIMO = 1000;

    @Override
    public String clave() {
        return "clientes";
    }

    @Override
    public String nombre() {
        return "Mensaje a clientes";
    }

    @Override
    protected String armarTexto(String negocio, Promocion promocion) {
        return "¡Hola! Te cuento lo que hay en " + negocio + ": " + promocion.titulo().toLowerCase() + ".\n\n"
                + renglonesDe(promocion) + "\n\n¿Te guardo alguno? Respondeme por acá.";
    }

    @Override
    protected int largoMaximo() {
        return LARGO_MAXIMO;
    }
}
