package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Guarda una carga confirmada: las filas que nombran algo que ya existía lo actualizan; el resto se suma al final de
 * la tabla. Antes se suman los productos o clientes nuevos que nombran sus vínculos y, si la tabla es de movimientos,
 * después se mueve el stock de cada producto.
 */
@RequiredArgsConstructor
final class GuardadoDePropuesta {

    private final BloqueRepository bloqueRepository;
    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;
    private final ControlPlanService controlPlanService;
    private final PropuestasDelChat propuestas;
    private final VinculosDeLaPropuesta vinculos;
    private final StockDeLaPropuesta stock;

    /** La tabla donde se guarda, con lo que hace falta para armar cada fila. */
    private record Tabla(Bloque bloque, List<Campo> campos, Vinculados vinculados, NegocioId negocioId,
                         OffsetDateTime ahora) {
    }

    /** Las filas ya validadas: las que se suman y las filas existentes que se actualizaron. */
    private record Filas(List<Map<String, Object>> nuevas, List<Registro> actualizadas) {
    }

    ResultadoDelGuardado guardar(PropuestaWhatsapp propuesta, NegocioId negocioId, OffsetDateTime ahora) {
        propuesta.exigirPendiente();
        Bloque bloque = obtenerBloque(propuesta.getBloqueId(), negocioId);
        List<Campo> campos = obtenerCampos(bloque, negocioId);
        Vinculados vinculados = vinculos.vincularTodo(propuesta.getFilas(), negocioId, ahora);
        Tabla tabla = new Tabla(bloque, campos, vinculados, negocioId, ahora);
        Filas filas = armarFilas(propuesta.getFilas(), tabla);
        guardarFilas(filas, tabla);
        return confirmar(propuesta, filas, tabla);
    }

    private Filas armarFilas(List<FilaPropuesta> propuestas, Tabla tabla) {
        List<Map<String, Object>> nuevas = new ArrayList<>();
        List<Registro> actualizadas = new ArrayList<>();
        for (FilaPropuesta fila : propuestas) {
            existente(fila, tabla.bloque().getId(), tabla.negocioId()).ifPresentOrElse(
                    registro -> actualizadas.add(actualizar(registro, fila, tabla)),
                    () -> nuevas.add(armarNueva(fila, tabla)));
        }
        return new Filas(nuevas, actualizadas);
    }

    private Registro actualizar(Registro registro, FilaPropuesta fila, Tabla tabla) {
        Map<String, Object> cambios = tabla.vinculados().ponerIds(fila,
                fila.cambiosTipados(tabla.campos(), registro.getValores()));
        registro.reemplazarValores(
                ValidacionDeFilas.validar(tabla.campos(), registro.combinarCon(cambios)), tabla.ahora());
        return registro;
    }

    private Map<String, Object> armarNueva(FilaPropuesta fila, Tabla tabla) {
        return ValidacionDeFilas.validar(tabla.campos(),
                tabla.vinculados().ponerIds(fila, fila.tipados(tabla.campos())));
    }

    private void guardarFilas(Filas filas, Tabla tabla) {
        guardarNuevas(filas.nuevas(), tabla);
        guardarActualizadas(filas.actualizadas());
    }

    private void guardarNuevas(List<Map<String, Object>> nuevas, Tabla tabla) {
        if (nuevas.isEmpty()) {
            return;
        }
        controlPlanService.validarAltaRegistros(tabla.negocioId(), nuevas.size(), tabla.ahora());
        int primero = registroRepository.siguienteOrden(tabla.bloque().getId(), tabla.negocioId());
        registroRepository.saveAll(IntStream.range(0, nuevas.size())
                .mapToObj(i -> Registro.deWhatsapp(tabla.negocioId(), tabla.bloque().getId(), nuevas.get(i),
                        primero + i, tabla.ahora()))
                .toList());
    }

    private void guardarActualizadas(List<Registro> actualizadas) {
        if (!actualizadas.isEmpty()) {
            registroRepository.saveAll(actualizadas);
        }
    }

    private ResultadoDelGuardado confirmar(PropuestaWhatsapp propuesta, Filas filas, Tabla tabla) {
        List<MovimientoDeStock> movimientos = stock.mover(tabla.bloque(), tabla.campos(), filas.nuevas(),
                tabla.negocioId(), tabla.ahora());
        propuesta.confirmar(tabla.ahora());
        propuestas.guardar(propuesta);
        return new ResultadoDelGuardado(tabla.bloque().getNombre(), filas.nuevas().size(),
                filas.actualizadas().size(), tabla.vinculados().sumados(), movimientos);
    }

    /** La fila que la propuesta iba a actualizar, si todavía está en esa tabla; si se borró, se agrega como nueva. */
    private Optional<Registro> existente(FilaPropuesta fila, BloqueId bloqueId, NegocioId negocioId) {
        if (!fila.actualizaUnaFila()) {
            return Optional.empty();
        }
        return registroRepository.findByIdAndNegocioId(RegistroId.de(fila.registroId()), negocioId)
                .filter(registro -> registro.getBloqueId().equals(bloqueId));
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.findByIdAndNegocioId(bloqueId, negocioId)
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
    }

    private List<Campo> obtenerCampos(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }
}
