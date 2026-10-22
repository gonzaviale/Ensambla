package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsoPlanRepositoryImpl implements UsoPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void bloquearNegocio(UUID negocioId) {
        if (jdbcTemplate.queryForList("SELECT id FROM negocio WHERE id = ? FOR UPDATE", UUID.class, negocioId)
                .isEmpty()) {
            throw new ResourceNotFoundException("Negocio no encontrado");
        }
    }

    @Override
    public long contarProyectos(UUID negocioId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM proyecto WHERE negocio_id = ?", Long.class, negocioId);
    }

    @Override
    public long contarRegistros(UUID negocioId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?", Long.class, negocioId);
    }

    @Override
    public long contarRegistrosProyecto(UUID negocioId, UUID proyectoId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM registro r JOIN bloque b ON b.id = r.bloque_id AND b.negocio_id = r.negocio_id
                WHERE r.negocio_id = ? AND b.proyecto_id = ?
                """, Long.class, negocioId, proyectoId);
    }

    @Override
    public long paginasEnPeriodo(UUID negocioId, LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(sum(paginas::bigint + reservadas), 0)
                FROM consumo_ia_diario WHERE negocio_id = ? AND fecha >= ? AND fecha < ?
                """, Long.class, negocioId, desde, hasta);
    }

    @Override
    public boolean registrarPaginaInmediata(UUID negocioId, LocalDate fecha, int limiteDiario, OffsetDateTime ahora) {
        return jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario
                    (negocio_id, fecha, paginas, paginas_inmediatas, reservadas, creado_en, actualizado_en)
                SELECT ?, ?, 1, 1, 0, ?, ? WHERE 1 <= ?
                ON CONFLICT (negocio_id, fecha) DO UPDATE
                    SET paginas = consumo_ia_diario.paginas + 1,
                        paginas_inmediatas = consumo_ia_diario.paginas_inmediatas + 1,
                        actualizado_en = EXCLUDED.actualizado_en
                    WHERE consumo_ia_diario.paginas + consumo_ia_diario.reservadas < ?
                """, negocioId, fecha, ahora, ahora, limiteDiario, limiteDiario) == 1;
    }
}
