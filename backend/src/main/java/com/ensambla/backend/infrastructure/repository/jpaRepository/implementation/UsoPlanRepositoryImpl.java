package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
    public void bloquearNegocio(NegocioId negocioId) {
        if (jdbcTemplate.queryForList("""
                SELECT u.id FROM usuario u JOIN negocio n ON n.duenio_id = u.id WHERE n.id = ? FOR UPDATE OF u
                """, UUID.class, negocioId.valor()).isEmpty()) {
            throw new ResourceNotFoundException("Negocio no encontrado");
        }
    }

    @Override
    public void bloquearCuenta(UsuarioId usuarioId) {
        if (jdbcTemplate.queryForList("SELECT id FROM usuario WHERE id = ? FOR UPDATE", UUID.class, usuarioId.valor())
                .isEmpty()) {
            throw new ResourceNotFoundException("Cuenta no encontrada");
        }
    }

    @Override
    public long contarNegocios(UsuarioId usuarioId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM negocio WHERE duenio_id = ?", Long.class,
                usuarioId.valor());
    }

    @Override
    public long contarRegistros(NegocioId negocioId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?", Long.class,
                negocioId.valor());
    }

    @Override
    public long contarRegistrosProyecto(NegocioId negocioId, ProyectoId proyectoId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM registro r JOIN bloque b ON b.id = r.bloque_id AND b.negocio_id = r.negocio_id
                WHERE r.negocio_id = ? AND b.proyecto_id = ?
                """, Long.class, negocioId.valor(), proyectoId.valor());
    }

    @Override
    public long paginasEnPeriodo(UsuarioId usuarioId, LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(sum(c.paginas::bigint + c.reservadas), 0)
                FROM consumo_ia_diario c JOIN negocio n ON n.id = c.negocio_id
                WHERE n.duenio_id = ? AND c.fecha >= ? AND c.fecha < ?
                """, Long.class, usuarioId.valor(), desde, hasta);
    }

    @Override
    public boolean registrarPaginaInmediata(NegocioId negocioId, LocalDate fecha, int limiteDiario,
            OffsetDateTime ahora) {
        return jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario
                    (negocio_id, fecha, paginas, paginas_inmediatas, reservadas, creado_en, actualizado_en)
                SELECT ?, ?, 1, 1, 0, ?, ? WHERE 1 <= ?
                ON CONFLICT (negocio_id, fecha) DO UPDATE
                    SET paginas = consumo_ia_diario.paginas + 1,
                        paginas_inmediatas = consumo_ia_diario.paginas_inmediatas + 1,
                        actualizado_en = EXCLUDED.actualizado_en
                    WHERE consumo_ia_diario.paginas + consumo_ia_diario.reservadas < ?
                """, negocioId.valor(), fecha, ahora, ahora, limiteDiario, limiteDiario) == 1;
    }
}
