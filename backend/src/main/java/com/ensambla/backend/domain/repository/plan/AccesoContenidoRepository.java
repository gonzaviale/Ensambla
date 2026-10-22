package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.enums.TipoContenidoPlan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public interface AccesoContenidoRepository {

    Set<String> autorizadas(UUID negocioId, TipoContenidoPlan tipo, LocalDate periodo);

    void registrar(UUID negocioId, TipoContenidoPlan tipo, LocalDate periodo, String clave, OffsetDateTime ahora);
}
