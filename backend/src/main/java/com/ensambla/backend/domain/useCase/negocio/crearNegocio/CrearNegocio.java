package com.ensambla.backend.domain.useCase.negocio.crearNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.negocio.NegocioDeLaCuentaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearNegocio {

    private final NegocioRepository negocioRepository;

    private final ZonaRepository zonaRepository;

    private final GeocodificadorPort geocodificadorPort;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public NegocioDeLaCuentaItem execute(CrearNegocioRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return transaccionPort.ejecutar(() -> crear(request, ahora));
    }

    private NegocioDeLaCuentaItem crear(CrearNegocioRequest request, OffsetDateTime ahora) {
        controlPlanService.validarAltaNegocio(request.usuarioId(), ahora);
        UbicacionNegocio ubicacion = request.ubicacion() == null ? null : request.ubicacion().aUbicacionNegocio();
        if (ubicacion != null) {
            exigirLocalidadDelPunto(ubicacion, ahora);
        }
        Negocio negocio = Negocio.builder()
                .duenioId(request.usuarioId())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
        negocio.configurar(request.nombre(), request.rubro() == null ? RubroNegocio.OTRO : request.rubro(),
                ubicacion, true, ahora);
        if (negocio.necesitaZona()) {
            negocio.asignarZona(zonaRepository.resolver(negocio.getLatitud(), negocio.getLongitud()).orElse(null));
        }
        Negocio guardado = negocioRepository.save(negocio);
        NegocioId activoId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return NegocioDeLaCuentaItem.de(guardado, activoId);
    }

    private void exigirLocalidadDelPunto(UbicacionNegocio ubicacion, OffsetDateTime ahora) {
        LocalidadesDelPunto localidades = geocodificadorPort.resolverLocalidades(
                ubicacion.latitud(), ubicacion.longitud(), ahora.toInstant());
        if (!localidades.admite(ubicacion.localidad())) {
            throw new BusinessRuleException(
                    "La localidad no coincide con la del mapa (" + localidades.nombres().getFirst() + ")");
        }
    }
}
