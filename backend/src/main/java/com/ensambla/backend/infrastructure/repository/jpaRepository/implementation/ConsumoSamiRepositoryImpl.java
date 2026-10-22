package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConsumoSamiRepositoryImpl implements ConsumoSamiRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Una sola sentencia: inserta la primera consulta del día o suma una más sólo si queda cupo, así dos pedidos
     * simultáneos del mismo negocio no pasan el límite.
     */
    @Override
    @Transactional
    public boolean reservar(UUID negocioId, LocalDate fecha, int limite, OffsetDateTime ahora) {
        int modificadas = jdbcTemplate.update("""
                INSERT INTO consumo_sami_diario (negocio_id, fecha, consultas, creado_en, actualizado_en)
                SELECT :negocio, :fecha, 1, :ahora, :ahora WHERE :limite >= 1
                ON CONFLICT (negocio_id, fecha) DO UPDATE
                    SET consultas = consumo_sami_diario.consultas + 1, actualizado_en = :ahora
                    WHERE consumo_sami_diario.consultas < :limite
                """, new MapSqlParameterSource()
                .addValue("negocio", negocioId)
                .addValue("fecha", fecha)
                .addValue("limite", limite)
                .addValue("ahora", ahora));
        return modificadas > 0;
    }

    @Override
    @Transactional
    public void registrar(UUID negocioId, LocalDate fecha, ConsumoIaSami consumo, OffsetDateTime ahora) {
        jdbcTemplate.update("""
                UPDATE consumo_sami_diario
                SET tokens_entrada = tokens_entrada + :entrada,
                    tokens_salida = tokens_salida + :salida,
                    modelo = :modelo,
                    actualizado_en = :ahora
                WHERE negocio_id = :negocio AND fecha = :fecha
                """, new MapSqlParameterSource()
                .addValue("negocio", negocioId)
                .addValue("fecha", fecha)
                .addValue("entrada", consumo.tokensEntrada())
                .addValue("salida", consumo.tokensSalida())
                .addValue("modelo", consumo.modelo())
                .addValue("ahora", ahora));
    }
}
