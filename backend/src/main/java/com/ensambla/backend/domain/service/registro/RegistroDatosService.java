package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroDatosService {

    private final RegistroRepository registroRepository;

    @Transactional
    public Registro crear(UUID negocioId, UUID bloqueId, List<Campo> campos, Map<String, Object> valores,
                          OffsetDateTime ahora) {
        Map<String, Object> validos = Registro.validar(campos, valores);
        return registroRepository.save(Registro.nuevo(negocioId, bloqueId, validos, ahora));
    }

    /** Guarda registros ya validados (por ejemplo, los que salen de una lectura del cuaderno). */
    @Transactional
    public List<Registro> crearVarios(List<Registro> registros) {
        return registroRepository.saveAll(registros);
    }

    public Registro obtener(UUID registroId, UUID negocioId) {
        return registroRepository.findByIdAndNegocioId(registroId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El registro no existe"));
    }

    public Pagina<Registro> listar(ConsultaRegistros consulta) {
        return registroRepository.buscar(consulta);
    }

    /** Cantidad y suma de cada campo numérico de los registros que cumplen la consulta, sin importar la página. */
    public TotalesRegistros totales(ConsultaRegistros consulta, List<Campo> campos, long cantidad) {
        List<UUID> numericos = campos.stream()
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .map(Campo::getId)
                .toList();
        return TotalesRegistros.armar(cantidad, numericos, registroRepository.sumarNumericos(consulta, numericos));
    }

    @Transactional
    public Registro editar(Registro registro, List<Campo> campos, Map<String, Object> cambios,
                           OffsetDateTime ahora) {
        Map<String, Object> validos = Registro.validar(campos, registro.combinarCon(cambios));
        registro.reemplazarValores(validos, ahora);
        return registroRepository.save(registro);
    }

    @Transactional
    public void eliminar(Registro registro) {
        registroRepository.deleteById(registro.getId());
    }
}
