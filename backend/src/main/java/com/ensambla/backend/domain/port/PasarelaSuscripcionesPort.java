package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;

import java.util.List;
import java.util.Optional;

public interface PasarelaSuscripcionesPort {

    boolean disponible();

    default void exigirDisponible() {
        if (!disponible()) {
            throw new BusinessRuleException("La contratación está pendiente de configurar Mercado Pago.");
        }
    }

    ContratoRemoto crear(RenovacionPremium renovacion, String email);

    Optional<ContratoRemoto> buscar(RenovacionId referencia);

    ContratoRemoto consultar(String proveedorId);

    void cancelar(String proveedorId);

    List<PagoRemoto> listarPagos(String proveedorId);

    String identificarContrato(String tipo, String recursoId);

    void validarNotificacion(String firma, String solicitudId, String recursoId);
}
