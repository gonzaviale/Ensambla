package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las decisiones de la persona frente a un objetivo de punta a punta con PostgreSQL: se anotan solas al aplicar o
 * descartar un aviso, se vinculan al mini-objetivo del producto y muestran su efecto cuando el impacto ya se midió.
 */
@Transactional
class DecisionesObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.now();
    private static final String ACEPTADO = "{\"tipo\":\"ACEPTADO\"}";

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IAvisoJpaRepository avisos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    private MockMvc mockMvc;
    private SesionHttpDePrueba sesion;
    private UUID negocioId;
    private UUID productoId;
    private String semanaDeHoy;
    private UUID objetivoId;
    private UUID otroObjetivoId;
    private UUID objetivoAlcanzadoId;
    private UUID miniId;
    private UUID avisoId;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = crearUsuario();
        negocioId = crearNegocio(usuarioId);
        productoId = UUID.randomUUID();
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
        semanaDeHoy = SemanaObjetivo.lunesDe(ZonaNegocio.dia(OffsetDateTime.now())).toString();
    }

    @Test
    @DisplayName("Aplicar un aviso deja la decisión en la semana de hoy, vinculada al mini-objetivo del producto")
    void aplicarUnAvisoDejaLaDecisionEnLaSemanaDeHoyVinculadaAlMiniObjetivoDelProducto() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();
        dadoUnAvisoEstancadoAplicado();

        cuandoSeListanLasDecisionesDelObjetivo();

        entoncesLaDecisionQuedaEnLaSemanaDeHoyVinculadaAlMiniObjetivo();
    }

    @Test
    @DisplayName("Cuando el impacto del aviso ya se midió la decisión trae su efecto en el ingreso semanal")
    void cuandoElImpactoDelAvisoYaSeMidioLaDecisionTraeSuEfectoEnElIngresoSemanal() throws Exception {
        dadoUnObjetivoActivo();
        dadoUnAvisoEstancadoAplicado();
        dadoUnImpactoMedidoDelAviso();

        cuandoSeListanLasDecisionesDelObjetivo();

        entoncesLaDecisionTraeSuEfectoMedido();
    }

    @Test
    @DisplayName("Descartar un aviso deja la decisión de descartado, sin medición")
    void descartarUnAvisoDejaLaDecisionDeDescartadoSinMedicion() throws Exception {
        dadoUnObjetivoActivo();
        dadoUnAvisoEstancadoDescartado();

        cuandoSeListanLasDecisionesDelObjetivo();

        entoncesLaDecisionQuedaDescartadaSinMedicion();
    }

    @Test
    @DisplayName("La decisión se anota en cada objetivo activo y no en los que ya llegaron a su meta")
    void laDecisionSeAnotaEnCadaObjetivoActivoYNoEnLosQueYaLlegaronASuMeta() throws Exception {
        dadoDosObjetivosActivosYUnoAlcanzado();
        dadoUnAvisoEstancado();

        cuandoSeAplicaElAviso();

        entoncesSeAnotaUnaDecisionPorObjetivoActivoYNingunaEnElAlcanzado();
    }

    @Test
    @DisplayName("Sin objetivos aplicar un aviso funciona igual y no anota nada")
    void sinObjetivosAplicarUnAvisoFuncionaIgualYNoAnotaNada() throws Exception {
        dadoUnAvisoEstancado();

        cuandoSeAplicaElAviso();

        entoncesElAvisoSeAplicaYNoSeAnotaNingunaDecision();
    }

    @Test
    @DisplayName("Aceptar el paso de un mini-objetivo responde 201 y queda en el historial")
    void aceptarElPasoDeUnMiniObjetivoResponde201YQuedaEnElHistorial() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();

        cuandoSeDecideElPaso(ACEPTADO);

        entoncesElPasoAceptadoQuedaEnElHistorial();
    }

    @Test
    @DisplayName("Ignorar el paso es explícito: queda anotado como ignorado")
    void ignorarElPasoEsExplicitoQuedaAnotadoComoIgnorado() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();

        cuandoSeDecideElPaso("{\"tipo\":\"IGNORADO\"}");

        entoncesElPasoQuedaAnotadoComoIgnorado();
    }

    @Test
    @DisplayName("Decidir el paso de un mini-objetivo que ya no está en curso responde 400")
    void decidirElPasoDeUnMiniObjetivoQueYaNoEstaEnCursoResponde400() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();
        dadoQueElMiniObjetivoFueAbandonado();

        cuandoSeDecideElPaso(ACEPTADO);

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("Una decisión que no es de paso responde 400")
    void unaDecisionQueNoEsDePasoResponde400() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();

        cuandoSeDecideElPaso("{\"tipo\":\"AVISO_APLICADO\"}");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("Una decisión sin cuerpo válido responde 400")
    void unaDecisionSinCuerpoValidoResponde400() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();

        cuandoSeDecideElPaso("{}");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("Listar las decisiones de un objetivo de otro negocio responde 404")
    void listarLasDecisionesDeUnObjetivoDeOtroNegocioResponde404() throws Exception {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeListanLasDecisionesDelObjetivo();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Decidir el paso de un objetivo de otro negocio responde 404 y no se anota nada")
    void decidirElPasoDeUnObjetivoDeOtroNegocioResponde404YNoSeAnotaNada() throws Exception {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeDecideElPasoDeUnMiniObjetivoInexistente();

        entoncesRespondeNoEncontradoYNoSeAnotaNada();
    }

    @Test
    @DisplayName("Sin sesión responde 401")
    void sinSesionResponde401() throws Exception {
        cuandoSeListanLasDecisionesSinSesion();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Sin token CSRF decidir un paso responde 403")
    void sinTokenCsrfDecidirUnPasoResponde403() throws Exception {
        cuandoSeDecideUnPasoSinCsrf();

        entoncesSeProhibe();
    }

    // --- dado ---

    private void dadoUnObjetivoActivo() {
        objetivoId = crearObjetivo(negocioId, 500000);
    }

    private void dadoUnObjetivoConUnMiniObjetivoEnCurso() {
        dadoUnObjetivoActivo();
        miniId = crearMini(objetivoId);
    }

    private void dadoDosObjetivosActivosYUnoAlcanzado() {
        objetivoId = crearObjetivo(negocioId, 500000);
        otroObjetivoId = crearObjetivo(negocioId, 600000);
        objetivoAlcanzadoId = crearObjetivo(negocioId, 100);
        jdbcTemplate.update("UPDATE objetivo SET alcanzado_en = now() WHERE id = ?", objetivoAlcanzadoId);
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        UUID otroNegocio = crearNegocio(crearUsuario());
        objetivoId = crearObjetivo(otroNegocio, 400000);
    }

    private void dadoUnAvisoEstancado() {
        avisoId = crearAvisoEstancado();
    }

    private void dadoUnAvisoEstancadoAplicado() throws Exception {
        dadoUnAvisoEstancado();
        resolverElAviso("aplicar");
    }

    private void dadoUnAvisoEstancadoDescartado() throws Exception {
        dadoUnAvisoEstancado();
        resolverElAviso("descartar");
    }

    /** Lo que deja MedirImpactoAvisos cuando termina la ventana: el ingreso por día pasa de $100 a $150. */
    private void dadoUnImpactoMedidoDelAviso() {
        AvisoEntity aviso = avisos.findById(avisoId).orElseThrow();
        Map<String, Object> impacto = new LinkedHashMap<>();
        impacto.put("ventanaDias", 14);
        impacto.put("unidadesPorDiaAntes", 2);
        impacto.put("unidadesPorDiaDespues", 3);
        impacto.put("ingresoPorDiaAntes", 100);
        impacto.put("ingresoPorDiaDespues", 150);
        Map<String, Object> datos = new LinkedHashMap<>(aviso.getDatos());
        datos.put("impacto", impacto);
        aviso.setDatos(datos);
        avisos.saveAndFlush(aviso);
    }

    private void dadoQueElMiniObjetivoFueAbandonado() {
        MiniObjetivoEntity abandonado = minis.findById(miniId).orElseThrow();
        abandonado.setEstado(EstadoMiniObjetivo.ABANDONADO);
        minis.saveAndFlush(abandonado);
    }

    // --- cuando ---

    private void cuandoSeListanLasDecisionesDelObjetivo() throws Exception {
        respuesta = mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion.sesion()));
    }

    private void cuandoSeListanLasDecisionesSinSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/objetivos/{id}/decisiones", UUID.randomUUID()));
    }

    private void cuandoSeAplicaElAviso() throws Exception {
        respuesta = resolverElAviso("aplicar");
    }

    private void cuandoSeDecideElPaso(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeDecideElPasoDeUnMiniObjetivoInexistente() throws Exception {
        respuesta = mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId,
                UUID.randomUUID())
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content(ACEPTADO));
    }

    private void cuandoSeDecideUnPasoSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", UUID.randomUUID(),
                UUID.randomUUID())
                .session(sesion.sesion()).contentType("application/json").content(ACEPTADO));
    }

    // --- entonces ---

    private void entoncesLaDecisionQuedaEnLaSemanaDeHoyVinculadaAlMiniObjetivo() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas.length()").value(1))
                .andExpect(jsonPath("$.semanas[0].semanaInicio").value(semanaDeHoy))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_APLICADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].avisoId").value(avisoId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].miniObjetivoId").value(miniId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].registroId").value(productoId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].descripcion").value("Bajá el precio de la Yerba"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MIDIENDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").doesNotExist());
    }

    private void entoncesLaDecisionTraeSuEfectoMedido() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MEDIDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").value(350.0));
    }

    private void entoncesLaDecisionQuedaDescartadaSinMedicion() throws Exception {
        respuesta.andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_DESCARTADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    private void entoncesSeAnotaUnaDecisionPorObjetivoActivoYNingunaEnElAlcanzado() {
        assertThat(decisionesDe(objetivoId)).isEqualTo(1);
        assertThat(decisionesDe(otroObjetivoId)).isEqualTo(1);
        assertThat(decisionesDe(objetivoAlcanzadoId)).isZero();
    }

    private void entoncesElAvisoSeAplicaYNoSeAnotaNingunaDecision() throws Exception {
        respuesta.andExpect(status().isOk());
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE negocio_id = ?", Integer.class, negocioId)).isZero();
    }

    private void entoncesElPasoAceptadoQuedaEnElHistorial() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.miniObjetivoId").value(miniId.toString()));
        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion.sesion()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    private void entoncesElPasoQuedaAnotadoComoIgnorado() throws Exception {
        respuesta.andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("PASO_IGNORADO"));
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesRespondeNoEncontradoYNoSeAnotaNada() throws Exception {
        respuesta.andExpect(status().isNotFound());
        assertThat(decisionesDe(objetivoId)).isZero();
    }

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    // --- datos ---

    private ResultActions resolverElAviso(String accion) throws Exception {
        return mockMvc.perform(post("/api/avisos/{id}/" + accion, avisoId)
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    private int decisionesDe(UUID objetivo) {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE objetivo_id = ?", Integer.class, objetivo);
    }

    private UUID crearUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearNegocio(UUID usuarioId) {
        UUID creado = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creado), AHORA.minusDays(1));
        return creado;
    }

    private UUID crearObjetivo(UUID negocio, int meta) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Ganar más', ?)",
                id, negocio, meta);
        return id;
    }

    /** Un mini-objetivo en curso del producto del aviso. */
    private UUID crearMini(UUID objetivo) {
        return minis.saveAndFlush(MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivo)
                .negocioId(negocioId)
                .posicion((short) 1)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .registroId(productoId)
                .referencia("Yerba 1 kg")
                .meta(new BigDecimal("14183.00"))
                .valorInicial(new BigDecimal("12600.00"))
                .valorActual(new BigDecimal("12600.00"))
                .aporteEstimado(new BigDecimal("6786.00"))
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(LocalDate.parse(semanaDeHoy))
                .titulo("Subile el precio a Yerba 1 kg")
                .justificacion("Te deja más por unidad")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    /** Un aviso pendiente de «producto estancado» sobre el producto del mini-objetivo, sin acción que ejecutar. */
    private UUID crearAvisoEstancado() {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.ESTANCADO).grupo(GrupoAviso.VENTAS).prioridad((short) 2)
                .entidad("registro:" + productoId).huella("ESTANCADO:" + productoId + ":" + UUID.randomUUID())
                .titulo("Bajá el precio de la Yerba").detalle("No se vende hace semanas")
                .acciones(List.of()).estado(EstadoAviso.PENDIENTE)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }
}
