package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarNegocio {

    private final NegocioRepository negocioRepository;

    private final ZonaRepository zonaRepository;

    private final GeocodificadorPort geocodificadorPort;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ConfiguracionNegocioResponse execute(ActualizarNegocioRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Negocio negocio = actualizarEnTransaccion(request, ahora);
        return ConfiguracionNegocioResponse.de(negocio, obtenerZonaDe(negocio));
    }

    private Negocio actualizarEnTransaccion(ActualizarNegocioRequest request, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> actualizar(request, ahora));
    }

    private Negocio actualizar(ActualizarNegocioRequest request, OffsetDateTime ahora) {
        Negocio negocio = obtenerNegocioParaActualizar(request.usuarioId());
        UbicacionNegocio ubicacion = request.ubicacionNegocio();
        exigirLocalidadSiCambiaLaUbicacion(negocio, ubicacion, ahora);
        negocio.configurar(request.nombre(), request.rubro(), ubicacion, request.comparteDatosZona(), ahora);
        asignarZonaSiLaNecesita(negocio);
        return guardar(negocio);
    }

    private Negocio obtenerNegocioParaActualizar(UsuarioId usuarioId) {
        return negocioRepository.findByDuenioIdParaActualizar(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }

    private void exigirLocalidadSiCambiaLaUbicacion(Negocio negocio, UbicacionNegocio ubicacion, OffsetDateTime ahora) {
        if (ubicacion != null && negocio.cambiaUbicacion(ubicacion)) {
            exigirLocalidadDelPunto(ubicacion, ahora);
        }
    }

    /** La localidad escrita tiene que ser una de las que el mapa reconoce en el punto. */
    private void exigirLocalidadDelPunto(UbicacionNegocio ubicacion, OffsetDateTime ahora) {
        LocalidadesDelPunto localidades = resolverLocalidades(ubicacion, ahora);
        if (!localidades.admite(ubicacion.localidad())) {
            throw new BusinessRuleException(
                    "La localidad no coincide con la del mapa (" + localidades.nombres().getFirst() + ")");
        }
    }

    private LocalidadesDelPunto resolverLocalidades(UbicacionNegocio ubicacion, OffsetDateTime ahora) {
        return geocodificadorPort.resolverLocalidades(ubicacion.latitud(), ubicacion.longitud(), ahora.toInstant());
    }

    private void asignarZonaSiLaNecesita(Negocio negocio) {
        if (negocio.necesitaZona()) {
            negocio.asignarZona(resolverZonaDelPunto(negocio));
        }
    }

    private Zona resolverZonaDelPunto(Negocio negocio) {
        return zonaRepository.resolver(negocio.getLatitud(), negocio.getLongitud()).orElse(null);
    }

    private Negocio guardar(Negocio negocio) {
        return negocioRepository.save(negocio);
    }

    private Zona obtenerZonaDe(Negocio negocio) {
        return zonaRepository.findByIdSiTiene(negocio.getZonaId()).orElse(null);
    }
}
