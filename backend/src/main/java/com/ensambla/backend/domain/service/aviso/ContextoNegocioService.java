package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoBloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Arma los datos del negocio que las reglas de aviso necesitan: sus tablas de inventario y de ventas con campos y
 * registros, y los agregados de su zona.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextoNegocioService {

    private final ProyectoRepository proyectoRepository;

    private final RegistroRepository registroRepository;

    private final NegocioRepository negocioRepository;

    private final ZonaService zonaService;

    private final AgregadoZonaService agregadoZonaService;

    public ContextoNegocio armar(UUID negocioId, LocalDate hoy) {
        List<Proyecto> proyectos = proyectoRepository.findAllByNegocioId(negocioId).stream()
                .map(proyecto -> proyectoRepository.findByIdAndNegocioId(proyecto.getId(), negocioId))
                .flatMap(java.util.Optional::stream)
                .toList();
        List<BloqueInventario> inventarios = proyectos.stream()
                .flatMap(proyecto -> inventariosDe(proyecto, negocioId).stream())
                .toList();
        List<BloqueVentas> ventas = proyectos.stream()
                .flatMap(proyecto -> ventasDe(proyecto, negocioId).stream())
                .toList();
        return new ContextoNegocio(negocioId, hoy, inventarios, ventas, zonaDe(negocioId));
    }

    private AnalisisZona zonaDe(UUID negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaService::obtener)
                .map(agregadoZonaService::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }

    private List<BloqueInventario> inventariosDe(Proyecto proyecto, UUID negocioId) {
        UUID entradasId = proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.ENTRADAS)
                .map(Bloque::getId)
                .findFirst()
                .orElse(null);
        return proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.INVENTARIO)
                .map(bloque -> new BloqueInventario(
                        bloque,
                        entradasId,
                        camposPorClave(bloque),
                        registroRepository.findAllByBloqueIdAndNegocioId(bloque.getId(), negocioId)))
                .toList();
    }

    private List<BloqueVentas> ventasDe(Proyecto proyecto, UUID negocioId) {
        return proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.VENTAS)
                .map(bloque -> new BloqueVentas(
                        bloque,
                        camposPorClave(bloque),
                        registroRepository.findAllByBloqueIdAndNegocioId(bloque.getId(), negocioId)))
                .toList();
    }

    private Map<String, Campo> camposPorClave(Bloque bloque) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().stream()
                .filter(campo -> Objects.nonNull(campo.getClave()))
                .forEach(campo -> porClave.put(campo.getClave(), campo));
        return porClave;
    }
}
