package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionCajaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String ESQUEMA = "prueba_migracion_caja";

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUsuario;

    @Value("${spring.datasource.password}")
    private String jdbcContrasenia;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.flyway.out-of-order:false}")
    private boolean permitirMigracionesPendientesAnteriores;

    @TempDir
    private Path migracionesDev;

    @Test
    @DisplayName("Una base de dev en V33 incorpora Caja sin perder datos ni editar su historial")
    void migrateShouldUpgradeExistingDevWithoutCaja() throws IOException {
        try {
            givenDevWithoutCaja();
            Long negociosAntes = jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".negocio",
                    Long.class);

            var resultado = Flyway.configure().dataSource(jdbcUrl, jdbcUsuario, jdbcContrasenia)
                    .schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .outOfOrder(permitirMigracionesPendientesAnteriores).load().migrate();

            assertThat(permitirMigracionesPendientesAnteriores).isTrue();
            assertThat(resultado.migrationsExecuted).isEqualTo(3);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".negocio", Long.class))
                    .isEqualTo(negociosAntes);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".venta_caja", Long.class))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".cierre_caja", Long.class))
                    .isZero();
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    @Test
    @DisplayName("Las ventas de la primera caja siguen siendo legibles después de agregar pagos y cierres")
    void migrateShouldPreserveLegacyReceiptAndTypedInventoryId() throws Exception {
        try {
            givenLegacySale();

            Flyway.configure().dataSource(jdbcUrl, jdbcUsuario, jdbcContrasenia)
                    .schemas(ESQUEMA).defaultSchema(ESQUEMA).load().migrate();

            String datos = jdbcTemplate.queryForObject("SELECT datos::text FROM " + ESQUEMA + ".venta_caja",
                    String.class);
            VentaCaja venta = new ObjectMapper().findAndRegisterModules().readValue(datos, VentaCaja.class);
            assertThat(venta.id()).isEqualTo(CajaTestData.OPERACION);
            assertThat(venta.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
            assertThat(venta.total()).isEqualByComparingTo("2000");
            assertThat(venta.recibido()).isEqualByComparingTo("5000");
            assertThat(venta.vuelto()).isEqualByComparingTo("3000");
            assertThat(venta.pagos()).singleElement().satisfies(pago -> {
                assertThat(pago.medio()).isEqualTo(MedioPagoCaja.EFECTIVO);
                assertThat(pago.importe()).isEqualByComparingTo("2000");
            });
            assertThat(venta.lineas().getFirst().nombre()).isEqualTo("Pan");
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    // --- arrange ---
    private void givenDevWithoutCaja() throws IOException {
        try (var archivos = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path archivo : archivos.filter(path -> !path.getFileName().toString()
                    .matches("V(27|34|35)__.*")).toList()) {
                Files.copy(archivo, migracionesDev.resolve(archivo.getFileName()));
            }
        }
        Flyway.configure().dataSource(jdbcUrl, jdbcUsuario, jdbcContrasenia)
                .schemas(ESQUEMA).defaultSchema(ESQUEMA)
                .locations("filesystem:" + migracionesDev).target("33").load().migrate();
    }

    private void givenLegacySale() {
        Flyway.configure().dataSource(jdbcUrl, jdbcUsuario, jdbcContrasenia)
                .schemas(ESQUEMA).defaultSchema(ESQUEMA)
                .target("27").load().migrate();
        UUID negocioId = jdbcTemplate.queryForObject("SELECT id FROM " + ESQUEMA + ".negocio LIMIT 1", UUID.class);
        String datos = """
                {"id":{"valor":"%s"},"negocioId":{"valor":"%s"},"proyectoId":{"valor":"%s"},
                "huella":"%s:%s:%s:EFECTIVO:5000:[]",
                "lineas":[{"productoId":{"valor":"%s"},"nombre":"Pan","modo":"PESO",
                "cantidad":0.5,"precio":4000,"importe":2000}],
                "medio":"EFECTIVO","total":2000,"recibido":5000,"vuelto":3000,"creadoEn":"2026-11-12T12:00:00Z"}
                """.formatted(CajaTestData.OPERACION, negocioId, CajaTestData.PROYECTO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, CajaTestData.VENTAS, CajaTestData.PRODUCTO);
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".venta_caja "
                        + "(id,negocio_id,proyecto_id,datos,creado_en,actualizado_en) VALUES (?,?,?,?::jsonb,?,?)",
                CajaTestData.OPERACION.valor(), negocioId, CajaTestData.PROYECTO.valor(), datos,
                CajaTestData.AHORA, CajaTestData.AHORA);
    }
}
