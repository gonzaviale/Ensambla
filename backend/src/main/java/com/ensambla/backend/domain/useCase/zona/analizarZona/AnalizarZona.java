package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.ubicacion.ConsultaDireccion;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class AnalizarZona {

    private final ZonaRepository zonaRepository;

    private final GeocodificadorPort geocodificadorPort;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final Clock clock;

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    /**
     * Con un punto del mapa busca la zona precargada que lo contiene; si no, busca un barrio por nombre y, si
     * no es uno conocido, lo trata como una dirección. No filtra por rubro: usa los agregados de la zona, sin
     * productos si aportan menos de {@link AnalisisZona#MINIMO_COMERCIOS} comercios y ordenados por ventas.
     */
    public ZonaItem execute(AnalizarZonaRequest request) {
        Zona zona = zonaPedida(request)
                .orElseThrow(() -> new ResourceNotFoundException("Todavía no tenemos datos de esa zona"));
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        AnalisisZona analisis = agregadoZonaRepository.analizar(zona)
                .sinProductosSiHayMenosDe(AnalisisZona.MINIMO_COMERCIOS)
                .ordenadoPorVentas();
        ZonaItem zonaItem = ZonaItem.de(analisis);
        AccesoContenidoItem acceso = controlPlanService.seleccionar(
                negocioId, TipoContenidoPlan.ZONA, zonaItem.clavesDeAcceso(), OffsetDateTime.now(clock));
        return zonaItem.filtrada(acceso);
    }

    private Optional<Zona> zonaPedida(AnalizarZonaRequest request) {
        if (request.tienePunto()) {
            return zonaRepository.resolver(request.latitud(), request.longitud());
        }
        return buscarPorTexto(request.lugar())
                .or(() -> zonaDeLaDireccion(request.lugar()));
    }

    /** Zona cuyo nombre o localidad coincide con lo escrito ("San Justo", "villa crespo"). */
    private Optional<Zona> buscarPorTexto(String texto) {
        return zonaRepository.findAll().stream()
                .filter(zona -> zona.coincideCon(texto))
                .findFirst();
    }

    private Optional<Zona> zonaDeLaDireccion(String lugar) {
        String consulta = ConsultaDireccion.de(lugar).texto();
        return geocodificadorPort.buscar(consulta, clock.instant()).stream()
                .findFirst()
                .flatMap(this::zonaDe);
    }

    private Optional<Zona> zonaDe(DireccionEncontrada direccion) {
        return zonaRepository.resolver(direccion.latitud(), direccion.longitud());
    }
}
