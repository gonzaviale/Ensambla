package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.ia.PromptLecturaCuaderno;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcesamientoRepositoryImpl implements ProcesamientoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final IaProperties iaProperties;

    @Override
    @Transactional
    public Optional<LoteTrabajo> reclamar(OffsetDateTime ahora, OffsetDateTime vence) {
        return jdbcTemplate.query("""
                WITH siguiente AS (
                    SELECT id FROM lote_subida
                    WHERE estado = 'PENDIENTE' AND (proximo_intento_en IS NULL OR proximo_intento_en <= :ahora)
                    ORDER BY creado_en, id FOR UPDATE SKIP LOCKED LIMIT 1
                )
                UPDATE lote_subida l SET estado = 'PROCESANDO', intentos = intentos + 1,
                    reclamo_id = :token, reclamo_hasta = :vence, motivo_error = NULL, actualizado_en = :ahora
                FROM siguiente s WHERE l.id = s.id
                RETURNING l.id, l.negocio_id, l.bloque_destino_id, l.reclamo_id, l.intentos
                """, new MapSqlParameterSource("ahora", ahora)
                .addValue("vence", vence).addValue("token", UUID.randomUUID()), this::lote).stream().findFirst();
    }

    @Override
    public List<LoteTrabajo> vencidos(OffsetDateTime ahora) {
        return jdbcTemplate.query("""
                SELECT id, negocio_id, bloque_destino_id, reclamo_id, intentos FROM lote_subida
                WHERE estado = 'PROCESANDO' AND reclamo_hasta <= :ahora
                ORDER BY reclamo_hasta LIMIT 100
                """, Map.of("ahora", ahora), this::lote);
    }

    @Override
    public List<FotoLectura> fotos(LoteTrabajo lote) {
        return jdbcTemplate.query("""
                SELECT clave_almacenamiento, tipo_contenido FROM foto_cuaderno
                WHERE negocio_id = :negocio AND lote_id = :lote ORDER BY posicion
                """, parametros(lote), (fila, indice) ->
                new FotoLectura(fila.getString("clave_almacenamiento"), fila.getString("tipo_contenido")));
    }

    @Override
    public List<BloqueLectura> bloques(UUID negocioId) {
        List<Map<String, Object>> bloques = jdbcTemplate.queryForList("""
                SELECT id, nombre FROM bloque WHERE negocio_id = :negocio ORDER BY id
                """, Map.of("negocio", negocioId));
        Map<UUID, List<CampoConocido>> campos = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT id, bloque_id, nombre, tipo, opciones, bloque_relacionado_id FROM campo
                WHERE negocio_id = :negocio ORDER BY posicion, id
                """, Map.of("negocio", negocioId), fila -> {
            UUID bloqueId = fila.getObject("bloque_id", UUID.class);
            List<String> opciones = objectMapper.readValue(fila.getString("opciones"), new TypeReference<>() { });
            campos.computeIfAbsent(bloqueId, id -> new ArrayList<>()).add(new CampoConocido(
                    fila.getObject("id", UUID.class), fila.getString("nombre"),
                    TipoCampo.valueOf(fila.getString("tipo")), opciones,
                    fila.getObject("bloque_relacionado_id", UUID.class)));
        });
        return bloques.stream().map(bloque -> new BloqueLectura((UUID) bloque.get("id"),
                (String) bloque.get("nombre"), campos.getOrDefault(bloque.get("id"), List.of()))).toList();
    }

    @Override
    @Transactional
    public boolean reservar(LoteTrabajo lote, int paginas, int limite, OffsetDateTime ahora) {
        MapSqlParameterSource parametros = parametros(lote).addValue("paginas", paginas)
                .addValue("limite", limite).addValue("fecha", ahora.toLocalDate()).addValue("ahora", ahora);
        List<Integer> reserva = jdbcTemplate.query("""
                SELECT paginas_reservadas FROM lote_subida
                WHERE negocio_id = :negocio AND id = :lote AND reclamo_id = :token AND estado = 'PROCESANDO'
                FOR UPDATE
                """, parametros, (fila, indice) -> fila.getInt(1));
        if (reserva.isEmpty() || reserva.getFirst() > 0) {
            return false;
        }
        int modificadas = jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, reservadas, creado_en, actualizado_en)
                SELECT :negocio, :fecha, :paginas, :ahora, :ahora WHERE :paginas <= :limite
                ON CONFLICT (negocio_id, fecha) DO UPDATE
                    SET reservadas = consumo_ia_diario.reservadas + EXCLUDED.reservadas, actualizado_en = :ahora
                    WHERE consumo_ia_diario.paginas + consumo_ia_diario.reservadas + EXCLUDED.reservadas <= :limite
                """, parametros);
        if (modificadas == 0) {
            return false;
        }
        jdbcTemplate.update("""
                UPDATE lote_subida SET paginas_reservadas = :paginas, dia_reserva = :fecha
                WHERE negocio_id = :negocio AND id = :lote AND reclamo_id = :token
                """, parametros);
        return true;
    }

    @Override
    @Transactional
    public void finalizar(LoteTrabajo lote, ResultadoIntento resultado, BigDecimal costo,
                          OffsetDateTime ahora, OffsetDateTime reintentarEn) {
        MapSqlParameterSource parametros = parametros(lote);
        List<Map<String, Object>> reservas = jdbcTemplate.queryForList("""
                SELECT paginas_reservadas, dia_reserva FROM lote_subida
                WHERE negocio_id = :negocio AND id = :lote AND reclamo_id = :token AND estado = 'PROCESANDO'
                FOR UPDATE
                """, parametros);
        if (reservas.isEmpty()) {
            return;
        }
        int paginas = (Integer) reservas.getFirst().get("paginas_reservadas");
        parametros.addValue("ahora", ahora).addValue("error", resultado.error())
                .addValue("proximo", reintentarEn, Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("estado", estadoFinal(resultado, reintentarEn));
        if (paginas > 0) {
            LocalDate fecha = ((Date) reservas.getFirst().get("dia_reserva")).toLocalDate();
            guardarIntento(lote, resultado, costo, ahora, paginas, fecha);
            parametros.addValue("fecha", reservas.getFirst().get("dia_reserva")).addValue("paginas", paginas);
            jdbcTemplate.update("""
                    UPDATE consumo_ia_diario SET reservadas = reservadas - :paginas, paginas = paginas + :paginas,
                        actualizado_en = :ahora
                    WHERE negocio_id = :negocio AND fecha = :fecha
                    """, parametros);
        }
        jdbcTemplate.update("""
                UPDATE lote_subida SET estado = :estado, motivo_error = :error, reclamo_id = NULL,
                    reclamo_hasta = NULL, proximo_intento_en = :proximo, paginas_reservadas = 0, dia_reserva = NULL,
                    procesado_en = :ahora, actualizado_en = :ahora
                WHERE negocio_id = :negocio AND id = :lote AND reclamo_id = :token
                """, parametros);
    }

    @Override
    public Optional<EstadoLectura> obtener(UUID negocioId, UUID loteId) {
        return jdbcTemplate.query("""
                SELECT l.id, l.estado, l.motivo_error, l.intentos,
                    (SELECT propuesta::text FROM lectura_ia i
                     WHERE i.negocio_id = l.negocio_id AND i.lote_id = l.id AND i.propuesta IS NOT NULL
                     ORDER BY i.creado_en DESC, i.id DESC LIMIT 1) AS propuesta
                FROM lote_subida l WHERE l.negocio_id = :negocio AND l.id = :lote
                """, Map.of("negocio", negocioId, "lote", loteId), (fila, indice) ->
                new EstadoLectura(fila.getObject("id", UUID.class), EstadoLote.valueOf(fila.getString("estado")),
                        fila.getString("motivo_error"), fila.getInt("intentos"),
                        propuesta(fila.getString("propuesta")))).stream().findFirst();
    }

    @Override
    @Transactional
    public boolean reencolar(UUID negocioId, UUID loteId, OffsetDateTime ahora) {
        return jdbcTemplate.update("""
                UPDATE lote_subida SET estado = 'PENDIENTE', motivo_error = NULL, intentos = 0,
                    proximo_intento_en = :ahora, actualizado_en = :ahora
                WHERE negocio_id = :negocio AND id = :lote AND estado = 'ERROR'
                """, Map.of("negocio", negocioId, "lote", loteId, "ahora", ahora)) == 1;
    }

    @Override
    public ConsumoDiario consumo(UUID negocioId, LocalDate fecha) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(c.paginas, 0) AS paginas, COALESCE(c.reservadas, 0) AS reservadas,
                    COALESCE(SUM(i.tokens_entrada), 0) AS entrada, COALESCE(SUM(i.tokens_salida), 0) AS salida,
                    CASE WHEN COALESCE(c.paginas_inmediatas, 0) > 0 THEN NULL
                        WHEN COUNT(i.id) = 0 THEN 0 ELSE SUM(i.costo_estimado_usd) END AS costo,
                    CASE WHEN COALESCE(c.paginas_inmediatas, 0) > 0 THEN false
                        WHEN COUNT(i.id) = 0 THEN true
                        ELSE BOOL_AND(i.consumo_completo AND i.costo_estimado_usd IS NOT NULL) END AS completo
                FROM (VALUES (1)) base(dummy)
                LEFT JOIN consumo_ia_diario c ON c.negocio_id = :negocio AND c.fecha = :fecha
                LEFT JOIN lectura_ia i ON i.negocio_id = :negocio
                    AND i.dia_consumo = :fecha
                GROUP BY c.paginas, c.reservadas, c.paginas_inmediatas
                """, Map.of("negocio", negocioId, "fecha", fecha), (fila, indice) ->
                new ConsumoDiario(fecha, fila.getInt("paginas"), fila.getInt("reservadas"),
                        fila.getLong("entrada"), fila.getLong("salida"),
                        fila.getBigDecimal("costo"), fila.getBoolean("completo")));
    }

    private void guardarIntento(LoteTrabajo lote, ResultadoIntento resultado, BigDecimal costo,
                                OffsetDateTime ahora, int paginas, LocalDate fecha) {
        ConsumoIa consumo = consumoIntento(resultado, paginas);
        MapSqlParameterSource parametros = parametros(lote)
                .addValue("id", UUID.randomUUID()).addValue("proveedor", consumo.proveedor())
                .addValue("modelo", consumo.modelo()).addValue("version", consumo.versionPrompt())
                .addValue("paginas", paginas).addValue("entrada", consumo.tokensEntrada())
                .addValue("salida", consumo.tokensSalida()).addValue("duracion", consumo.duracionMs())
                .addValue("costo", costo).addValue("error", resultado.error()).addValue("ahora", ahora)
                .addValue("fecha", fecha)
                .addValue("completo", resultado.propuesta() != null)
                .addValue("resultado", jsonResultado(resultado))
                .addValue("propuesta", jsonPropuesta(resultado));
        jdbcTemplate.update("""
                INSERT INTO lectura_ia (id, negocio_id, lote_id, reclamo_id, proveedor, modelo, version_prompt,
                    resultado, propuesta, motivo_error, paginas, tokens_entrada, tokens_salida,
                    costo_estimado_usd, duracion_ms, dia_consumo, consumo_completo, creado_en, actualizado_en)
                VALUES (:id, :negocio, :lote, :token, :proveedor, :modelo, :version,
                    CAST(:resultado AS jsonb), CAST(:propuesta AS jsonb), :error, :paginas, :entrada, :salida,
                    :costo, :duracion, :fecha, :completo, :ahora, :ahora)
                """, parametros);
    }

    private ConsumoIa consumoIntento(ResultadoIntento resultado, int paginas) {
        if (resultado.propuesta() != null) {
            return resultado.propuesta().lectura().consumo();
        }
        String modelo = iaProperties.modelo();
        if (modelo == null || modelo.isBlank()) {
            modelo = "no-disponible";
        }
        return new ConsumoIa(iaProperties.proveedor(), modelo, PromptLecturaCuaderno.VERSION, paginas, 0, 0, 0);
    }

    private String jsonResultado(ResultadoIntento resultado) {
        if (resultado.propuesta() == null) {
            return null;
        }
        return objectMapper.writeValueAsString(resultado.propuesta().lectura());
    }

    private String jsonPropuesta(ResultadoIntento resultado) {
        if (resultado.propuesta() == null) {
            return null;
        }
        return objectMapper.writeValueAsString(resultado.propuesta());
    }

    private PropuestaLectura propuesta(String json) {
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, PropuestaLectura.class);
    }

    private String estadoFinal(ResultadoIntento resultado, OffsetDateTime reintentarEn) {
        if (resultado.propuesta() != null) {
            return "LISTO";
        }
        if (reintentarEn != null) {
            return "PENDIENTE";
        }
        return "ERROR";
    }

    private MapSqlParameterSource parametros(LoteTrabajo lote) {
        return new MapSqlParameterSource("negocio", lote.negocioId())
                .addValue("lote", lote.id()).addValue("token", lote.reclamoId());
    }

    private LoteTrabajo lote(ResultSet fila, int indice) throws SQLException {
        return new LoteTrabajo(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("bloque_destino_id", UUID.class), fila.getObject("reclamo_id", UUID.class),
                fila.getInt("intentos"));
    }
}
