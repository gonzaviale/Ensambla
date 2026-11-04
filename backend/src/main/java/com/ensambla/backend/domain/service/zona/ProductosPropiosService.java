package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Los nombres de los productos que el negocio tiene en su inventario, de todos sus proyectos y sin las filas de
 * ejemplo. Sirven para decidir, en cada pantalla de Mi zona, qué productos de la zona se ven primero.
 */
@RequiredArgsConstructor
public class ProductosPropiosService {

    static final String CLAVE_NOMBRE = "name";

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    public List<String> nombresDelInventario(NegocioId negocioId) {
        return proyectoRepository.findAllByNegocioId(negocioId).stream()
                .map(proyecto -> proyectoRepository.findByIdAndNegocioId(proyecto.getId(), negocioId))
                .flatMap(Optional::stream)
                .flatMap(proyecto -> datosDeProyectoService.cargar(proyecto, negocioId).inventarios().stream())
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> nombreDe(inventario, registro)))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<String> nombreDe(BloqueInventario inventario, Registro registro) {
        return inventario.valorDe(registro, CLAVE_NOMBRE) instanceof String nombre && !nombre.isBlank()
                ? Optional.of(nombre)
                : Optional.empty();
    }
}
