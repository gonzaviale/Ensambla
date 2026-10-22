package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.dto.zona.AnalizarZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.zona.ZonaItemMapper;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.Optional;

@RequiredArgsConstructor
public class AnalizarZonaUseCase {

    /** Menos comercios que esto y los agregados no son anónimos: no se muestran productos. */
    static final int MINIMO_COMERCIOS = 3;

    private final ZonaService zonaService;

    private final UbicacionService ubicacionService;

    private final AgregadoZonaService agregadoZonaService;

    private final ZonaItemMapper zonaItemMapper;

    private final Clock clock;

    /**
     * Con un punto del mapa busca la zona precargada que lo contiene; si no, busca un barrio por nombre y, si
     * no es uno conocido, lo trata como una dirección. No filtra por rubro: usa los agregados de la zona.
     */
    public ZonaItem execute(AnalizarZonaRequest request) {
        Zona zona = zonaDe(request)
                .orElseThrow(() -> new ResourceNotFoundException("Todavía no tenemos datos de esa zona"));
        AnalisisZona analisis = agregadoZonaService.analizar(zona)
                .sinProductosSiHayMenosDe(MINIMO_COMERCIOS)
                .ordenadoPorVentas();
        return zonaItemMapper.toResponse(analisis);
    }

    private Optional<Zona> zonaDe(AnalizarZonaRequest request) {
        if (request.tienePunto()) {
            return zonaService.resolver(request.latitud(), request.longitud());
        }
        return zonaService.buscarPorTexto(request.lugar())
                .or(() -> zonaDeLaDireccion(request.lugar()));
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
