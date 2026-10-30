package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Corre el motor de avisos sobre los datos de prueba de V10 (almacén de ejemplo de San Justo). Usa su propia base:
 * los demás tests de repositorio borran negocios y usuarios, y estos datos tienen que estar intactos.
 *
 * <p>Hoy el motor sólo trae la regla de stock; cada regla nueva suma acá su caso positivo y su caso de control.
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
    private List<ReglaAviso> reglas;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private RegistroRepository registroRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @Autowired
    private AgregadoZonaRepository agregadoZonaRepository;

    @Autowired
    private TransaccionPort transaccionPort;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private AplicarAviso aplicarAviso;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("El almacén demo recibe un aviso por cada producto agotado o bajo el mínimo y ninguno por los demás")
    void ejecutarShouldCreateStockAvisosOnlyForLowProductsOfDemo() {
        UUID demoId = negocioDemo();

        ejecutarMotor(NegocioId.de(demoId), AHORA);

        List<Aviso> avisos = pendientesDe(demoId, TipoAviso.STOCK);
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

        ejecutarMotor(NegocioId.de(demoId), AHORA);

        assertThat(pendientesDe(demoId, TipoAviso.STOCK))
                .allSatisfy(aviso -> assertThat(aviso.getAcciones().toString())
                        .contains("REGISTRAR").contains("Registrar entrada"));
    }

    @Test
    @DisplayName("Correr el motor dos veces no duplica los avisos")
    void ejecutarShouldBeIdempotent() {
        UUID demoId = negocioDemo();

        ejecutarMotor(NegocioId.de(demoId), AHORA);
        ejecutarMotor(NegocioId.de(demoId), AHORA.plusMinutes(5));

        assertThat(delTipo(avisoRepository.findByNegocioId(NegocioId.de(demoId)), TipoAviso.STOCK)).hasSize(3);
    }

    @Test
    @DisplayName("Un aviso descartado no reaparece hasta que cambian los datos de origen")
    void ejecutarShouldKeepDiscardedAvisoUntilDatosChange() {
        UUID demoId = negocioDemo();
        ejecutarMotor(NegocioId.de(demoId), AHORA);
        Aviso yerba = pendientesDe(demoId, TipoAviso.STOCK).stream()
                .filter(aviso -> aviso.getTitulo().startsWith("Yerba")).findFirst().orElseThrow();
        yerba.descartar(AHORA);
        avisoRepository.save(yerba);

        ejecutarMotor(NegocioId.de(demoId), AHORA.plusMinutes(5));

        assertThat(pendientesDe(demoId, TipoAviso.STOCK)).hasSize(2);

        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '1') WHERE id = ?",
                UUID.fromString(yerba.getEntidad().substring("registro:".length())));
        entityManager.clear();
        ejecutarMotor(NegocioId.de(demoId), AHORA.plusMinutes(10));

        assertThat(pendientesDe(demoId, TipoAviso.STOCK))
                .extracting(Aviso::getTitulo).anyMatch(titulo -> titulo.startsWith("Yerba"));
    }

    @Test
    @DisplayName("Un producto que repone stock deja de tener aviso y el aviso vence")
    void ejecutarShouldExpireAvisoWhenStockIsRestored() {
        UUID demoId = negocioDemo();
        ejecutarMotor(NegocioId.de(demoId), AHORA);
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '50')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Aceite Natura%'", demoId);
        entityManager.clear();

        ejecutarMotor(NegocioId.de(demoId), AHORA.plusMinutes(5));

        assertThat(pendientesDe(demoId, TipoAviso.STOCK)).hasSize(2);
        assertThat(delTipo(avisoRepository.findByNegocioIdAndEstado(NegocioId.de(demoId), EstadoAviso.VENCIDO), TipoAviso.STOCK)).hasSize(1);
    }

    @Test
    @DisplayName("El azúcar del demo, cara y sin ventas hace más de un mes, recibe el aviso de rebajar el precio")
    void executeShouldCreateEstancadoAvisoForAzucarOfDemo() {
        UUID demoId = negocioDemo();

        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now());

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).singleElement().satisfies(aviso -> {
            assertThat(aviso.getTitulo()).isEqualTo("Azúcar Ledesma 1 kg está caro y no se mueve");
            assertThat(aviso.getPrioridad()).isEqualTo((short) 2);
            assertThat(aviso.getAcciones().toString()).contains("EDITAR_REGISTRO").contains("Rebajar a $1.150");
            assertThat(aviso.getMotivos().getFirst().toString()).startsWith("En tu zona, 8 comercios lo venden");
        });
    }

    @Test
    @DisplayName("Los productos que el demo vende normalmente no reciben el aviso de producto estancado")
    void executeShouldNotCreateEstancadoAvisoForProductsThatSell() {
        UUID demoId = negocioDemo();

        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now());

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).extracting(Aviso::getTitulo)
                .noneMatch(titulo -> titulo.startsWith("Yerba") || titulo.startsWith("Coca"));
    }

    @Test
    @DisplayName("Rebajar el azúcar hace desaparecer el aviso de producto estancado")
    void executeShouldExpireEstancadoAvisoWhenPriceIsLowered() {
        UUID demoId = negocioDemo();
        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now());
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'price')], '1150')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Azúcar Ledesma%'", demoId);
        entityManager.clear();

        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now().plusMinutes(5));

        assertThat(pendientesDe(demoId, TipoAviso.ESTANCADO)).isEmpty();
    }

    @Test
    @DisplayName("El alfajor y las galletitas del demo, que salen poco pero siempre juntos, reciben el aviso de combo")
    void executeShouldCreateComboAvisoForAlfajorAndGalletitasOfDemo() {
        UUID demoId = negocioDemo();

        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now());

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
    void aplicarShouldCreateComboProductWhenAvisoIsApplied() {
        UUID demoId = negocioDemo();
        jdbc.update("UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'PREMIUM')"
                + " WHERE negocio_id = ?", demoId);
        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now());
        Aviso combo = pendientesDe(demoId, TipoAviso.COMBO).getFirst();
        UsuarioId duenio = UsuarioId.de(jdbc.queryForObject(
                "SELECT duenio_id FROM negocio WHERE id = ?", UUID.class, demoId));

        aplicarAviso.execute(new ResolverAvisoRequest(duenio, combo.getId()));
        entityManager.flush();
        entityManager.clear();
        ejecutarMotor(NegocioId.de(demoId), OffsetDateTime.now().plusMinutes(5));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?"
                + " AND valores::text LIKE '%\"Combo %'", Integer.class, demoId))
                .isEqualTo(1);
        assertThat(pendientesDe(demoId, TipoAviso.COMBO)).isEmpty();
        assertThat(pendientesDe(demoId, TipoAviso.STOCK)).noneMatch(aviso -> aviso.getTitulo().contains("Combo"));
    }

    private UUID negocioDemo() {
        return jdbc.queryForObject("SELECT n.id FROM negocio n JOIN usuario u ON u.id = n.duenio_id"
                + " WHERE u.email = 'demo@ensambla.test'", UUID.class);
    }
    /** El motor real sobre el negocio demo y en el instante del test; los demás negocios de la base no se tocan. */
    private void ejecutarMotor(NegocioId negocioId, OffsetDateTime ahora) {
        NegocioRepository soloEsteNegocio = mock(NegocioRepository.class);
        when(soloEsteNegocio.findAllIds()).thenReturn(List.of(negocioId));
        when(soloEsteNegocio.findZonaIdById(negocioId)).thenReturn(negocioRepository.findZonaIdById(negocioId));
        new EjecutarMotorAvisos(soloEsteNegocio, reglas, proyectoRepository, registroRepository, zonaRepository,
                agregadoZonaRepository, avisoRepository, transaccionPort, Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute();
    }

    private List<Aviso> pendientesDe(UUID negocioId, TipoAviso tipo) {
        return delTipo(avisoRepository.findByNegocioIdAndEstado(NegocioId.de(negocioId), EstadoAviso.PENDIENTE), tipo);
    }

    private static List<Aviso> delTipo(List<Aviso> avisos, TipoAviso tipo) {
        return avisos.stream().filter(aviso -> aviso.getTipo() == tipo).toList();
    }
}
