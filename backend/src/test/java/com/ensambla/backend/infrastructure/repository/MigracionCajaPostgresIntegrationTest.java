package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
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
    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

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

    private Long negociosAntes;
    private MigrateResult resultado;

    @AfterEach
    void borrarElEsquemaDePrueba() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
    }

    @Test
    @DisplayName("Una base de dev en V33 incorpora Caja sin perder datos ni editar su historial")
    void unaBaseDeDevEnV33IncorporaCajaSinPerderDatosNiEditarSuHistorial() throws IOException {
        dadoUnaBaseDeDevEnV33();

        cuandoSeMigraHastaLaUltimaVersion();

        entoncesNoSePermitenMigracionesAnterioresPendientes();
        entoncesSeAplicaronSoloLasMigracionesPosterioresALaV33();
        entoncesSeConservaronLosNegocios();
        entoncesLasTablasDeCajaEstanVacias();
    }

    @Test
    @DisplayName("Las ventas de la primera caja siguen siendo legibles después de agregar pagos y cierres")
    void lasVentasDeLaPrimeraCajaSiguenSiendoLegiblesDespuesDeAgregarPagosYCierres() throws Exception {
        dadoUnaVentaGuardadaConElFormatoDeLaPrimeraCaja();

        cuandoSeMigraHastaLaUltimaVersion();

        entoncesLaVentaConservaSusImportes(ventaGuardada());
        entoncesLaVentaConservaSuPagoEnEfectivo(ventaGuardada());
        entoncesLaVentaConservaSuLinea(ventaGuardada());
    }

    // --- dado ---
    private void dadoUnaBaseDeDevEnV33() throws IOException {
        copiarLasMigracionesDeDevSinCaja();
        migrador().locations("filesystem:" + migracionesDev).target("33").load().migrate();
        negociosAntes = cantidadDeFilasEn("negocio");
    }

    private void dadoUnaVentaGuardadaConElFormatoDeLaPrimeraCaja() {
        migrador().target("34").load().migrate();
        UUID negocioId = jdbcTemplate.queryForObject("SELECT id FROM " + ESQUEMA + ".negocio LIMIT 1", UUID.class);
        insertarVentaCon(datosDeLaVentaDeLaPrimeraCaja(negocioId), negocioId);
    }

    // --- cuando ---
    private void cuandoSeMigraHastaLaUltimaVersion() {
        resultado = migrador().outOfOrder(permitirMigracionesPendientesAnteriores).load().migrate();
    }

    // --- entonces ---
    private void entoncesNoSePermitenMigracionesAnterioresPendientes() {
        assertThat(permitirMigracionesPendientesAnteriores).isFalse();
    }

    private void entoncesSeAplicaronSoloLasMigracionesPosterioresALaV33() throws IOException {
        assertThat(resultado.migrationsExecuted).isEqualTo(cantidadDeMigracionesPosterioresALaV33());
    }

    private void entoncesSeConservaronLosNegocios() {
        assertThat(cantidadDeFilasEn("negocio")).isEqualTo(negociosAntes);
    }

    private void entoncesLasTablasDeCajaEstanVacias() {
        assertThat(cantidadDeFilasEn("venta_caja")).isZero();
        assertThat(cantidadDeFilasEn("cierre_caja")).isZero();
        assertThat(cantidadDeFilasEn("cobro_caja")).isZero();
    }

    private void entoncesLaVentaConservaSusImportes(VentaCaja venta) {
        assertThat(venta.id()).isEqualTo(CajaTestData.OPERACION);
        assertThat(venta.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
        assertThat(venta.total()).isEqualByComparingTo("2000");
        assertThat(venta.recibido()).isEqualByComparingTo("5000");
        assertThat(venta.vuelto()).isEqualByComparingTo("3000");
    }

    private void entoncesLaVentaConservaSuPagoEnEfectivo(VentaCaja venta) {
        assertThat(venta.pagos()).singleElement().satisfies(pago -> {
            assertThat(pago.medio()).isEqualTo(MedioPagoCaja.EFECTIVO);
            assertThat(pago.importe()).isEqualByComparingTo("2000");
        });
    }

    private void entoncesLaVentaConservaSuLinea(VentaCaja venta) {
        assertThat(venta.lineas().getFirst().nombre()).isEqualTo("Pan");
    }

    // --- apoyo ---
    private FluentConfiguration migrador() {
        return Flyway.configure().dataSource(jdbcUrl, jdbcUsuario, jdbcContrasenia)
                .schemas(ESQUEMA).defaultSchema(ESQUEMA);
    }

    private Long cantidadDeFilasEn(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + "." + tabla, Long.class);
    }

    private VentaCaja ventaGuardada() throws Exception {
        String datos = jdbcTemplate.queryForObject("SELECT datos::text FROM " + ESQUEMA + ".venta_caja", String.class);
        return new ObjectMapper().findAndRegisterModules().readValue(datos, VentaCaja.class);
    }

    private int cantidadDeMigracionesPosterioresALaV33() throws IOException {
        try (var archivos = Files.list(MIGRACIONES)) {
            return (int) archivos.filter(archivo -> versionDe(archivo) > 33).count();
        }
    }

    private int versionDe(Path archivo) {
        return Integer.parseInt(archivo.getFileName().toString().replaceFirst("^V(\\d+)__.*", "$1"));
    }

    private void copiarLasMigracionesDeDevSinCaja() throws IOException {
        try (var archivos = Files.list(MIGRACIONES)) {
            for (Path archivo : archivos.filter(this::esAnteriorALaCaja).toList()) {
                Files.copy(archivo, migracionesDev.resolve(archivo.getFileName()));
            }
        }
    }

    private boolean esAnteriorALaCaja(Path archivo) {
        return versionDe(archivo) <= 33;
    }

    private String datosDeLaVentaDeLaPrimeraCaja(UUID negocioId) {
        return """
                {"id":{"valor":"%s"},"negocioId":{"valor":"%s"},"proyectoId":{"valor":"%s"},
                "huella":"%s:%s:%s:EFECTIVO:5000:[]",
                "lineas":[{"productoId":{"valor":"%s"},"nombre":"Pan","modo":"PESO",
                "cantidad":0.5,"precio":4000,"importe":2000}],
                "medio":"EFECTIVO","total":2000,"recibido":5000,"vuelto":3000,"creadoEn":"2026-11-12T12:00:00Z"}
                """.formatted(CajaTestData.OPERACION, negocioId, CajaTestData.PROYECTO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, CajaTestData.VENTAS, CajaTestData.PRODUCTO);
    }

    private void insertarVentaCon(String datos, UUID negocioId) {
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".venta_caja "
                        + "(id,negocio_id,proyecto_id,datos,creado_en,actualizado_en) VALUES (?,?,?,?::jsonb,?,?)",
                CajaTestData.OPERACION.valor(), negocioId, CajaTestData.PROYECTO.valor(), datos,
                CajaTestData.AHORA, CajaTestData.AHORA);
    }
}
