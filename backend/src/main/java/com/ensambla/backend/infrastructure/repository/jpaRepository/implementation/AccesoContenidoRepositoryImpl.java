package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccesoContenidoRepositoryImpl implements AccesoContenidoRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<String> autorizadas(UUID negocioId, TipoContenidoPlan tipo, LocalDate periodo) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                SELECT clave FROM contenido_plan_desbloqueado WHERE negocio_id = ? AND tipo = ? AND periodo = ?
                """, String.class, negocioId, tipo.name(), periodo));
    }

    @Override
    public void registrar(UUID negocioId, TipoContenidoPlan tipo, LocalDate periodo, String clave,
                          OffsetDateTime ahora) {
        jdbcTemplate.update("""
                INSERT INTO contenido_plan_desbloqueado (negocio_id,tipo,periodo,clave,creado_en)
                VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING
                """, negocioId, tipo.name(), periodo, clave, ahora);
    }
}
