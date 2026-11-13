package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El panel de la home de punta a punta: sesión, base de datos real y las tablas de un sistema cargadas como las
 * guarda la aplicación. Verifica el aislamiento por negocio y que los números sean los esperados.
 */
@Transactional
class PanelNegocioIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IPlanJpaRepository planes;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;
    private final LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        givenPlanGratuito();
        UsuarioId usuarioId = UsuarioId.de(givenUsuario());
        negocioId = NegocioId.de(givenNegocio(usuarioId));
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Devuelve las métricas y el flujo de caja de un sistema cargado en la base")
    void obtenerShouldReturnTheMetricsAndTheCashFlowOfALoadedProject() throws Exception {
        UUID proyectoId = givenSistemaCargado(negocioId);

        mockMvc.perform(get("/api/negocio/panel").session(sesion)
                        .param("proyectoId", proyectoId.toString()).param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayDatos").value(true))
                .andExpect(jsonPath("$.dias").value(30))
                .andExpect(jsonPath("$.hasta").value(hoy.toString()))
                .andExpect(jsonPath("$.metricas.vendiste").value(54400))
                .andExpect(jsonPath("$.metricas.teDeben").value(17000))
                .andExpect(jsonPath("$.metricas.gastos").value(5000))
                .andExpect(jsonPath("$.metricas.mercaderia").value(13500))
                .andExpect(jsonPath("$.metricas.queda").value(18900))
                .andExpect(jsonPath("$.flujoDeCaja.length()").value(5))
                .andExpect(jsonPath("$.flujoDeCaja[4].saldoAcumulado").value(18900));
    }

    @Test
    @DisplayName("Un sistema sin ventas responde vacío, sin métricas ni semanas")
    void obtenerShouldRespondEmptyForAProjectWithoutSales() throws Exception {
        UUID proyectoId = givenProyecto(negocioId, "Sin ventas");

        mockMvc.perform(get("/api/negocio/panel").session(sesion).param("proyectoId", proyectoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayDatos").value(false))
                .andExpect(jsonPath("$.metricas").doesNotExist())
                .andExpect(jsonPath("$.flujoDeCaja.length()").value(0));
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde 404, aunque tenga ventas")
    void obtenerShouldHideTheProjectOfAnotherBusiness() throws Exception {
        NegocioId otroNegocioId = NegocioId.de(givenNegocio(UsuarioId.de(givenUsuario())));
        UUID ajenoId = givenSistemaCargado(otroNegocioId);

        mockMvc.perform(get("/api/negocio/panel").session(sesion).param("proyectoId", ajenoId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin sesión responde 401 y con días inválidos o un id mal escrito, 400")
    void obtenerShouldRequireASessionAndValidParameters() throws Exception {
        UUID proyectoId = givenProyecto(negocioId, "Cualquiera");

        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", proyectoId.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/negocio/panel").session(sesion)
                        .param("proyectoId", proyectoId.toString()).param("dias", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/negocio/panel").session(sesion).param("proyectoId", "no-es-un-id"))
                .andExpect(status().isBadRequest());
    }

    // --- arrange ---

    /**
     * Inventario con yerba (costo 2700) y fideos (sin costo), tres ventas dentro del período (una fiada), una vieja,
     * un gasto, un gasto de mercadería (no cuenta) y una compra de 5 yerbas en la tabla de entradas.
     */
    private UUID givenSistemaCargado(NegocioId negocio) {
        UUID proyecto = givenProyecto(negocio, "Almacén");
        UUID inventario = givenBloque(negocio, proyecto, TipoBloque.INVENTARIO);
        UUID ventas = givenBloque(negocio, proyecto, TipoBloque.VENTAS);
        UUID gastos = givenBloque(negocio, proyecto, TipoBloque.GASTOS);
        UUID entradas = givenBloque(negocio, proyecto, TipoBloque.ENTRADAS);
        Map<String, UUID> inv = givenCampos(negocio, inventario,
                Map.of("name", TipoCampo.TEXTO, "price", TipoCampo.MONEDA, "cost", TipoCampo.MONEDA));
        Map<String, UUID> ven = givenCampos(negocio, ventas, Map.of("product", TipoCampo.TEXTO,
                "quantity", TipoCampo.NUMERO, "price", TipoCampo.MONEDA, "date", TipoCampo.FECHA,
                "credit", TipoCampo.SI_NO));
        Map<String, UUID> gas = givenCampos(negocio, gastos, Map.of("date", TipoCampo.FECHA,
                "total", TipoCampo.MONEDA, "category", TipoCampo.TEXTO));
        Map<String, UUID> ent = givenCampos(negocio, entradas, Map.of("product", TipoCampo.TEXTO,
                "quantity", TipoCampo.NUMERO, "date", TipoCampo.FECHA));

        UUID yerba = givenRegistro(negocio, inventario, valores(inv, "name", "Yerba", "price", 3400, "cost", 2700));
        UUID fideos = givenRegistro(negocio, inventario, valores(inv, "name", "Fideos", "price", 1700));
        givenRegistro(negocio, ventas, venta(ven, yerba, 10, 3400, hoy.minusDays(20), false));
        givenRegistro(negocio, ventas, venta(ven, yerba, 5, 3400, hoy.minusDays(10), true));
        givenRegistro(negocio, ventas, venta(ven, fideos, 2, 1700, hoy.minusDays(2), false));
        givenRegistro(negocio, ventas, venta(ven, yerba, 100, 3400, hoy.minusDays(90), false));
        givenRegistro(negocio, gastos, valores(gas, "date", hoy.minusDays(15).toString(), "total", 5000));
        givenRegistro(negocio, gastos,
                valores(gas, "date", hoy.minusDays(14).toString(), "total", 9000, "category", "Mercadería"));
        givenRegistro(negocio, entradas,
                valores(ent, "product", yerba.toString(), "quantity", 5, "date", hoy.minusDays(18).toString()));
        return proyecto;
    }

    private Map<String, Object> venta(Map<String, UUID> campos, UUID producto, int cantidad, int precio,
            LocalDate fecha, boolean fiado) {
        return valores(campos, "product", producto.toString(), "quantity", cantidad, "price", precio,
                "date", fecha.toString(), "credit", fiado);
    }

    private Map<String, Object> valores(Map<String, UUID> campos, Object... clavesYValores) {
        Map<String, Object> valores = new HashMap<>();
        for (int i = 0; i < clavesYValores.length; i += 2) {
            valores.put(campos.get((String) clavesYValores[i]).toString(), clavesYValores[i + 1]);
        }
        return valores;
    }

    private UUID givenProyecto(NegocioId negocio, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocio.valor())
                .nombre(nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenBloque(NegocioId negocio, UUID proyecto, TipoBloque tipo) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocio.valor())
                .proyectoId(proyecto)
                .nombre(tipo.name())
                .tipo(tipo)
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private Map<String, UUID> givenCampos(NegocioId negocio, UUID bloque, Map<String, TipoCampo> porClave) {
        Map<String, UUID> ids = new HashMap<>();
        short posicion = 0;
        for (Map.Entry<String, TipoCampo> campo : porClave.entrySet()) {
            ids.put(campo.getKey(), campos.saveAndFlush(CampoEntity.builder()
                    .negocioId(negocio.valor())
                    .bloqueId(bloque)
                    .nombre(campo.getKey())
                    .clave(campo.getKey())
                    .tipo(campo.getValue())
                    .obligatorio(false)
                    .opciones(List.of())
                    .posicion(posicion++)
                    .creadoEn(AHORA)
                    .actualizadoEn(AHORA)
                    .build()).getId());
        }
        return ids;
    }

    private UUID givenRegistro(NegocioId negocio, UUID bloque, Map<String, Object> valores) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocio.valor())
                .bloqueId(bloque)
                .valores(valores)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenNegocio(UsuarioId usuarioId) {
        UUID creadoId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creadoId), AHORA.minusDays(1));
        return creadoId;
    }

    private void givenPlanGratuito() {
        if (planes.findByCodigo(CodigoPlan.GRATUITO).isEmpty()) {
            planes.saveAndFlush(PlanEntity.builder()
                    .codigo(CodigoPlan.GRATUITO).nombre("Gratuito").configurado(false)
                    .limitesConfigurados(true).limiteNegocios(1).limiteRegistros(1000L)
                    .limitePaginasIaMensuales(10).funciones(List.of())
                    .creadoEn(AHORA).actualizadoEn(AHORA).build());
        }
    }

    private MockHttpSession givenSesion(UsuarioId usuarioId) {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }
}
