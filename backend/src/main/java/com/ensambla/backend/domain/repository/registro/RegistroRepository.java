package com.ensambla.backend.domain.repository.registro;

import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.Optional;
import java.util.UUID;

public interface RegistroRepository {

    Registro save(Registro registro);

    Optional<Registro> findByIdAndNegocioId(UUID id, UUID negocioId);

    void deleteById(UUID id);

    Pagina<Registro> buscar(ConsultaRegistros consulta);
}
