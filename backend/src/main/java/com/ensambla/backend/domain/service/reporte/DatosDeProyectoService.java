package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.BloqueEntradas;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Carga las tablas de inventario, de ventas, de gastos y de entradas de un proyecto con sus campos por clave semántica y sus
 * registros. Las filas de ejemplo de la plantilla no son datos del negocio y quedan afuera.
 */
@RequiredArgsConstructor
public class DatosDeProyectoService {

    private final RegistroRepository registroRepository;

    public DatosDeProyecto cargar(Proyecto proyecto, NegocioId negocioId) {
        BloqueId entradasId = proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.ENTRADAS)
                .map(Bloque::getId)
                .findFirst()
                .orElse(null);
        return new DatosDeProyecto(
                proyecto.getNombre(),
                delTipo(proyecto, TipoBloque.INVENTARIO).stream()
                        .map(bloque -> new BloqueInventario(
                                bloque, entradasId, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                        .toList(),
                delTipo(proyecto, TipoBloque.VENTAS).stream()
                        .map(bloque -> new BloqueVentas(
                                bloque, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                        .toList(),
                delTipo(proyecto, TipoBloque.GASTOS).stream()
                        .map(bloque -> new BloqueGastos(
                                bloque, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                        .toList(),
                nombresDeClientes(proyecto, negocioId),
                delTipo(proyecto, TipoBloque.ENTRADAS).stream()
                        .map(bloque -> new BloqueEntradas(
                                bloque, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                        .toList());
    }

    /** El nombre (campo {@code name}) de cada fila de las tablas de clientes, por id de la fila. */
    private Map<String, String> nombresDeClientes(Proyecto proyecto, NegocioId negocioId) {
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Bloque bloque : delTipo(proyecto, TipoBloque.CLIENTES)) {
            Campo nombre = camposPorClave(bloque).get("name");
            if (nombre == null) {
                continue;
            }
            for (Registro registro : registrosReales(bloque, negocioId)) {
                if (registro.getValores().get(nombre.getId().toString()) instanceof String texto && !texto.isBlank()) {
                    nombres.put(registro.getId().toString(), texto.trim());
                }
            }
        }
        return nombres;
    }

    private static List<Bloque> delTipo(Proyecto proyecto, TipoBloque tipo) {
        return proyecto.getBloques().stream().filter(bloque -> bloque.getTipo() == tipo).toList();
    }

    private List<Registro> registrosReales(Bloque bloque, NegocioId negocioId) {
        return registroRepository.findAllByBloqueIdAndNegocioId(bloque.getId(), negocioId).stream()
                .filter(registro -> !registro.esDeEjemplo())
                .toList();
    }

    private static Map<String, Campo> camposPorClave(Bloque bloque) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().stream()
                .filter(campo -> Objects.nonNull(campo.getClave()))
                .forEach(campo -> porClave.put(campo.getClave(), campo));
        return porClave;
    }
}
