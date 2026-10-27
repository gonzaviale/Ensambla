package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;

import java.util.Optional;

public interface PropuestaWhatsappRepository {

    PropuestaWhatsapp save(PropuestaWhatsapp propuesta);

    /** La carga que espera respuesta de ese número, si hay una (como máximo hay una por teléfono). */
    Optional<PropuestaWhatsapp> findAbiertaByTelefono(String telefono);
}
