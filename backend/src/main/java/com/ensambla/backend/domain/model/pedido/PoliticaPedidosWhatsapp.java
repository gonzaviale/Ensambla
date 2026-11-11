package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quién puede pedirle a Sami por WhatsApp. Con la API de prueba de WhatsApp se trabaja con una lista cerrada de
 * clientes: un número que no está en ella (ni es un comercio vinculado) no genera pedidos.
 *
 * @param clientesPermitidos teléfonos de los clientes de prueba, sólo dígitos con código de país
 */
public record PoliticaPedidosWhatsapp(Set<String> clientesPermitidos) {

    public PoliticaPedidosWhatsapp {
        clientesPermitidos = Set.copyOf(clientesPermitidos);
    }

    /** Arma la lista desde lo que se configuró: acepta «+54 9 11…», ignora los vacíos y repite los números igual. */
    public static PoliticaPedidosWhatsapp de(Collection<String> configurados) {
        return new PoliticaPedidosWhatsapp(configurados.stream()
                .filter(numero -> !numero.isBlank())
                .map(numero -> TelefonoWhatsapp.deIngresoManual(numero).valor())
                .collect(Collectors.toSet()));
    }

    public boolean permite(String telefono) {
        return clientesPermitidos.contains(telefono);
    }
}
