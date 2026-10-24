package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarNegocio {

    private final NegocioRepository negocioRepository;

    private final ZonaService zonaService;

    private final UbicacionService ubicacionService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ConfiguracionNegocioResponse execute(ActualizarNegocioRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Negocio negocio = transaccionPort.ejecutar(() -> actualizar(request, ahora));
        return ConfiguracionNegocioResponse.de(negocio, zonaService.obtener(negocio.getZonaId()).orElse(null));
    }

    private Negocio actualizar(ActualizarNegocioRequest request, OffsetDateTime ahora) {
        Negocio negocio = negocioRepository.findByDuenioIdParaActualizar(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
        UbicacionNegocio ubicacion = request.ubicacionNegocio();
        if (ubicacion != null && negocio.cambiaUbicacion(ubicacion)) {
            exigirLocalidadDelPunto(ubicacion, ahora);
        }
        negocio.configurar(request.nombre(), request.rubro(), ubicacion, request.comparteDatosZona(), ahora);
        if (negocio.necesitaZona()) {
            negocio.asignarZona(zonaService.resolver(negocio.getLatitud(), negocio.getLongitud()).orElse(null));
        }
        return negocioRepository.save(negocio);
    }

    /** La localidad escrita tiene que ser una de las que el mapa reconoce en el punto. */
    private void exigirLocalidadDelPunto(UbicacionNegocio ubicacion, OffsetDateTime ahora) {
        LocalidadesDelPunto localidades = ubicacionService.resolverLocalidades(
                ubicacion.latitud(), ubicacion.longitud(), ahora.toInstant());
        if (!localidades.admite(ubicacion.localidad())) {
            throw new BusinessRuleException(
                    "La localidad no coincide con la del mapa (" + localidades.nombres().getFirst() + ")");
        }
    }
}
