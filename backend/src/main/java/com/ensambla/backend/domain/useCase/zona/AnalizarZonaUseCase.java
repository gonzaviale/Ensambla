package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.dto.zona.AnalizarZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.zona.ZonaItemMapper;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class AnalizarZonaUseCase {

    private final ZonaService zonaService;

    private final UbicacionService ubicacionService;

    private final AgregadoZonaService agregadoZonaService;

    private final ZonaItemMapper zonaItemMapper;

    private final Clock clock;

    private final NegocioService negocioService;

    private final AccesoZonaService accesoZonaService;

    /** Primero busca un barrio por nombre; si no es uno conocido, lo trata como una dirección. */
    public ZonaItem execute(AnalizarZonaRequest request) {
        Zona zona = zonaService.buscarPorTexto(request.lugar())
                .or(() -> zonaDeLaDireccion(request.lugar()))
                .orElseThrow(() -> new ResourceNotFoundException("Todavía no tenemos datos de esa zona"));
        return accesoZonaService.filtrar(negocioService.obtenerDelUsuario(request.usuarioId()),
                zonaItemMapper.toResponse(agregadoZonaService.analizar(zona)), OffsetDateTime.now(clock));
    }

    private Optional<Zona> zonaDeLaDireccion(String lugar) {
        return ubicacionService.buscar(lugar, clock.instant()).stream()
                .findFirst()
                .flatMap(this::zonaDe);
    }

    private Optional<Zona> zonaDe(DireccionEncontrada direccion) {
        return zonaService.resolver(direccion.latitud(), direccion.longitud());
    }
}
