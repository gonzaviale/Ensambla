package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ComoVieneElNegocioIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/objetivos/negocio";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SistemaRepository sistemaRepository;

    private MockMvc mockMvc;
    private MockHttpSession sesion;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenNegocioConVentas(60);
        givenNegocioConVentas(1000);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        sesion = new MockHttpSession();
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }

    @Test
    @DisplayName("El endpoint exige la sesión del negocio")
    void obtenerShouldRequireSession() throws Exception {
        mockMvc.perform(get(API))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Calcula desde PostgreSQL cómo viene el negocio de la sesión, sin sumar los cuadernos de otro negocio")
    void obtenerShouldReturnTheBusinessOfTheSessionFromPostgres() throws Exception {
        mockMvc.perform(get(API).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayVentas").value(true))
                .andExpect(jsonPath("$.vendiste").value(229500))
                .andExpect(jsonPath("$.mercaderia").value(171750))
                .andExpect(jsonPath("$.gastos").value(0))
                .andExpect(jsonPath("$.queda").value(57750))
                .andExpect(jsonPath("$.teDeben").value(0))
                .andExpect(jsonPath("$.semanas.length()").value(6))
                .andExpect(jsonPath("$.semanas[5].semana").value("Hoy"))
                .andExpect(jsonPath("$.semanas[5].queda").value(57750))
                .andExpect(jsonPath("$.semanas[4].queda").value(0));
    }

    // --- arrange ---
    /** Un usuario con su negocio y un almacén que hoy vendió {@code yerbas} yerbas y 5 gaseosas; devuelve el usuario. */
    private UUID givenNegocioConVentas(int yerbas) {
        UUID usuarioId = UUID.randomUUID();
        UUID negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Cómo viene HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Cómo viene HTTP', ?)",
                negocioId, usuarioId);
        guardarSistema(negocioId, yerbas);
        return usuarioId;
    }

    private void guardarSistema(UUID negocioId, int yerbas) {
        String sistemaId = UUID.randomUUID().toString();
        String seccionId = frontId();
        String inventarioId = frontId();
        String ventasId = frontId();
        String yerbaId = frontId();
        String azucarId = frontId();
        String cocaColaId = frontId();
        BloqueItem inventario = new BloqueItem(
                inventarioId, "inventory", "Inventario", "", "orange", "full",
                List.of(
                        campo("name", "Producto", "text"),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("cost", "Costo", "currency")),
                List.of(
                        fila(yerbaId, "name", "Yerba Playadito 1 kg", "quantity", 4,
                                "price", 3600, "cost", 2700),
                        fila(azucarId, "name", "Azúcar Ledesma 1 kg", "quantity", 25,
                                "price", 1500, "cost", 900),
                        fila(cocaColaId, "name", "Coca-Cola 2,25 L", "quantity", 18,
                                "price", 2700, "cost", 1950)),
                "", false, null);
        BloqueItem ventas = new BloqueItem(
                ventasId, "sales", "Ventas", "", "yellow", "full",
                List.of(
                        new CampoItem("product", "Producto", "relation", true, null, null, inventarioId),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("date", "Fecha", "date")),
                List.of(
                        fila(frontId(), "product", yerbaId, "quantity", yerbas,
                                "price", 3600, "date", hoyEnElNegocio()),
                        fila(frontId(), "product", cocaColaId, "quantity", 5,
                                "price", 2700, "date", hoyEnElNegocio())),
                "", false, null);
        SistemaItem item = new SistemaItem(
                sistemaId, "Almacén de cómo viene", OffsetDateTime.now().toString(), null,
                List.of(new SeccionItem(seccionId, "Vista general", List.of(inventario, ventas))),
                "shop", new OpcionesItem(true, false), "mosaic", false);

        sistemaRepository.guardar(item.aSistema(NegocioId.de(negocioId), sistemaId, OffsetDateTime.now()));
    }

    private static CampoItem campo(String clave, String nombre, String tipo) {
        return new CampoItem(clave, nombre, tipo, true, null, null, null);
    }

    private static Map<String, Object> fila(String id, Object... clavesYValores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", id);
        for (int indice = 0; indice < clavesYValores.length; indice += 2) {
            fila.put((String) clavesYValores[indice], clavesYValores[indice + 1]);
        }
        return fila;
    }

    /** El día del negocio (Argentina), no el del servidor: pasadas las 21:00 hora local el UTC ya es el día siguiente. */
    private static String hoyEnElNegocio() {
        return ZonaNegocio.dia(OffsetDateTime.now()).toString();
    }

    private static String frontId() {
        return "e" + UUID.randomUUID();
    }
}
