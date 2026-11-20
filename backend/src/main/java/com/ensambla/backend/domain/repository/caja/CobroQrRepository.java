package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.Optional;

public interface CobroQrRepository {

    Optional<CobroQr> findByIdAndNegocioId(CobroQrId id, NegocioId negocioId);

    CobroQr save(CobroQr cobro);

    /** El cobro del negocio de la sesión; uno de otro negocio responde como inexistente. */
    default CobroQr obtener(CobroQrId id, NegocioId negocioId) {
        return findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobro con QR no encontrado"));
    }
}
