package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.zona.DatosDondeAbrirRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatosDondeAbrirRepositoryImpl implements DatosDondeAbrirRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<DatosZonaDondeAbrir> buscar(RubroNegocio rubro) {
        Map<ZonaId, ConstructorZona> zonas = cargarZonasYMetricas(rubro);
        cargarDemandaEnsambla(zonas);
        cargarPuntos(zonas, rubro);
        return zonas.values().stream().map(ConstructorZona::construir).toList();
    }

    private Map<ZonaId, ConstructorZona> cargarZonasYMetricas(RubroNegocio rubro) {
        Map<ZonaId, ConstructorZona> zonas = new LinkedHashMap<>();
        jdbcTemplate.query("""
                WITH metricas_elegidas AS (
                    SELECT DISTINCT ON (m.zona_id, m.tipo)
                        m.zona_id, m.tipo, m.valor, m.unidad, m.observada_en,
                        f.codigo fuente_codigo, f.nombre fuente_nombre, f.tipo fuente_tipo,
                        f.url fuente_url, f.licencia fuente_licencia
                    FROM metrica_zona m
                    JOIN fuente_dato_zona f ON f.codigo = m.fuente_codigo
                    WHERE m.tipo <> 'COMPETENCIA' OR m.rubro = ?
                    ORDER BY m.zona_id, m.tipo, m.confianza DESC, m.observada_en DESC, f.codigo
                )
                SELECT z.id zona_id, z.nombre, z.localidad, z.partido, z.provincia, z.latitud, z.longitud,
                       m.tipo metrica_tipo, m.valor metrica_valor, m.unidad metrica_unidad,
                       m.observada_en metrica_observada_en, m.fuente_codigo, m.fuente_nombre,
                       m.fuente_tipo, m.fuente_url, m.fuente_licencia
                FROM zona z
                JOIN metricas_elegidas m ON m.zona_id = z.id
                ORDER BY z.nombre, m.tipo
                """, (RowCallbackHandler) rs -> agregarZonaYMetrica(zonas, rs), rubro.name());
        return zonas;
    }

    private static void agregarZonaYMetrica(Map<ZonaId, ConstructorZona> zonas, ResultSet rs) throws SQLException {
        ZonaId zonaId = ZonaId.de(rs.getObject("zona_id", UUID.class));
        ConstructorZona zona = zonas.get(zonaId);
        if (zona == null) {
            zona = new ConstructorZona(zonaId, texto(rs, "nombre"), texto(rs, "localidad"),
                    texto(rs, "partido"), texto(rs, "provincia"), numero(rs, "latitud"),
                    numero(rs, "longitud"));
            zonas.put(zonaId, zona);
        }
        FactorDondeAbrir factor = FactorDondeAbrir.valueOf(texto(rs, "metrica_tipo"));
        zona.metricas.put(factor, new DatosZonaDondeAbrir.Medicion(
                numero(rs, "metrica_valor"), texto(rs, "metrica_unidad"), fuente(rs),
                rs.getObject("metrica_observada_en", LocalDate.class)));
    }

    private void cargarDemandaEnsambla(Map<ZonaId, ConstructorZona> zonas) {
        jdbcTemplate.query("""
                SELECT a.zona_id, SUM(a.unidades_semana) valor, MAX(a.periodo_hasta) observada_en,
                       f.codigo fuente_codigo, f.nombre fuente_nombre, f.tipo fuente_tipo,
                       f.url fuente_url, f.licencia fuente_licencia
                FROM agregado_zona_producto a
                JOIN fuente_dato_zona f ON f.codigo = 'ENSAMBLA'
                WHERE a.comercios >= 3
                GROUP BY a.zona_id, f.codigo, f.nombre, f.tipo, f.url, f.licencia
                """, (RowCallbackHandler) rs -> {
                    ConstructorZona zona = zonas.get(ZonaId.de(rs.getObject("zona_id", UUID.class)));
                    if (zona != null) {
                        zona.metricas.put(FactorDondeAbrir.DEMANDA_ENSAMBLA,
                                new DatosZonaDondeAbrir.Medicion(numero(rs, "valor"), "unidades_semana",
                                        fuente(rs), rs.getObject("observada_en", LocalDate.class)));
                    }
                });
    }

    private void cargarPuntos(Map<ZonaId, ConstructorZona> zonas, RubroNegocio rubro) {
        jdbcTemplate.query("""
                SELECT p.zona_id, p.clave_externa, p.tipo, p.rubro, p.nombre, p.latitud, p.longitud,
                       p.observada_en, f.codigo fuente_codigo, f.nombre fuente_nombre, f.tipo fuente_tipo,
                       f.url fuente_url, f.licencia fuente_licencia
                FROM punto_interes_zona p
                JOIN fuente_dato_zona f ON f.codigo = p.fuente_codigo
                WHERE p.tipo <> 'COMPETENCIA' OR p.rubro = ?
                ORDER BY p.zona_id, p.tipo, p.nombre, p.clave_externa
                """, (RowCallbackHandler) rs -> {
                    ConstructorZona zona = zonas.get(ZonaId.de(rs.getObject("zona_id", UUID.class)));
                    if (zona != null) {
                        zona.puntos.add(new DatosZonaDondeAbrir.Punto(
                                texto(rs, "clave_externa"), texto(rs, "tipo"), texto(rs, "rubro"),
                                texto(rs, "nombre"), numero(rs, "latitud"), numero(rs, "longitud"), fuente(rs),
                                rs.getObject("observada_en", LocalDate.class)));
                    }
                }, rubro.name());
    }

    private static DatosZonaDondeAbrir.Fuente fuente(ResultSet rs) throws SQLException {
        return new DatosZonaDondeAbrir.Fuente(texto(rs, "fuente_codigo"), texto(rs, "fuente_nombre"),
                texto(rs, "fuente_tipo"), texto(rs, "fuente_url"), texto(rs, "fuente_licencia"));
    }

    private static String texto(ResultSet rs, String columna) throws SQLException {
        return rs.getString(columna);
    }

    private static BigDecimal numero(ResultSet rs, String columna) throws SQLException {
        return rs.getBigDecimal(columna);
    }

    private static final class ConstructorZona {
        private final ZonaId id;
        private final String nombre;
        private final String localidad;
        private final String partido;
        private final String provincia;
        private final BigDecimal latitud;
        private final BigDecimal longitud;
        private final Map<FactorDondeAbrir, DatosZonaDondeAbrir.Medicion> metricas =
                new EnumMap<>(FactorDondeAbrir.class);
        private final List<DatosZonaDondeAbrir.Punto> puntos = new ArrayList<>();

        private ConstructorZona(ZonaId id, String nombre, String localidad, String partido, String provincia,
                                BigDecimal latitud, BigDecimal longitud) {
            this.id = id;
            this.nombre = nombre;
            this.localidad = localidad;
            this.partido = partido;
            this.provincia = provincia;
            this.latitud = latitud;
            this.longitud = longitud;
        }

        private DatosZonaDondeAbrir construir() {
            return new DatosZonaDondeAbrir(id, nombre, localidad, partido, provincia, latitud, longitud,
                    Map.copyOf(metricas), List.copyOf(puntos));
        }
    }
}
