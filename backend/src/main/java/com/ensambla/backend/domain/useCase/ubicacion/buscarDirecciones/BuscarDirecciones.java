package com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones;

import com.ensambla.backend.domain.model.ubicacion.ConsultaDireccion;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.List;

@RequiredArgsConstructor
public class BuscarDirecciones {

    private final GeocodificadorPort geocodificadorPort;
    private final Clock clock;

    public BuscarDireccionesResponse execute(BuscarDireccionesRequest request) {
        List<DireccionEncontrada> direcciones = geocodificadorPort
                .buscar(ConsultaDireccion.de(request.consulta()).texto(), clock.instant());
        return BuscarDireccionesResponse.de(direcciones);
    }
}
