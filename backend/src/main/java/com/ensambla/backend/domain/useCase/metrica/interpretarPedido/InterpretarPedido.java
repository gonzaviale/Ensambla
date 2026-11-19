package com.ensambla.backend.domain.useCase.metrica.interpretarPedido;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.InterpreteDePedidos;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.metrica.PedidoInterpretado;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Entiende un pedido en palabras («¿qué producto vendo más este mes?») y devuelve la receta que arma con lo que daría
 * hoy, sin guardarla: la persona la mira y decide. No usa IA: el pedido se interpreta con un diccionario fijo y el
 * número lo calcula el motor con las filas reales. Si no se entiende o no se puede calcular, lo dice. Con la receta
 * va lo que se tomó por defecto y lo que no se entendió, para que la persona vea si se la entendió bien.
 */
@RequiredArgsConstructor
public class InterpretarPedido {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MotorMetricas motorMetricas;

    private final InterpreteDePedidos interpreteDePedidos;

    private final Clock clock;

    public PedidoInterpretadoItem execute(InterpretarPedidoRequest request) {
        PedidoInterpretado interpretado = interpreteDePedidos.interpretar(request.pedido());
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerCuaderno(negocio.getId(), request.proyectoId());
        return PedidoInterpretadoItem.de(interpretado, calcularHoy(interpretado.receta(), proyecto, negocio.getId()));
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerCuaderno(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private ResultadoMetrica calcularHoy(DefinicionMetrica receta, Proyecto proyecto, NegocioId negocioId) {
        return motorMetricas.calcular(receta, datosDeProyectoService.cargar(proyecto, negocioId),
                ZonaNegocio.dia(OffsetDateTime.now(clock)));
    }
}
