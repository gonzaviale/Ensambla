package com.ensambla.backend.infrastructure.presentation.controller;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/objetivos/pasos";

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
        UUID usuarioId = UUID.randomUUID();
        UUID negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Objetivos HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Objetivos HTTP', ?)",
                negocioId, usuarioId);
        guardarSistema(negocioId);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        sesion = new MockHttpSession();
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }

    @Test
    @DisplayName("El endpoint exige la sesión del negocio")
    void pasosShouldRequireSession() throws Exception {
        mockMvc.perform(get(API))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devuelve desde PostgreSQL los tres productos elegidos para el almacén de demostración")
    void pasosShouldReturnProductsFromPostgres() throws Exception {
        mockMvc.perform(get(API).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$[0].nombre").value("Yerba Playadito 1 kg"))
                .andExpect(jsonPath("$[0].precio").value(3600))
                .andExpect(jsonPath("$[0].costo").value(2700))
                .andExpect(jsonPath("$[0].stock").value(4))
                .andExpect(jsonPath("$[0].precioZona").isEmpty())
                .andExpect(jsonPath("$[1].tipo").value("MOVER_PARADO"))
                .andExpect(jsonPath("$[1].nombre").value("Azúcar Ledesma 1 kg"))
                .andExpect(jsonPath("$[1].vendidas30Dias").value(0))
                .andExpect(jsonPath("$[2].tipo").value("PONER_A_LA_VISTA"))
                .andExpect(jsonPath("$[2].nombre").value("Coca-Cola 2,25 L"));
    }

    // --- arrange ---
    private void guardarSistema(UUID negocioId) {
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
                        fila(frontId(), "product", yerbaId, "quantity", 60,
                                "price", 3600, "date", LocalDate.now().toString()),
                        fila(frontId(), "product", cocaColaId, "quantity", 5,
                                "price", 2700, "date", LocalDate.now().toString())),
                "", false, null);
        SistemaItem item = new SistemaItem(
                sistemaId, "Almacén de objetivos", OffsetDateTime.now().toString(), null,
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

    private static String frontId() {
        return "e" + UUID.randomUUID();
    }
}
