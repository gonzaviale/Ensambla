package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los endpoints que carga la pantalla al entrar: sistemas, preferencias y zona. */
@Transactional
class SistemaSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String SISTEMA_ID = "e0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b";
    private static final UUID SISTEMA_UUID = UUID.fromString("0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b");

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

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
    private IProyectoJpaRepository proyectos;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;
    private String csrfToken;

    private ResultActions respuesta;
    private ResultActions segundaRespuesta;
    private ResultActions terceraRespuesta;
    private String body;

    private MockHttpSession ajena;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(dadoUsuario());
        negocioId = NegocioId.de(dadoNegocio(usuarioId));
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("Un sistema recién guardado vuelve completo en la lista, con ids, columnas y filas")
    void unSistemaRecienGuardadoVuelveCompletoEnLaListaConIdsColumnasYFilas() throws Exception {
        cuandoSeEjecutaUnSistemaRecienGuardadoVuelveCompletoEnLaListaConIdsColumnasYFilas();

        entoncesSeVerificaUnSistemaRecienGuardadoVuelveCompletoEnLaListaConIdsColumnasYFilas();
    }

    @Test
    @DisplayName("Guardar otra vez reemplaza en vez de duplicar")
    void guardarOtraVezReemplazaEnVezDeDuplicar() throws Exception {
        cuandoSeEjecutaGuardarOtraVezReemplazaEnVezDeDuplicar();

        entoncesSeVerificaGuardarOtraVezReemplazaEnVezDeDuplicar();
    }

    @Test
    @DisplayName("La lista contiene sólo sistemas del negocio autenticado")
    void laListaContieneSoloSistemasDelNegocioAutenticado() throws Exception {
        cuandoSeEjecutaLaListaContieneSoloSistemasDelNegocioAutenticado();

        entoncesSeVerificaLaListaContieneSoloSistemasDelNegocioAutenticado();
    }

    @Test
    @DisplayName("Guardar un sistema que es de otro negocio responde 404 y no lo modifica")
    void guardarUnSistemaQueEsDeOtroNegocioResponde404YNoLoModifica() throws Exception {
        cuandoSeEjecutaGuardarUnSistemaQueEsDeOtroNegocioResponde404YNoLoModifica();

        entoncesSeVerificaGuardarUnSistemaQueEsDeOtroNegocioResponde404YNoLoModifica();
    }

    @Test
    @DisplayName("Un cuerpo inválido responde 400 y no guarda nada")
    void unCuerpoInvalidoResponde400YNoGuardaNada() throws Exception {
        cuandoSeEjecutaUnCuerpoInvalidoResponde400YNoGuardaNada();

        entoncesSeVerificaUnCuerpoInvalidoResponde400YNoGuardaNada();
    }

    @Test
    @DisplayName("Guardar sin CSRF devuelve 403 y sin sesión 401")
    void guardarSinCSRFDevuelve403YSinSesion401() throws Exception {
        cuandoSeEjecutaGuardarSinCSRFDevuelve403YSinSesion401();

        entoncesSeVerificaGuardarSinCSRFDevuelve403YSinSesion401();
    }

    @Test
    @DisplayName("Eliminar borra el sistema y se puede repetir sin error; uno ajeno no se borra")
    void eliminarBorraElSistemaYSePuedeRepetirSinErrorUnoAjenoNoSeBorra() throws Exception {
        dadoUnSistemaPropioGuardadoYUnaSesionAjena();

        cuandoLaSesionAjenaIntentaEliminarElSistema();

        entoncesElSistemaAjenoNoSeBorra();

        cuandoElDuenioEliminaElSistemaDosVeces();

        entoncesElSistemaSeBorraYLaRepeticionNoFalla();
    }

    @Test
    @DisplayName("Sin preferencias guardadas devuelve las iniciales y un PUT las conserva")
    void sinPreferenciasGuardadasDevuelveLasInicialesYUnPUTLasConserva() throws Exception {
        cuandoSeEjecutaSinPreferenciasGuardadasDevuelveLasInicialesYUnPUTLasConserva();

        entoncesSeVerificaSinPreferenciasGuardadasDevuelveLasInicialesYUnPUTLasConserva();
    }

    @Test
    @DisplayName("Un negocio sin zona recibe una zona vacía en vez de un error")
    void unNegocioSinZonaRecibeUnaZonaVaciaEnVezDeUnError() throws Exception {
        cuandoSeEjecutaUnNegocioSinZonaRecibeUnaZonaVaciaEnVezDeUnError();

        entoncesSeVerificaUnNegocioSinZonaRecibeUnaZonaVaciaEnVezDeUnError();
    }

    @Test
    @DisplayName("Analizar un lugar que no es una zona conocida ni una dirección buscable responde 400")
    void analizarUnLugarQueNoEsUnaZonaConocidaNiUnaDireccionBuscableResponde400() throws Exception {
        cuandoSeEjecutaAnalizarUnLugarQueNoEsUnaZonaConocidaNiUnaDireccionBuscableResponde400();

        entoncesSeVerificaAnalizarUnLugarQueNoEsUnaZonaConocidaNiUnaDireccionBuscableResponde400();
    }

    @Test
    @DisplayName("El catálogo incluido requiere sesión y responde desde el backend")
    void elCatalogoIncluidoRequiereSesionYRespondeDesdeElBackend() throws Exception {
        cuandoSeEjecutaElCatalogoIncluidoRequiereSesionYRespondeDesdeElBackend();

        entoncesSeVerificaElCatalogoIncluidoRequiereSesionYRespondeDesdeElBackend();
    }

    @Test
    @DisplayName("Guarda el código como texto y recarga el producto con su stock real desde PostgreSQL")
    void guardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL() throws Exception {
        dadoElEscenarioDeGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL();

        cuandoSeEjecutaGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL();

        entoncesSeVerificaGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL();
    }

    @Test
    @DisplayName("El backend rechaza códigos equivalentes duplicados sin guardar parcialmente")
    void elBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente() throws Exception {
        dadoElEscenarioDeElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente();

        cuandoSeEjecutaElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente();

        entoncesSeVerificaElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente();
    }

    @Test
    @DisplayName("El backend rechaza un código de control inválido aunque se omita la validación del navegador")
    void elBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador() throws Exception {
        dadoElEscenarioDeElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador();

        cuandoSeEjecutaElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador();

        entoncesSeVerificaElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador();
    }

    @Test
    @DisplayName("Un código inválido o una fuente desconocida se rechazan antes de consultar al proveedor")
    void unCodigoInvalidoOUnaFuenteDesconocidaSeRechazanAntesDeConsultarAlProveedor() throws Exception {
        cuandoSeEjecutaUnCodigoInvalidoOUnaFuenteDesconocidaSeRechazanAntesDeConsultarAlProveedor();

        entoncesSeVerificaUnCodigoInvalidoOUnaFuenteDesconocidaSeRechazanAntesDeConsultarAlProveedor();
    }

    // --- dado ---
    private String dadoBodyConCodigos(String primero, String segundo) {
        return dadoBody("Inventario escaneado")
                .replace("\"fields\":[", "\"fields\":[{\"key\":\"barcode\",\"label\":\"Código\",\"type\":\"text\",\"required\":false},"
                        + "{\"key\":\"quantity\",\"label\":\"Cantidad\",\"type\":\"number\",\"required\":true,\"min\":0},")
                .replace("\"name\":\"Harina\",\"price\":1200", "\"name\":\"Harina\",\"price\":1200,\"quantity\":14,\"barcode\":\"" + primero + "\"")
                .replace("\"name\":\"Azúcar\",\"price\":900", "\"name\":\"Azúcar\",\"price\":900,\"quantity\":0,\"barcode\":\"" + segundo + "\"");
    }

    private String dadoBody(String nombre) {
        return """
                {"id":"%s","name":"%s","createdAt":"2026-01-10T09:00:00.000Z","updatedAt":"2026-10-21T10:00:00.000Z",
                 "template":"shop","layout":"mosaic","options":{"movements":true,"expenses":false},"demoAlmacen":false,
                 "sections":[{"id":"e1","name":"Vista general","blocks":[
                   {"id":"eb1","type":"inventory","name":"Productos","description":"","color":"orange","width":"half",
                    "fields":[{"key":"name","label":"Producto","type":"text","required":true},
                              {"key":"price","label":"Precio","type":"currency","required":false,"min":0}],
                    "rows":[{"id":"er1","name":"Harina","price":1200},{"id":"er2","name":"Azúcar","price":900}],
                    "content":""}]}]}
                """.formatted(SISTEMA_ID, nombre);
    }

    private UUID dadoUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID dadoNegocio(UsuarioId usuarioId) {
        UUID creado = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creado), AHORA.minusDays(1));
        return creado;
    }

    private UUID dadoUsuarioConNegocio() {
        UsuarioId usuarioId = UsuarioId.de(dadoUsuario());
        dadoNegocio(usuarioId);
        return usuarioId.valor();
    }

    private MockHttpSession dadoSesion(UsuarioId usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    // --- dado ---

    private void dadoElEscenarioDeGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL() throws Exception {
        body = dadoBodyConCodigos("07790895000997", "77903754");
    }

    private void dadoElEscenarioDeElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente() throws Exception {
        body = dadoBodyConCodigos("77903754", "00000077903754");
    }

    private void dadoElEscenarioDeElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador() throws Exception {
        body = dadoBodyConCodigos("77903755", "77905741");
    }

    private void dadoUnSistemaPropioGuardadoYUnaSesionAjena() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(dadoBody("Propio"))).andExpect(status().isOk());
        ajena = dadoSesion(UsuarioId.de(dadoUsuarioConNegocio()));
    }

    // --- cuando ---

    private void cuandoSeEjecutaUnSistemaRecienGuardadoVuelveCompletoEnLaListaConIdsColumnasYFilas() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(dadoBody("Mi almacén")));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas").session(sesion));
    }

    private void cuandoSeEjecutaGuardarOtraVezReemplazaEnVezDeDuplicar() throws Exception {
        for (String nombre : List.of("Primero", "Segundo")) {
            mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                            .contentType("application/json").content(dadoBody(nombre)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/sistemas").session(sesion))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Segundo"));
    }

    private void cuandoSeEjecutaLaListaContieneSoloSistemasDelNegocioAutenticado() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(dadoBody("Propio")));
        MockHttpSession ajena = dadoSesion(UsuarioId.de(dadoUsuarioConNegocio()));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas").session(ajena));
    }

    private void cuandoSeEjecutaGuardarUnSistemaQueEsDeOtroNegocioResponde404YNoLoModifica() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(dadoBody("Propio")));
        MockHttpSession ajena = dadoSesion(UsuarioId.de(dadoUsuarioConNegocio()));
        segundaRespuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(ajena).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(dadoBody("Robado")));
    }

    private void cuandoSeEjecutaUnCuerpoInvalidoResponde400YNoGuardaNada() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"name\":\"Sin secciones\",\"sections\":[]}"));
    }

    private void cuandoSeEjecutaGuardarSinCSRFDevuelve403YSinSesion401() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                        .contentType("application/json").content(dadoBody("Mi almacén")));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas"));
    }

    private void cuandoSeEjecutaSinPreferenciasGuardadasDevuelveLasInicialesYUnPUTLasConserva() throws Exception {
        respuesta = mockMvc.perform(get("/api/preferencias").session(sesion));
        segundaRespuesta = mockMvc.perform(put("/api/preferencias").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"descartadas\":[\"a1\"],\"grupos\":{\"ZONA\":false},"
                                + "\"bienvenidaVista\":true,\"nombre\":\"Gonza\"}"));
        terceraRespuesta = mockMvc.perform(get("/api/preferencias").session(sesion));
    }

    private void cuandoSeEjecutaUnNegocioSinZonaRecibeUnaZonaVaciaEnVezDeUnError() throws Exception {
        respuesta = mockMvc.perform(get("/api/zona/agregados").session(sesion));
    }

    private void cuandoSeEjecutaAnalizarUnLugarQueNoEsUnaZonaConocidaNiUnaDireccionBuscableResponde400() throws Exception {
        respuesta = mockMvc.perform(post("/api/zona/analisis").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"lugar\":\"xy\"}"));
    }

    private void cuandoSeEjecutaElCatalogoIncluidoRequiereSesionYRespondeDesdeElBackend() throws Exception {
        respuesta = mockMvc.perform(get("/api/catalogos/sepa/7790895000997"));
        segundaRespuesta = mockMvc.perform(get("/api/catalogos/sepa/7790895000997").session(sesion));
    }

    private void cuandoSeEjecutaGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType("application/json").content(body));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas").session(sesion));
    }

    private void cuandoSeEjecutaElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType("application/json").content(body));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas").session(sesion));
    }

    private void cuandoSeEjecutaElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador() throws Exception {
        respuesta = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType("application/json").content(body));
        segundaRespuesta = mockMvc.perform(get("/api/sistemas").session(sesion));
    }

    private void cuandoSeEjecutaUnCodigoInvalidoOUnaFuenteDesconocidaSeRechazanAntesDeConsultarAlProveedor() throws Exception {
        respuesta = mockMvc.perform(get("/api/catalogos/sepa/77903755").session(sesion));
        segundaRespuesta = mockMvc.perform(get("/api/catalogos/desconocida/77903754").session(sesion));
    }

    private void cuandoLaSesionAjenaIntentaEliminarElSistema() throws Exception {
        respuesta = mockMvc.perform(delete("/api/sistemas/{id}", SISTEMA_ID).session(ajena)
                .header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoElDuenioEliminaElSistemaDosVeces() throws Exception {
        segundaRespuesta = mockMvc.perform(delete("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken));
        terceraRespuesta = mockMvc.perform(delete("/api/sistemas/{id}", SISTEMA_ID).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken));
    }

    // --- entonces ---

    private void entoncesSeVerificaUnSistemaRecienGuardadoVuelveCompletoEnLaListaConIdsColumnasYFilas() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SISTEMA_UUID.toString()));
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(SISTEMA_UUID.toString()))
                .andExpect(jsonPath("$[0].name").value("Mi almacén"))
                .andExpect(jsonPath("$[0].template").value("shop"))
                .andExpect(jsonPath("$[0].options.movements").value(true))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].type").value("inventory"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].key").value("name"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[1].key").value("price"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].name").value("Harina"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].price").value(1200))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[1].name").value("Azúcar"));
    }

    private void entoncesSeVerificaGuardarOtraVezReemplazaEnVezDeDuplicar() throws Exception {
    }

    private void entoncesSeVerificaLaListaContieneSoloSistemasDelNegocioAutenticado() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void entoncesSeVerificaGuardarUnSistemaQueEsDeOtroNegocioResponde404YNoLoModifica() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isNotFound());
        assertThat(proyectos.findById(SISTEMA_UUID).orElseThrow().getNombre()).isEqualTo("Propio");
        assertThat(proyectos.findById(SISTEMA_UUID).orElseThrow().getNegocioId()).isEqualTo(negocioId.valor());
    }

    private void entoncesSeVerificaUnCuerpoInvalidoResponde400YNoGuardaNada() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).isEmpty();
    }

    private void entoncesSeVerificaGuardarSinCSRFDevuelve403YSinSesion401() throws Exception {
        respuesta
                .andExpect(status().isForbidden());
        segundaRespuesta
                .andExpect(status().isUnauthorized());
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor())).isEmpty();
    }

    private void entoncesSeVerificaSinPreferenciasGuardadasDevuelveLasInicialesYUnPUTLasConserva() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descartadas.length()").value(0))
                .andExpect(jsonPath("$.bienvenidaVista").value(false))
                .andExpect(jsonPath("$.nombre").doesNotExist());
        segundaRespuesta
                .andExpect(status().isOk());
        terceraRespuesta
                .andExpect(jsonPath("$.descartadas[0]").value("a1"))
                .andExpect(jsonPath("$.grupos.ZONA").value(false))
                .andExpect(jsonPath("$.bienvenidaVista").value(true))
                .andExpect(jsonPath("$.nombre").value("Gonza"));
    }

    private void entoncesSeVerificaUnNegocioSinZonaRecibeUnaZonaVaciaEnVezDeUnError() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(""))
                .andExpect(jsonPath("$.comercios").value(0))
                .andExpect(jsonPath("$.productos.length()").value(0));
    }

    private void entoncesSeVerificaAnalizarUnLugarQueNoEsUnaZonaConocidaNiUnaDireccionBuscableResponde400() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
    }

    private void entoncesSeVerificaElCatalogoIncluidoRequiereSesionYRespondeDesdeElBackend() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.producto.codigo").value("7790895000997"));
    }

    private void entoncesSeVerificaGuardaElCodigoComoTextoYRecargaElProductoConSuStockRealDesdePostgreSQL() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].barcode").value("07790895000997"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].quantity").value(14))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[1].barcode").value("77903754"));
    }

    private void entoncesSeVerificaElBackendRechazaCodigosEquivalentesDuplicadosSinGuardarParcialmente() throws Exception {
        respuesta
                .andExpect(status().isConflict());
        segundaRespuesta
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void entoncesSeVerificaElBackendRechazaUnCodigoDeControlInvalidoAunqueSeOmitaLaValidacionDelNavegador() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        segundaRespuesta
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void entoncesSeVerificaUnCodigoInvalidoOUnaFuenteDesconocidaSeRechazanAntesDeConsultarAlProveedor() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        segundaRespuesta
                .andExpect(status().isBadRequest());
    }

    private void entoncesElSistemaAjenoNoSeBorra() throws Exception {
        respuesta.andExpect(status().isNoContent());
        assertThat(proyectos.findById(SISTEMA_UUID)).isPresent();
    }

    private void entoncesElSistemaSeBorraYLaRepeticionNoFalla() throws Exception {
        segundaRespuesta.andExpect(status().isNoContent());
        terceraRespuesta.andExpect(status().isNoContent());
        assertThat(proyectos.findById(SISTEMA_UUID)).isEmpty();
    }
}
