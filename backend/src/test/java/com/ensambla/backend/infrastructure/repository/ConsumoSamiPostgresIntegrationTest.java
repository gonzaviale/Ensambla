package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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

    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private final List<Boolean> reservas = new ArrayList<>();
    private int reservadas;

    @BeforeEach
    void setUp() {
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba Sami')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba Sami', ?)",
                negocioId.valor(), usuarioId.valor());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId.valor());
    }

    @Test
    @DisplayName("Reserva consultas hasta el límite del día y rechaza la siguiente")
    void reservaConsultasHastaElLimiteDelDiaYRechazaLaSiguiente() {
        cuandoSeReservaTresVecesConLimiteDeDos();

        entoncesSeAceptanLasDosPrimerasYSeRechazaLaTercera();
    }

    @Test
    @DisplayName("Con límite cero no reserva ni crea la fila del día")
    void conLimiteCeroNoReservaNiCreaLaFilaDelDia() {
        cuandoSeReservaUnaVezConLimite(0);

        entoncesNoSeReservaNiSeCreaLaFilaDelDia();
    }

    @Test
    @DisplayName("Cada día tiene su propio cupo")
    void cadaDiaTieneSuPropioCupo() {
        cuandoSeAgotaElCupoDeHoyYSePideElDeManana();

        entoncesElCupoDeMananaEstaDisponible();
    }

    @Test
    @DisplayName("Pedidos simultáneos del mismo negocio nunca pasan el límite")
    void pedidosSimultaneosDelMismoNegocioNuncaPasanElLimite() throws Exception {
        cuandoSeReservaDoceVecesALaVezConLimiteDeTres();

        entoncesSoloSeReservanTres();
    }

    @Test
    @DisplayName("Registra los tokens y el modelo de cada consulta reservada")
    void registraLosTokensYElModeloDeCadaConsultaReservada() {
        dadoDosConsultasReservadas();

        cuandoSeRegistraElConsumoDeAmbas();

        entoncesSeSumanLosTokensYSeConservaElUltimoModelo();
    }

    // --- dado ---

    private void dadoDosConsultasReservadas() {
        consumoSamiRepository.reservar(negocioId, HOY, 5, AHORA);
        consumoSamiRepository.reservar(negocioId, HOY, 5, AHORA);
    }

    // --- cuando ---

    private void cuandoSeReservaTresVecesConLimiteDeDos() {
        for (int i = 0; i < 3; i++) {
            reservas.add(consumoSamiRepository.reservar(negocioId, HOY, 2, AHORA));
        }
    }

    private void cuandoSeReservaUnaVezConLimite(int limite) {
        reservas.add(consumoSamiRepository.reservar(negocioId, HOY, limite, AHORA));
    }

    private void cuandoSeAgotaElCupoDeHoyYSePideElDeManana() {
        reservas.add(consumoSamiRepository.reservar(negocioId, HOY, 1, AHORA));
        reservas.add(consumoSamiRepository.reservar(negocioId, HOY, 1, AHORA));
        reservas.add(consumoSamiRepository.reservar(negocioId, HOY.plusDays(1), 1, AHORA));
    }

    private void cuandoSeReservaDoceVecesALaVezConLimiteDeTres() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                return consumoSamiRepository.reservar(negocioId, HOY, 3, AHORA);
            }));
        }
        salida.countDown();
        for (Future<Boolean> resultado : resultados) {
            reservadas += resultado.get(30, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();
    }

    private void cuandoSeRegistraElConsumoDeAmbas() {
        consumoSamiRepository.registrar(negocioId, HOY, new ConsumoIaSami("bedrock", "haiku", 100, 20), AHORA);
        consumoSamiRepository.registrar(negocioId, HOY,
                new ConsumoIaSami("bedrock", "haiku + sonnet", 250, 50), AHORA);
    }

    // --- entonces ---

    private void entoncesSeAceptanLasDosPrimerasYSeRechazaLaTercera() {
        assertThat(reservas).containsExactly(true, true, false);
        assertThat(consultas(HOY)).isEqualTo(2);
    }

    private void entoncesNoSeReservaNiSeCreaLaFilaDelDia() {
        assertThat(reservas).containsExactly(false);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM consumo_sami_diario WHERE negocio_id = ?", Integer.class, negocioId.valor()))
                .isZero();
    }

    private void entoncesElCupoDeMananaEstaDisponible() {
        assertThat(reservas).containsExactly(true, false, true);
    }

    private void entoncesSoloSeReservanTres() {
        assertThat(reservadas).isEqualTo(3);
        assertThat(consultas(HOY)).isEqualTo(3);
    }

    private void entoncesSeSumanLosTokensYSeConservaElUltimoModelo() {
        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT consultas, tokens_entrada, tokens_salida, modelo FROM consumo_sami_diario"
                        + " WHERE negocio_id = ? AND fecha = ?", negocioId.valor(), HOY);
        assertThat(fila.get("consultas")).isEqualTo(2);
        assertThat(fila.get("tokens_entrada")).isEqualTo(350L);
        assertThat(fila.get("tokens_salida")).isEqualTo(70L);
        assertThat(fila.get("modelo")).isEqualTo("haiku + sonnet");
    }

    // --- datos ---

    private int consultas(LocalDate fecha) {
        return jdbcTemplate.queryForObject(
                "SELECT consultas FROM consumo_sami_diario WHERE negocio_id = ? AND fecha = ?",
                Integer.class, negocioId.valor(), fecha);
    }
}
