package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

public interface AccesoContenidoRepository {

    Set<String> autorizadas(NegocioId negocioId, TipoContenidoPlan tipo, LocalDate periodo);

    void registrar(NegocioId negocioId, TipoContenidoPlan tipo, LocalDate periodo, String clave, OffsetDateTime ahora);
}
