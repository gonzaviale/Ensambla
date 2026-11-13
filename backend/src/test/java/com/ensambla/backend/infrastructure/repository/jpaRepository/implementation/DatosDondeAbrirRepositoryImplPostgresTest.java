package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DatosDondeAbrirRepositoryImplPostgresTest extends PostgresRepositoryTestSupport {

    @Autowired
    private DatosDondeAbrirRepositoryImpl repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Carga las zonas, el dato anónimo de Ensambla y sólo la competencia del rubro pedido")
    void shouldLoadWholeSnapshotInBatches() {
        insertarZonaSinMetricasParaProbarQueNoSeFiltraEnMemoria();

        var zonas = repository.buscar(RubroNegocio.PAPELERIA);

        assertThat(zonas).hasSize(13);
        DatosZonaDondeAbrir sanJusto = zonas.stream()
                .filter(zona -> zona.nombre().equals("San Justo - UNLaM"))
                .findFirst().orElseThrow();
        assertThat(sanJusto.metricas()).containsKeys(
                FactorDondeAbrir.ACTIVIDAD_ACCESO,
                FactorDondeAbrir.COMPETENCIA,
                FactorDondeAbrir.DEMANDA_ENSAMBLA);
        assertThat(sanJusto.metricas().get(FactorDondeAbrir.COMPETENCIA).fuente().codigo()).isEqualTo("OSM");
        assertThat(sanJusto.metricas().get(FactorDondeAbrir.DEMANDA_ENSAMBLA).fuente().codigo())
                .isEqualTo("ENSAMBLA");
        assertThat(sanJusto.puntos()).isNotEmpty()
                .filteredOn(punto -> punto.tipo().equals("COMPETENCIA"))
                .allMatch(punto -> punto.rubro().equals("PAPELERIA"));
        assertThat(sanJusto.puntos()).filteredOn(punto -> !punto.tipo().equals("COMPETENCIA"))
                .allMatch(punto -> punto.rubro() == null);
    }

    private void insertarZonaSinMetricasParaProbarQueNoSeFiltraEnMemoria() {
        UUID zonaId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO zona (id, nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
                VALUES (?, 'Sin métricas', 'Prueba', 'Prueba', 'Buenos Aires', -34.6, -58.4, 500)
                """, zonaId);
        jdbcTemplate.update("""
                INSERT INTO punto_interes_zona
                    (zona_id, clave_externa, tipo, nombre, latitud, longitud, fuente_codigo, observada_en)
                VALUES (?, 'node/sin-metrica', 'ESCUELA', 'Escuela aislada', -34.6, -58.4, 'OSM', current_date)
                """, zonaId);
        jdbcTemplate.update("""
                INSERT INTO agregado_zona_producto
                    (zona_id, producto_normalizado, nombre, comercios, comercios_venden,
                     precio_minimo, precio_promedio, precio_maximo, unidades_semana,
                     periodo_desde, periodo_hasta, calculado_en)
                VALUES (?, 'prueba', 'Prueba', 3, 3, 1, 1, 1, 1, current_date, current_date, now())
                """, zonaId);
    }
}
