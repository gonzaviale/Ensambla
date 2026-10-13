package com.ensambla.backend.domain.repository.registro;

import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RegistroRepository {

    Registro save(Registro registro);

    List<Registro> saveAll(List<Registro> registros);

    Optional<Registro> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Registros del bloque con esos ids; los que no existen (o son de otro bloque o negocio) no vienen. */
    List<Registro> findAllByIdsAndBloque(Collection<UUID> ids, UUID bloqueId, UUID negocioId);

    void deleteById(UUID id);

    Pagina<Registro> buscar(ConsultaRegistros consulta);

    /**
     * Suma de cada campo numérico entre todos los registros que cumplen la consulta (sin paginar). Sólo trae los
     * campos que algún registro cargó.
     */
    Map<UUID, BigDecimal> sumarNumericos(ConsultaRegistros consulta, List<UUID> campoIds);
}
