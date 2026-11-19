package com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno;

import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * De qué se puede armar una métrica en este cuaderno: sólo las ventas, los gastos o el stock que ya cargó. Así «Armala
 * vos» no ofrece algo que después no se puede calcular.
 */
@RequiredArgsConstructor
public class ListarFuentesDelCuaderno {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    public List<FuenteMetrica> execute(ListarFuentesDelCuadernoRequest request) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerCuaderno(negocio.getId(), request.proyectoId());
        return FuenteMetrica.lasQueEstanEn(leerElCuaderno(proyecto, negocio.getId()));
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerCuaderno(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private DatosDeProyecto leerElCuaderno(Proyecto proyecto, NegocioId negocioId) {
        return datosDeProyectoService.cargar(proyecto, negocioId);
    }
}
