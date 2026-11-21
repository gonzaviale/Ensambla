package com.ensambla.backend.domain.service.caja;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lo que repiten la venta y el cobro de una deuda cuando el cliente pagó mostrando un QR. */
@RequiredArgsConstructor
public class CobroQrService {

    private final CobroQrRepository cobroQrRepository;

    /** Sin cobro con QR no hace nada; con uno, exige que coincida con lo transferido y lo confirma a mano. */
    public void confirmarSiHay(CobroQrId cobroQrId, NegocioId negocioId, BigDecimal transferido,
                               OffsetDateTime ahora) {
        if (cobroQrId == null) {
            return;
        }
        CobroQr cobro = obtenerCobro(cobroQrId, negocioId);
        cobro.exigirTransferenciaPor(transferido);
        guardar(cobro.confirmarManualmente(ahora));
    }

    private CobroQr obtenerCobro(CobroQrId cobroQrId, NegocioId negocioId) {
        return cobroQrRepository.obtener(cobroQrId, negocioId);
    }

    private void guardar(CobroQr cobro) {
        cobroQrRepository.save(cobro);
    }
}
