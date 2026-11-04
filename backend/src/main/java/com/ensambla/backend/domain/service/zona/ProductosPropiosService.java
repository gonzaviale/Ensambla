package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Pone primero, en lo que se ve de la zona, los productos que el negocio tiene en su inventario: con el plan gratuito
 * sólo se pueden ver unos pocos y tienen que ser los que sirven para comparar. Los nombres salen de las tablas de
 * inventario de todos los proyectos del negocio, sin las filas de ejemplo.
 */
@RequiredArgsConstructor
public class ProductosPropiosService {

    static final String CLAVE_NOMBRE = "name";

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    public AnalisisZona conLosPropiosPrimero(NegocioId negocioId, AnalisisZona analisis) {
        if (analisis.productos().isEmpty()) {
            return analisis;
        }
        return analisis.conLosPropiosPrimero(nombresDelInventario(negocioId));
    }

    private List<String> nombresDelInventario(NegocioId negocioId) {
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
