package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumoSamiPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Autowired
    private ConsumoSamiRepository consumoSamiRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID usuarioId;
    private UUID negocioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba Sami')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba Sami', ?)",
                negocioId, usuarioId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Reserva consultas hasta el límite del día y rechaza la siguiente")
    void reservarShouldAllowUpToTheLimitAndRejectTheNext() {
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 2, AHORA)).isTrue();
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 2, AHORA)).isTrue();
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 2, AHORA)).isFalse();

        assertThat(consultas(HOY)).isEqualTo(2);
    }

    @Test
    @DisplayName("Con límite cero no reserva ni crea la fila del día")
    void reservarShouldRejectWhenLimitIsZero() {
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 0, AHORA)).isFalse();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM consumo_sami_diario WHERE negocio_id = ?", Integer.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Cada día tiene su propio cupo")
    void reservarShouldKeepSeparateQuotaPerDay() {
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 1, AHORA)).isTrue();
        assertThat(consumoSamiRepository.reservar(negocioId, HOY, 1, AHORA)).isFalse();

        assertThat(consumoSamiRepository.reservar(negocioId, HOY.plusDays(1), 1, AHORA)).isTrue();
    }

    @Test
    @DisplayName("Pedidos simultáneos del mismo negocio nunca pasan el límite")
    void reservarShouldNeverExceedTheLimitUnderConcurrency() throws Exception {
        int limite = 3;
        int pedidos = 12;
        ExecutorService pool = Executors.newFixedThreadPool(pedidos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < pedidos; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                return consumoSamiRepository.reservar(negocioId, HOY, limite, AHORA);
            }));
        }

        salida.countDown();
        int reservadas = 0;
        for (Future<Boolean> resultado : resultados) {
            reservadas += resultado.get(30, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();

        assertThat(reservadas).isEqualTo(limite);
        assertThat(consultas(HOY)).isEqualTo(limite);
    }

    @Test
    @DisplayName("Registra los tokens y el modelo de cada consulta reservada")
    void registrarShouldAddTokensAndKeepModel() {
        consumoSamiRepository.reservar(negocioId, HOY, 5, AHORA);
        consumoSamiRepository.reservar(negocioId, HOY, 5, AHORA);

        consumoSamiRepository.registrar(negocioId, HOY, new ConsumoIaSami("bedrock", "haiku", 100, 20), AHORA);
        consumoSamiRepository.registrar(negocioId, HOY, new ConsumoIaSami("bedrock", "haiku + sonnet", 250, 50), AHORA);

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT consultas, tokens_entrada, tokens_salida, modelo FROM consumo_sami_diario"
                        + " WHERE negocio_id = ? AND fecha = ?", negocioId, HOY);
        assertThat(fila.get("consultas")).isEqualTo(2);
        assertThat(fila.get("tokens_entrada")).isEqualTo(350L);
        assertThat(fila.get("tokens_salida")).isEqualTo(70L);
        assertThat(fila.get("modelo")).isEqualTo("haiku + sonnet");
    }

    private int consultas(LocalDate fecha) {
        return jdbcTemplate.queryForObject(
                "SELECT consultas FROM consumo_sami_diario WHERE negocio_id = ? AND fecha = ?",
                Integer.class, negocioId, fecha);
    }
}
