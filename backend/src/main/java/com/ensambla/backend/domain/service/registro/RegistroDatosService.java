package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RegistroDatosService {

    static final int MAXIMO_REFERENCIAS = 20;

    private static final String MENSAJE_FILA_INEXISTENTE = "Esa fila ya no existe";

    private final RegistroRepository registroRepository;

    private final TransaccionPort transaccionPort;

    /** Guarda registros ya validados (por ejemplo, los que salen de una lectura del cuaderno). */
    public Registro obtener(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.findByIdAndNegocioId(registroId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El registro no existe"));
    }

    /**
     * Filas del bloque que se ofrecen en un campo RELACION, las más nuevas primero: con {@code ids}, sólo esas (las que
     * existen); sin ids, hasta {@value #MAXIMO_REFERENCIAS} que coinciden con el texto como en el listado.
     */
    public List<Registro> buscarReferenciables(NegocioId negocioId, BloqueId bloqueId, List<Campo> campos, String texto,
                                               List<RegistroId> ids) {
        if (!ids.isEmpty()) {
            return registroRepository.findAllByIdsAndBloque(ids, bloqueId, negocioId);
        }
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloqueId, campos, texto, null, null, 0, MAXIMO_REFERENCIAS);
        return registroRepository.buscar(consulta).contenido();
    }

    public Pagina<Registro> listar(ConsultaRegistros consulta) {
        return registroRepository.buscar(consulta);
    }

    public Registro editar(Registro registro, List<Campo> campos, Map<String, Object> cambios,
                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            Map<String, Object> validos = Registro.validar(campos, registro.combinarCon(cambios));
            validarConexiones(registro.getNegocioId(), campos, validos);
            registro.reemplazarValores(validos, ahora);
            return registroRepository.save(registro);
        });
    }

    /**
     * Cada valor de un campo RELACION tiene que ser una fila existente de la tabla conectada. Hace una consulta por
     * tabla conectada, no una por valor. Los valores ya vienen validados como ids.
     *
     * @throws RegistroInvalidoException con "Esa fila ya no existe" en cada campo que apunta a una fila inexistente
     */
    public void validarConexiones(NegocioId negocioId, List<Campo> campos, Map<String, Object> valores) {
        Map<BloqueId, List<Campo>> porBloque = campos.stream()
                .filter(Campo::esRelacion)
                .filter(campo -> valores.containsKey(campo.getId().toString()))
                .collect(Collectors.groupingBy(Campo::getBloqueRelacionadoId));
        Map<String, String> errores = new LinkedHashMap<>();
        porBloque.forEach((bloqueId, conexiones) -> {
            Set<RegistroId> buscados = conexiones.stream()
                    .map(campo -> referenciaDe(campo, valores))
                    .collect(Collectors.toSet());
            Set<RegistroId> existentes = registroRepository.findAllByIdsAndBloque(buscados, bloqueId, negocioId)
                    .stream()
                    .map(Registro::getId)
                    .collect(Collectors.toSet());
            conexiones.stream()
                    .filter(campo -> !existentes.contains(referenciaDe(campo, valores)))
                    .forEach(campo -> errores.put(campo.getId().toString(), MENSAJE_FILA_INEXISTENTE));
        });
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }

    private static RegistroId referenciaDe(Campo campo, Map<String, Object> valores) {
        return RegistroId.de((String) valores.get(campo.getId().toString()));
    }
}
