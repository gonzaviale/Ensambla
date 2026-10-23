package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.service.aviso.AccionAvisoService;
import com.ensambla.backend.domain.service.aviso.MotorAvisosService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre el motor de avisos sobre los datos de prueba de V10 (almacén de ejemplo de San Justo). Usa su propia base:
 * los demás tests de repositorio borran negocios y usuarios, y estos datos tienen que estar intactos.
 *
 * <p>Cada regla suma acá su caso positivo y su caso de control. Las ventas del demo se siembran relativas a la fecha de
 * hoy (current_date), así que las reglas de ventas se corren con la hora real y no con {@code AHORA}.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
@Transactional
class MotorAvisosDatosPruebaTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MotorAvisosService motorAvisosService;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private AccionAvisoService accionAvisoService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("El almacén demo recibe un aviso por cada producto agotado o bajo el mínimo y ninguno por los demás")
    void ejecutarShouldCreateStockAvisosOnlyForLowProductsOfDemo() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, AHORA);

        List<Aviso> avisos = pendientesDeStock(demoId);
        assertThat(avisos).extracting(Aviso::getTitulo).containsExactlyInAnyOrder(
                "Yerba Playadito 1 kg está por debajo del mínimo",
                "Te quedaste sin Aceite Natura 900 ml",
                "Lavandina Ayudín 1 L está por debajo del mínimo");
        assertThat(avisos).filteredOn(aviso -> aviso.getPrioridad() == 3).hasSize(1);
    }

    @Test
    @DisplayName("Los avisos del demo ofrecen registrar la entrada en la tabla de entradas del proyecto")
    void ejecutarShouldSuggestRegisteringEntriesInDemo() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, AHORA);

        assertThat(pendientesDeStock(demoId))
                .allSatisfy(aviso -> assertThat(aviso.getAcciones().toString())
                        .contains("REGISTRAR").contains("Registrar entrada"));
    }

    @Test
    @DisplayName("Correr el motor dos veces no duplica los avisos")
    void ejecutarShouldBeIdempotent() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, AHORA);
        int primeraVez = avisoRepository.findByNegocioId(demoId).size();
        motorAvisosService.ejecutar(demoId, AHORA.plusMinutes(5));

        assertThat(primeraVez).isGreaterThanOrEqualTo(3);
        assertThat(avisoRepository.findByNegocioId(demoId)).hasSize(primeraVez);
    }

    @Test
    @DisplayName("Un aviso descartado no reaparece hasta que cambian los datos de origen")
    void ejecutarShouldKeepDiscardedAvisoUntilDatosChange() {
        UUID demoId = negocioDemo();
        motorAvisosService.ejecutar(demoId, AHORA);
        Aviso yerba = pendientesDeStock(demoId).stream()
                .filter(aviso -> aviso.getTitulo().startsWith("Yerba")).findFirst().orElseThrow();
        yerba.descartar(AHORA);
        avisoRepository.save(yerba);

        motorAvisosService.ejecutar(demoId, AHORA.plusMinutes(5));

        assertThat(pendientesDeStock(demoId)).hasSize(2);

        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '1') WHERE id = ?",
                UUID.fromString(yerba.getEntidad().substring("registro:".length())));
        entityManager.clear();
        motorAvisosService.ejecutar(demoId, AHORA.plusMinutes(10));

        assertThat(pendientesDeStock(demoId))
                .extracting(Aviso::getTitulo).anyMatch(titulo -> titulo.startsWith("Yerba"));
    }

    @Test
    @DisplayName("Un producto que repone stock deja de tener aviso y el aviso vence")
    void ejecutarShouldExpireAvisoWhenStockIsRestored() {
        UUID demoId = negocioDemo();
        motorAvisosService.ejecutar(demoId, AHORA);
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '50')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Aceite Natura%'", demoId);
        entityManager.clear();

        motorAvisosService.ejecutar(demoId, AHORA.plusMinutes(5));

        assertThat(pendientesDeStock(demoId)).hasSize(2);
        assertThat(avisoRepository.findByNegocioIdAndEstado(demoId, EstadoAviso.VENCIDO))
                .filteredOn(aviso -> aviso.getTipo() == TipoAviso.STOCK).hasSize(1);
    }

    @Test
    @DisplayName("El azúcar del demo, cara y sin ventas hace más de un mes, recibe el aviso de rebajar el precio")
    void ejecutarShouldCreateEstancadoAvisoForAzucarOfDemo() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, OffsetDateTime.now());

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).singleElement().satisfies(aviso -> {
            assertThat(aviso.getTitulo()).isEqualTo("Azúcar Ledesma 1 kg está caro y no se mueve");
            assertThat(aviso.getPrioridad()).isEqualTo((short) 2);
            assertThat(aviso.getAcciones().toString()).contains("EDITAR_REGISTRO").contains("Rebajar a $1.150");
            assertThat(aviso.getMotivos().get(0).toString()).startsWith("En tu zona, 8 comercios lo venden");
        });
    }

    @Test
    @DisplayName("Los productos que el demo vende normalmente no reciben el aviso de producto estancado")
    void ejecutarShouldNotCreateEstancadoAvisoForProductsThatSell() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, OffsetDateTime.now());

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).extracting(Aviso::getTitulo)
                .noneMatch(titulo -> titulo.startsWith("Yerba") || titulo.startsWith("Coca"));
    }

    @Test
    @DisplayName("Rebajar el azúcar hace desaparecer el aviso de producto estancado")
    void ejecutarShouldExpireEstancadoAvisoWhenPriceIsLowered() {
        UUID demoId = negocioDemo();
        motorAvisosService.ejecutar(demoId, OffsetDateTime.now());
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'price')], '1150')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Azúcar Ledesma%'", demoId);
        entityManager.clear();

        motorAvisosService.ejecutar(demoId, OffsetDateTime.now().plusMinutes(5));

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).isEmpty();
    }

    @Test
    @DisplayName("El alfajor y las galletitas del demo, que salen poco pero siempre juntos, reciben el aviso de combo")
    void ejecutarShouldCreateComboAvisoForAlfajorAndGalletitasOfDemo() {
        UUID demoId = negocioDemo();

        motorAvisosService.ejecutar(demoId, OffsetDateTime.now());

        assertThat(pendientesDe(demoId, TipoAviso.COMBO)).singleElement().satisfies(aviso -> {
            assertThat(aviso.getTitulo()).endsWith(" se venden juntos")
                    .contains("Alfajor Jorgito").contains("Galletitas Criollitas 3 U");
            assertThat(aviso.getPrioridad()).isEqualTo((short) 1);
            assertThat(aviso.getDetalle()).contains("4 veces");
            assertThat(aviso.getAcciones().toString()).contains("CREAR_REGISTRO").contains("Crear el combo");
        });
    }

    @Test
    @DisplayName("Aplicar el combo crea el producto en el inventario y el aviso vence en la corrida siguiente")
    void ejecutarShouldCreateComboProductWhenAvisoIsApplied() {
        UUID demoId = negocioDemo();
        motorAvisosService.ejecutar(demoId, OffsetDateTime.now());
        Aviso combo = pendientesDe(demoId, TipoAviso.COMBO).get(0);

        accionAvisoService.ejecutar(combo, OffsetDateTime.now());
        entityManager.flush();
        entityManager.clear();
        motorAvisosService.ejecutar(demoId, OffsetDateTime.now().plusMinutes(5));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?"
                + " AND valores::text LIKE '%\"Combo %'", Integer.class, demoId))
                .isEqualTo(1);
        assertThat(pendientesDe(demoId, TipoAviso.COMBO)).isEmpty();
    }

    private List<Aviso> pendientesDeStock(UUID negocioId) {
        return pendientesDe(negocioId, TipoAviso.STOCK);
    }

    private List<Aviso> pendientesDe(UUID negocioId, TipoAviso tipo) {
        return avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.PENDIENTE).stream()
                .filter(aviso -> aviso.getTipo() == tipo)
                .toList();
    }

    private UUID negocioDemo() {
        return jdbc.queryForObject("SELECT n.id FROM negocio n JOIN usuario u ON u.id = n.duenio_id"
                + " WHERE u.email = 'demo@ensambla.test'", UUID.class);
    }
}
