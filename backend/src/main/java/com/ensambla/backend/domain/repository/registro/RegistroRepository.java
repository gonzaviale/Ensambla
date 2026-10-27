package com.ensambla.backend.domain.repository.registro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegistroRepository {

    Registro save(Registro registro);

    List<Registro> saveAll(List<Registro> registros);

    Optional<Registro> findByIdAndNegocioId(RegistroId id, NegocioId negocioId);

    /** Registros del bloque con esos ids; los que no existen (o son de otro bloque o negocio) no vienen. */
    List<Registro> findAllByIdsAndBloque(Collection<RegistroId> ids, BloqueId bloqueId, NegocioId negocioId);

    /** Todos los registros del bloque, los más viejos primero. */
    List<Registro> findAllByBloqueIdAndNegocioId(BloqueId bloqueId, NegocioId negocioId);

    void deleteById(RegistroId id);

    Pagina<Registro> buscar(ConsultaRegistros consulta);

    /**
     * Suma de cada campo numérico entre todos los registros que cumplen la consulta (sin paginar). Sólo trae los
     * campos que algún registro cargó.
     */
    Map<CampoId, BigDecimal> sumarNumericos(ConsultaRegistros consulta, List<CampoId> campoIds);

    /** Las primeras {@code limite} filas del reporte, las más nuevas primero. */
    List<Registro> buscarFiltrados(FiltroReporte filtro, int limite);

    /** Cuántas filas cumplen el filtro del reporte. */
    long contarFiltrados(FiltroReporte filtro);

    /** Suma de cada campo numérico entre todas las filas que cumplen el filtro del reporte. */
    Map<CampoId, BigDecimal> sumarNumericos(FiltroReporte filtro, List<CampoId> campoIds);

    /** El orden que le toca a la próxima fila del bloque: una más que la última. */
    int siguienteOrden(BloqueId bloqueId, NegocioId negocioId);

    default Registro obtener(RegistroId registroId, NegocioId negocioId) {
        return findByIdAndNegocioId(registroId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El registro no existe"));
    }
}
