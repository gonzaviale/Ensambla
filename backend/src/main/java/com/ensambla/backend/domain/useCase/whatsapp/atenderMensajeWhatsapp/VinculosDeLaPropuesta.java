package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve los vínculos de todas las filas al guardar: la fila que se encontró al proponer, si sigue estando, o una
 * nueva con ese nombre (una sola por nombre, aunque varias ventas la nombren).
 */
@RequiredArgsConstructor
final class VinculosDeLaPropuesta {

    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;
    private final ControlPlanService controlPlanService;

    Vinculados vincularTodo(List<FilaPropuesta> filas, NegocioId negocioId, OffsetDateTime ahora) {
        Map<String, String> ids = new LinkedHashMap<>();
        List<VinculoPropuesto> nuevos = new ArrayList<>();
        separarVigentesDeNuevos(vinculosDistintos(filas), negocioId, ids, nuevos);
        validarAltaDeLosNuevos(nuevos, negocioId, ahora);
        return new Vinculados(ids, sumarNuevos(nuevos, ids, negocioId, ahora));
    }

    private static Map<String, VinculoPropuesto> vinculosDistintos(List<FilaPropuesta> filas) {
        Map<String, VinculoPropuesto> distintos = new LinkedHashMap<>();
        filas.forEach(fila -> fila.vinculos().values()
                .forEach(vinculo -> distintos.putIfAbsent(Vinculados.clave(vinculo), vinculo)));
        return distintos;
    }

    private void separarVigentesDeNuevos(Map<String, VinculoPropuesto> distintos, NegocioId negocioId,
                                         Map<String, String> ids, List<VinculoPropuesto> nuevos) {
        distintos.forEach((clave, vinculo) -> vigente(vinculo, negocioId)
                .ifPresentOrElse(id -> ids.put(clave, id), () -> nuevos.add(vinculo)));
    }

    private void validarAltaDeLosNuevos(List<VinculoPropuesto> nuevos, NegocioId negocioId, OffsetDateTime ahora) {
        if (!nuevos.isEmpty()) {
            controlPlanService.validarAltaRegistros(negocioId, nuevos.size(), ahora);
        }
    }

    private List<String> sumarNuevos(List<VinculoPropuesto> nuevos, Map<String, String> ids, NegocioId negocioId,
                                     OffsetDateTime ahora) {
        List<String> sumados = new ArrayList<>();
        for (VinculoPropuesto vinculo : nuevos) {
            ids.put(Vinculados.clave(vinculo), sumar(vinculo, negocioId, ahora).getId().toString());
            sumados.add("%s en %s".formatted(vinculo.nombre(), vinculo.tabla()));
        }
        return sumados;
    }

    /** El id de la fila vinculada, si todavía está en su tabla. */
    private Optional<String> vigente(VinculoPropuesto vinculo, NegocioId negocioId) {
        if (vinculo.esNuevo()) {
            return Optional.empty();
        }
        return buscarRegistro(RegistroId.de(vinculo.registroId()), negocioId)
                .filter(registro -> registro.getBloqueId().equals(BloqueId.de(vinculo.bloqueId())))
                .map(registro -> registro.getId().toString());
    }

    /**
     * Suma al final de su tabla la fila que nombra el vínculo, con su nombre y los valores iniciales de la tabla; un
     * número obligatorio sin valor inicial (el stock de un producto) empieza en 0.
     */
    private Registro sumar(VinculoPropuesto vinculo, NegocioId negocioId, OffsetDateTime ahora) {
        BloqueId bloqueId = BloqueId.de(vinculo.bloqueId());
        List<Campo> campos = camposDe(bloqueId, negocioId);
        Map<String, String> valores = valoresIniciales(campos);
        valores.put(campoQueNombra(campos).getId().toString(), vinculo.nombre());
        Map<String, Object> validos = ValidacionDeFilas.validar(campos,
                new FilaPropuesta(valores, Set.of()).tipados(campos));
        return guardarAlFinal(validos, bloqueId, negocioId, ahora);
    }

    private static Campo campoQueNombra(List<Campo> campos) {
        return campos.stream().filter(Campo::nombraLasFilas).findFirst()
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
    }

    private static Map<String, String> valoresIniciales(List<Campo> campos) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo campo : campos) {
            valorInicialDe(campo).ifPresent(valor -> valores.put(campo.getId().toString(), valor));
        }
        return valores;
    }

    private static Optional<String> valorInicialDe(Campo campo) {
        if (campo.getValorInicial() != null && !campo.getValorInicial().isBlank()) {
            return Optional.of(campo.getValorInicial());
        }
        if (campo.esObligatorio() && campo.esNumerico()) {
            return Optional.of("0");
        }
        return Optional.empty();
    }

    private List<Campo> camposDe(BloqueId bloqueId, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }

    private Optional<Registro> buscarRegistro(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.findByIdAndNegocioId(registroId, negocioId);
    }

    private Registro guardarAlFinal(Map<String, Object> valores, BloqueId bloqueId, NegocioId negocioId,
                                    OffsetDateTime ahora) {
        int orden = registroRepository.siguienteOrden(bloqueId, negocioId);
        return registroRepository.save(Registro.deWhatsapp(negocioId, bloqueId, valores, orden, ahora));
    }
}
