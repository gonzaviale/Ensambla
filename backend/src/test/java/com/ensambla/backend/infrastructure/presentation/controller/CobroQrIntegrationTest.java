package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CobroQrIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/caja/cobros-qr";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private UUID negocioId;
    private UUID cobroId;
    private SesionHttpDePrueba sesion;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = insertarUsuario();
        negocioId = UUID.randomUUID();
        cobroId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id, cobro_alias, cobro_titular) "
                + "VALUES (?, 'Mi negocio', ?, 'mi.alias.mp', 'Ana Pérez')", negocioId, usuarioId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Generar un cobro con QR exige sesión")
    void generarUnCobroConQrExigeSesion() throws Exception {
        cuandoSeGeneraElCobroSinSesion();

        entoncesResponde(401);
    }

    @Test
    @DisplayName("Generar un cobro con QR exige CSRF")
    void generarUnCobroConQrExigeCsrf() throws Exception {
        cuandoSeGeneraElCobroSinTokenCsrf();

        entoncesResponde(403);
    }

    @Test
    @DisplayName("Cancelar un cobro con QR exige sesión")
    void cancelarUnCobroConQrExigeSesion() throws Exception {
        cuandoSeCancelaElCobroSinSesion();

        entoncesResponde(401);
    }

    @Test
    @DisplayName("Genera el cobro pendiente con el alias del negocio y lo guarda en PostgreSQL")
    void generaElCobroPendienteConElAliasDelNegocioYLoGuardaEnPostgresql() throws Exception {
        cuandoSeGeneraElCobro();

        entoncesSeResponde200ConElCobro("PENDIENTE");
        entoncesLaBaseGuardaUnCobroEn("PENDIENTE");
    }

    @Test
    @DisplayName("Generar dos veces el mismo cobro guarda uno solo")
    void generarDosVecesElMismoCobroGuardaUnoSolo() throws Exception {
        cuandoSeGeneraElCobro();
        cuandoSeGeneraElCobro();

        entoncesSeResponde200ConElCobro("PENDIENTE");
        entoncesLaBaseTieneCobros(1);
    }

    @Test
    @DisplayName("Sin alias cargado responde 400 y no guarda ningún cobro")
    void sinAliasCargadoResponde400YNoGuardaNingunCobro() throws Exception {
        dadoUnNegocioSinAlias();

        cuandoSeGeneraElCobro();

        entoncesResponde(400);
        entoncesLaBaseTieneCobros(0);
    }

    @Test
    @DisplayName("Cancela el cobro pendiente y queda cancelado en PostgreSQL")
    void cancelaElCobroPendienteYQuedaCanceladoEnPostgresql() throws Exception {
        dadoUnCobroGenerado();

        cuandoSeCancelaElCobro();

        entoncesSeResponde200ConElCobro("CANCELADO");
        entoncesLaBaseGuardaUnCobroEn("CANCELADO");
    }

    @Test
    @DisplayName("Cancelar dos veces el mismo cobro responde 400")
    void cancelarDosVecesElMismoCobroResponde400() throws Exception {
        dadoUnCobroGenerado();
        cuandoSeCancelaElCobro();

        cuandoSeCancelaElCobro();

        entoncesResponde(400);
    }

    @Test
    @DisplayName("Un cobro de otro negocio responde 404 y no cambia")
    void unCobroDeOtroNegocioResponde404YNoCambia() throws Exception {
        dadoUnCobroDeOtroNegocio();

        cuandoSeCancelaElCobro();

        entoncesResponde(404);
        entoncesLaBaseGuardaUnCobroEn("PENDIENTE");
    }

    // --- dado ---
    private void dadoUnNegocioSinAlias() {
        jdbcTemplate.update("UPDATE negocio SET cobro_alias = NULL, cobro_titular = NULL WHERE id = ?", negocioId);
    }

    private void dadoUnCobroGenerado() throws Exception {
        cuandoSeGeneraElCobro();
    }

    private void dadoUnCobroDeOtroNegocio() {
        UUID duenioAjeno = insertarUsuario();
        UUID negocioAjeno = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Ajeno', ?)",
                negocioAjeno, duenioAjeno);
        jdbcTemplate.update("INSERT INTO cobro_qr (id, negocio_id, alias, importe, estado, creado_en, vence_en, "
                + "actualizado_en) VALUES (?, ?, 'otro.alias', 100, 'PENDIENTE', now(), "
                + "now() + interval '15 minutes', now())", cobroId, negocioAjeno);
    }

    // --- cuando ---
    private void cuandoSeGeneraElCobroSinSesion() throws Exception {
        respuesta = mockMvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeGenerar()));
    }

    private void cuandoSeGeneraElCobroSinTokenCsrf() throws Exception {
        respuesta = mockMvc.perform(post(API).session(sesion.sesion())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoDeGenerar()));
    }

    private void cuandoSeCancelaElCobroSinSesion() throws Exception {
        respuesta = mockMvc.perform(post(API + "/" + cobroId + "/cancelar"));
    }

    private void cuandoSeGeneraElCobro() throws Exception {
        respuesta = mockMvc.perform(post(API).session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoDeGenerar()));
    }

    private void cuandoSeCancelaElCobro() throws Exception {
        respuesta = mockMvc.perform(post(API + "/" + cobroId + "/cancelar").session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    // --- entonces ---
    private void entoncesResponde(int estado) throws Exception {
        respuesta.andExpect(status().is(estado));
    }

    private void entoncesSeResponde200ConElCobro(String estado) throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cobroId.toString()))
                .andExpect(jsonPath("$.importe").value(2500.5))
                .andExpect(jsonPath("$.alias").value("mi.alias.mp"))
                .andExpect(jsonPath("$.titular").value("Ana Pérez"))
                .andExpect(jsonPath("$.estado").value(estado));
    }

    private void entoncesLaBaseGuardaUnCobroEn(String estado) {
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM cobro_qr WHERE id = ?", String.class, cobroId))
                .isEqualTo(estado);
    }

    private void entoncesLaBaseTieneCobros(int cantidad) {
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cobro_qr WHERE id = ?", Integer.class, cobroId))
                .isEqualTo(cantidad);
    }

    // --- datos ---
    private String cuerpoDeGenerar() {
        return "{\"id\":\"" + cobroId + "\",\"importe\":2500.50}";
    }

    private UUID insertarUsuario() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')", id,
                id + "@example.test");
        return id;
    }
}
