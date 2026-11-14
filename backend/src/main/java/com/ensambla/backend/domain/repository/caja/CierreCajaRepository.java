package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.util.List;
import java.util.Optional;

public interface CierreCajaRepository {

    Optional<CierreCaja> findByIdAndNegocioId(CierreCajaId id, NegocioId negocioId);

    List<CierreCaja> listar(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId, int pagina);

    CierreCaja save(CierreCaja cierre);
}
