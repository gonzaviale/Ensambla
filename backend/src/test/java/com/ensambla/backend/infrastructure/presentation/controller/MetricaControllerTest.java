package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DeDondeSale;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.useCase.metrica.cambiarGrafico.CambiarGrafico;
import com.ensambla.backend.domain.useCase.metrica.cambiarGrafico.CambiarGraficoRequest;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetrica;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetrica;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricas;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricasRequest;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridas;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridasRequest;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.InterpretarPedido;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.InterpretarPedidoRequest;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.PedidoInterpretadoItem;
import com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno.ListarFuentesDelCuaderno;
import com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno.ListarFuentesDelCuadernoRequest;
import com.ensambla.backend.domain.useCase.metrica.moverMetrica.MoverMetrica;
import com.ensambla.backend.domain.useCase.metrica.moverMetrica.MoverMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetrica;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.renombrarMetrica.RenombrarMetrica;
import com.ensambla.backend.domain.useCase.metrica.renombrarMetrica.RenombrarMetricaRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.metrica.MetricaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static com.ensambla.backend.support.PedidosDePrueba.unPedidoDelFindeInterpretadoComo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricaControllerTest {

    private static final String RUTA = "/api/proyectos/{proyectoId}/metricas";

    private static final String LA_RECETA = "\"nombre\":\"Ventas del mes\",\"fuente\":\"VENTAS\",\"calculo\":\"SUMA\","
            + "\"agrupar\":\"PRODUCTO\",\"periodo\":\"MES\",\"soloFiado\":true";

    private static final String LA_RECETA_EN_JSON = "{" + LA_RECETA + "}";

    private static final String LA_RECETA_SIN_FIADO = "{\"nombre\":\"Ventas del mes\",\"fuente\":\"VENTAS\","
            + "\"calculo\":\"SUMA\",\"agrupar\":\"PRODUCTO\",\"periodo\":\"MES\"}";

    private static final String UN_NOMBRE_NUEVO = "{\"nombre\":\"Lo que vendí\"}";

    private static final String UN_NOMBRE_EN_BLANCO = "{\"nombre\":\" \"}";

    private static final String UN_PEDIDO = "{\"pedido\":\"¿Cuánto vendí este mes?\"}";

    private static final String UN_PEDIDO_EN_BLANCO = "{\"pedido\":\" \"}";

    private static final String SIN_NADA_OBLIGATORIO = "{\"soloFiado\":false}";

    private static final String UN_NOMBRE_DEMASIADO_LARGO = "{\"nombre\":\"" + "a".repeat(81)
            + "\",\"fuente\":\"VENTAS\",\"calculo\":\"SUMA\",\"autoria\":\"PROPIA\"}";

    private static final DefinicionMetrica LAS_VENTAS_FIADAS_POR_PRODUCTO = unaReceta("Ventas del mes",
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true);

    private static final ResultadoMetrica SETECIENTOS_CINCUENTA_DEL_PAN = ResultadoMetrica.conValor(
            FormatoMetrica.MONEDA, new BigDecimal("750"), List.of(new Fila("Pan", new BigDecimal("750"))),
            new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 11)));

    private static final ResultadoMetrica SIN_VENTAS_EN_EL_PERIODO = ResultadoMetrica.sinValor(
            FormatoMetrica.NUMERO, "No hay ventas en este período.",
            new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

    private static final MetricaItem LA_GUARDADA = MetricaItem.de(
            metrica(LAS_VENTAS_FIADAS_POR_PRODUCTO, 2), SETECIENTOS_CINCUENTA_DEL_PAN);

    private final ListarMetricas listarMetricas = mock(ListarMetricas.class);
    private final ListarSugeridas listarSugeridas = mock(ListarSugeridas.class);
    private final PrevisualizarMetrica previsualizarMetrica = mock(PrevisualizarMetrica.class);
    private final CrearMetrica crearMetrica = mock(CrearMetrica.class);
    private final EliminarMetrica eliminarMetrica = mock(EliminarMetrica.class);
    private final RenombrarMetrica renombrarMetrica = mock(RenombrarMetrica.class);
    private final MoverMetrica moverMetrica = mock(MoverMetrica.class);
    private final InterpretarPedido interpretarPedido = mock(InterpretarPedido.class);
    private final ListarFuentesDelCuaderno listarFuentesDelCuaderno = mock(ListarFuentesDelCuaderno.class);
    private final CambiarGrafico cambiarGrafico = mock(CambiarGrafico.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(new MetricaController(
                    listarMetricas, listarSugeridas, previsualizarMetrica, crearMetrica, eliminarMetrica,
                    renombrarMetrica, moverMetrica, interpretarPedido, listarFuentesDelCuaderno,
                    cambiarGrafico, new MetricaPresentationMapper()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private ResultActions respuesta;

    @Test
    @DisplayName("GET lista las métricas del cuaderno con su receta, su número y de dónde sale")
    void listaLasMetricasConSuRecetaSuNumeroYDeDondeSale() throws Exception {
        dadoUnPanelConLasVentasFiadasPorProducto();

        cuandoSePide(get(RUTA, PROYECTO_ID));

        entoncesRespondeLasVentasFiadasPorProducto();
    }

    @Test
    @DisplayName("GET delega en el caso de uso con el usuario de la sesión y el cuaderno de la ruta")
    void listarDelegaConElUsuarioDeLaSesionYElCuaderno() throws Exception {
        dadoUnPanelVacio();

        cuandoSePide(get(RUTA, PROYECTO_ID));

        entoncesRespondeUnaListaVacia();
        entoncesSeListoElCuadernoDeLaSesion();
    }

    @Test
    @DisplayName("GET de un cuaderno ajeno devuelve el 404 estándar")
    void listarUnCuadernoAjenoDevuelveNoEncontrado() throws Exception {
        dadoQueListarNoEncuentraElCuaderno();

        cuandoSePide(get(RUTA, PROYECTO_ID));

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("GET con un id de cuaderno que no es UUID devuelve 400 sin llegar al caso de uso")
    void listarConUnIdQueNoEsUuidDevuelveError() throws Exception {
        cuandoSePide(get("/api/proyectos/invalido/metricas"));

        entoncesRespondeUnPedidoInvalido();
        entoncesNoSeListo();
    }

    @Test
    @DisplayName("GET sugeridas devuelve las del rubro con lo que darían hoy")
    void sugeridasDevuelveLasDelRubroConLoQueDarianHoy() throws Exception {
        dadaUnaSugeridaQueNoSePuedeCalcular();

        cuandoSePide(get(RUTA + "/sugeridas", PROYECTO_ID));

        entoncesRespondeLaSugeridaConSuMotivo("No hay ventas en este período.");
        entoncesSeListaronLasSugeridasDelCuadernoDeLaSesion();
    }

    @Test
    @DisplayName("POST previsualizar calcula la receta sin guardarla")
    void previsualizarCalculaLaRecetaSinGuardarla() throws Exception {
        dadaUnaPrevisualizacionDeSetecientosCincuenta();

        cuandoSeEnvia(post(RUTA + "/previsualizar", PROYECTO_ID), LA_RECETA_EN_JSON);

        entoncesRespondeElValor(750.0);
        entoncesSePrevisualizo(true);
    }

    @Test
    @DisplayName("POST previsualizar sin decir si es fiada mira todas las ventas")
    void previsualizarSinDecirSiEsFiadaMiraTodasLasVentas() throws Exception {
        dadaUnaPrevisualizacionDeSetecientosCincuenta();

        cuandoSeEnvia(post(RUTA + "/previsualizar", PROYECTO_ID), LA_RECETA_SIN_FIADO);

        entoncesSePrevisualizo(false);
    }

    @Test
    @DisplayName("POST previsualizar rechaza una receta que no se puede calcular con el 400 estándar")
    void previsualizarRechazaUnaRecetaInvalida() throws Exception {
        dadoQueLaPrevisualizacionRechazaLaReceta();

        cuandoSeEnvia(post(RUTA + "/previsualizar", PROYECTO_ID), LA_RECETA_EN_JSON);

        entoncesRespondeUnPedidoInvalido();
    }

    @Test
    @DisplayName("POST crea la métrica y devuelve 201 con su ubicación")
    void crearDevuelveCreadaConSuUbicacion() throws Exception {
        dadoQueSeCreaLaGuardada();

        cuandoSeEnvia(post(RUTA, PROYECTO_ID), conAutoria("SUGERIDA"));

        entoncesRespondeCreadaEnSuUbicacion();
    }

    @Test
    @DisplayName("POST pasa la receta, la autoría y el usuario de la sesión al caso de uso")
    void crearPasaLaRecetaLaAutoriaYElUsuario() throws Exception {
        dadoQueSeCreaLaGuardada();

        cuandoSeEnvia(post(RUTA, PROYECTO_ID), conAutoria("PROPIA"));

        entoncesSeCreo(AutoriaMetrica.PROPIA);
    }

    @Test
    @DisplayName("POST rechaza un body sin nombre, sin fuente, sin cálculo o sin autoría")
    void crearRechazaUnBodySinLosDatosObligatorios() throws Exception {
        cuandoSeEnvia(post(RUTA, PROYECTO_ID), SIN_NADA_OBLIGATORIO);

        entoncesRespondeErroresEn("nombre", "fuente", "calculo", "autoria");
        entoncesNoSeCreo();
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 80 caracteres")
    void crearRechazaUnNombreDemasiadoLargo() throws Exception {
        cuandoSeEnvia(post(RUTA, PROYECTO_ID), UN_NOMBRE_DEMASIADO_LARGO);

        entoncesRespondeErroresEn("nombre");
        entoncesNoSeCreo();
    }

    @Test
    @DisplayName("DELETE devuelve 204 y delega con el usuario de la sesión, el cuaderno y la métrica de la ruta")
    void eliminarDevuelveSinContenidoYDelega() throws Exception {
        cuandoSePide(delete(RUTA + "/{id}", PROYECTO_ID, METRICA_ID));

        entoncesRespondeSinContenido();
        entoncesSeEliminoLaMetricaDelCuadernoDeLaSesion();
    }

    @Test
    @DisplayName("DELETE de una métrica ajena o inexistente devuelve el 404 estándar")
    void eliminarUnaMetricaAjenaDevuelveNoEncontrada() throws Exception {
        dadoQueEliminarNoEncuentraLaMetrica();

        cuandoSePide(delete(RUTA + "/{id}", PROYECTO_ID, METRICA_ID));

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("DELETE con un id que no es UUID devuelve 400 sin llegar al caso de uso")
    void eliminarConUnIdQueNoEsUuidDevuelveError() throws Exception {
        cuandoSePide(delete(RUTA + "/invalido", PROYECTO_ID));

        entoncesRespondeUnPedidoInvalido();
        entoncesNoSeElimino();
    }

    @Test
    @DisplayName("PATCH con un nombre nuevo devuelve 204 y delega con el usuario, el cuaderno y la métrica")
    void renombrarDevuelveSinContenidoYDelega() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{id}", PROYECTO_ID, METRICA_ID), UN_NOMBRE_NUEVO);

        entoncesRespondeSinContenido();
        entoncesSeRenombroA("Lo que vendí");
    }

    @Test
    @DisplayName("PATCH con el nombre en blanco devuelve 400 sin llegar al caso de uso")
    void renombrarConElNombreEnBlancoDevuelveError() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{id}", PROYECTO_ID, METRICA_ID), UN_NOMBRE_EN_BLANCO);

        entoncesRespondeErroresEn("nombre");
        entoncesNoSeRenombro();
    }

    @Test
    @DisplayName("PATCH de una métrica ajena devuelve el 404 estándar")
    void renombrarUnaMetricaAjenaDevuelveNoEncontrada() throws Exception {
        dadoQueRenombrarNoEncuentraLaMetrica();

        cuandoSeEnvia(patch(RUTA + "/{id}", PROYECTO_ID, METRICA_ID), UN_NOMBRE_NUEVO);

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("PATCH del lugar devuelve 204 y delega con el lugar pedido")
    void moverDevuelveSinContenidoYDelega() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{id}/lugar", PROYECTO_ID, METRICA_ID), elLugar(2));

        entoncesRespondeSinContenido();
        entoncesSeMovioAlLugar(2);
    }

    @Test
    @DisplayName("PATCH del lugar con un lugar negativo devuelve 400 sin llegar al caso de uso")
    void moverAUnLugarNegativoDevuelveError() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{id}/lugar", PROYECTO_ID, METRICA_ID), elLugar(-1));

        entoncesRespondeErroresEn("lugar");
        entoncesNoSeMovio();
    }

    @Test
    @DisplayName("PATCH del lugar fuera del panel devuelve el 400 estándar")
    void moverFueraDelPanelDevuelveError() throws Exception {
        dadoQueMoverRechazaElLugar();

        cuandoSeEnvia(patch(RUTA + "/{id}/lugar", PROYECTO_ID, METRICA_ID), elLugar(9));

        entoncesRespondeUnPedidoInvalido();
    }

    @Test
    @DisplayName("POST interpretar devuelve la métrica que arma el pedido con lo que daría hoy")
    void interpretarDevuelveLaMetricaQueArmaElPedido() throws Exception {
        dadoQueElPedidoSeEntiende();

        cuandoSeEnvia(post(RUTA + "/interpretar", PROYECTO_ID), UN_PEDIDO);

        entoncesRespondeLaPropuesta();
        entoncesSeInterpreto("¿Cuánto vendí este mes?");
    }

    @Test
    @DisplayName("PATCH grafico cambia cómo se dibuja la métrica del cuaderno de la sesión y responde 204")
    void cambiarGraficoDelegaYRespondeSinContenido() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{metricaId}/grafico", PROYECTO_ID, METRICA_ID), "{\"grafico\":\"DONA\"}");

        entoncesRespondeSinContenidoYSeCambioElGraficoA(GraficoMetrica.DONA);
    }

    @Test
    @DisplayName("PATCH grafico sin decir cuál devuelve 400 sin llegar al caso de uso")
    void cambiarGraficoSinDecirCualDevuelveError() throws Exception {
        cuandoSeEnvia(patch(RUTA + "/{metricaId}/grafico", PROYECTO_ID, METRICA_ID), "{}");

        entoncesRespondeErroresEn("grafico");
        entoncesNoSeCambioElGrafico();
    }

    @Test
    @DisplayName("GET fuentes devuelve de qué se puede armar una métrica en el cuaderno de la sesión")
    void fuentesDevuelveDeQueSePuedeArmarUnaMetrica() throws Exception {
        dadoUnCuadernoConVentasYStock();

        cuandoSePide(get(RUTA + "/fuentes", PROYECTO_ID));

        entoncesRespondeLasFuentesVentasYStock();
    }

    @Test
    @DisplayName("POST interpretar con el pedido en blanco devuelve 400 sin llegar al caso de uso")
    void interpretarUnPedidoEnBlancoDevuelveError() throws Exception {
        cuandoSeEnvia(post(RUTA + "/interpretar", PROYECTO_ID), UN_PEDIDO_EN_BLANCO);

        entoncesRespondeErroresEn("pedido");
        entoncesNoSeInterpreto();
    }

    @Test
    @DisplayName("POST interpretar de un pedido que no se entiende devuelve el 400 estándar con el motivo")
    void interpretarUnPedidoQueNoSeEntiendeDevuelveElMotivo() throws Exception {
        dadoQueElPedidoNoSeEntiende("Decime si es de tus ventas, tus gastos o tu stock");

        cuandoSeEnvia(post(RUTA + "/interpretar", PROYECTO_ID), UN_PEDIDO);

        entoncesRespondeUnPedidoInvalido();
    }

    private static String elLugar(int lugar) {
        return "{\"lugar\":" + lugar + "}";
    }

    private static String conAutoria(String autoria) {
        return "{" + LA_RECETA + ",\"autoria\":\"" + autoria + "\"}";
    }

    private void dadoUnPanelConLasVentasFiadasPorProducto() {
        when(listarMetricas.execute(any())).thenReturn(List.of(LA_GUARDADA));
    }

    private void dadoUnPanelVacio() {
        when(listarMetricas.execute(any())).thenReturn(List.of());
    }

    private void dadoQueListarNoEncuentraElCuaderno() {
        when(listarMetricas.execute(any())).thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void dadaUnaSugeridaQueNoSePuedeCalcular() {
        when(listarSugeridas.execute(any())).thenReturn(List.of(new SugeridaItem(
                DefinicionItem.de(LAS_VENTAS_FIADAS_POR_PRODUCTO), ResultadoItem.de(SIN_VENTAS_EN_EL_PERIODO), true)));
    }

    private void dadaUnaPrevisualizacionDeSetecientosCincuenta() {
        when(previsualizarMetrica.execute(any())).thenReturn(ResultadoItem.de(SETECIENTOS_CINCUENTA_DEL_PAN));
    }

    private void dadoQueLaPrevisualizacionRechazaLaReceta() {
        when(previsualizarMetrica.execute(any())).thenThrow(new BusinessRuleException("Los gastos no tienen unidades"));
    }

    private void dadoQueSeCreaLaGuardada() {
        when(crearMetrica.execute(any())).thenReturn(LA_GUARDADA);
    }

    private void dadoQueEliminarNoEncuentraLaMetrica() {
        doThrow(new ResourceNotFoundException("Métrica no encontrada")).when(eliminarMetrica).execute(any());
    }

    private void cuandoSePide(MockHttpServletRequestBuilder pedido)
            throws Exception {
        respuesta = mockMvc.perform(pedido.principal(principal));
    }

    private void cuandoSeEnvia(MockHttpServletRequestBuilder pedido,
                               String cuerpo) throws Exception {
        cuandoSePide(pedido.contentType("application/json").content(cuerpo));
    }

    private void entoncesRespondeLasVentasFiadasPorProducto() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(METRICA_ID.toString()))
                .andExpect(jsonPath("$[0].orden").value(2))
                .andExpect(jsonPath("$[0].autoria").value("SUGERIDA"))
                .andExpect(jsonPath("$[0].definicion.nombre").value("Ventas del mes"))
                .andExpect(jsonPath("$[0].definicion.fuente").value("VENTAS"))
                .andExpect(jsonPath("$[0].definicion.soloFiado").value(true))
                .andExpect(jsonPath("$[0].resultado.formato").value("MONEDA"))
                .andExpect(jsonPath("$[0].resultado.valor").value(750.0))
                .andExpect(jsonPath("$[0].resultado.filas[0].etiqueta").value("Pan"))
                .andExpect(jsonPath("$[0].resultado.deDondeSale.filasUsadas").value(3))
                .andExpect(jsonPath("$[0].resultado.deDondeSale.desde").value("2026-11-05"));
    }

    private void entoncesRespondeUnaListaVacia() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private void entoncesRespondeLaSugeridaConSuMotivo(String motivo) throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].definicion.nombre").value("Ventas del mes"))
                .andExpect(jsonPath("$[0].resultado.valor").doesNotExist())
                .andExpect(jsonPath("$[0].resultado.motivo").value(motivo));
    }

    private void entoncesRespondeElValor(double valor) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(valor));
    }

    private void entoncesRespondeCreadaEnSuUbicacion() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/proyectos/" + PROYECTO_ID + "/metricas/" + METRICA_ID))
                .andExpect(jsonPath("$.id").value(METRICA_ID.toString()));
    }

    private void entoncesRespondeErroresEn(String... campos) throws Exception {
        respuesta.andExpect(status().isBadRequest());
        for (String campo : campos) {
            respuesta.andExpect(jsonPath("$.errors." + campo).exists());
        }
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private void entoncesRespondeUnPedidoInvalido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesRespondeSinContenido() throws Exception {
        respuesta.andExpect(status().isNoContent());
    }

    private void entoncesSeListoElCuadernoDeLaSesion() {
        verify(listarMetricas).execute(new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesNoSeListo() {
        verifyNoInteractions(listarMetricas);
    }

    private void entoncesSeListaronLasSugeridasDelCuadernoDeLaSesion() {
        verify(listarSugeridas).execute(new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesSePrevisualizo(boolean soloFiado) {
        verify(previsualizarMetrica).execute(new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID,
                "Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO,
                PeriodoMetrica.MES, soloFiado));
    }

    private void entoncesSeCreo(AutoriaMetrica autoria) {
        verify(crearMetrica).execute(new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes",
                FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true,
                autoria));
    }

    private void entoncesNoSeCreo() {
        verifyNoInteractions(crearMetrica);
    }

    private void entoncesSeEliminoLaMetricaDelCuadernoDeLaSesion() {
        verify(eliminarMetrica).execute(new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID));
    }

    private void entoncesNoSeElimino() {
        verifyNoInteractions(eliminarMetrica);
    }

    private void dadoQueRenombrarNoEncuentraLaMetrica() {
        doThrow(new ResourceNotFoundException("Métrica no encontrada")).when(renombrarMetrica).execute(any());
    }

    private void dadoQueMoverRechazaElLugar() {
        doThrow(new BusinessRuleException("Ese lugar no está en el panel")).when(moverMetrica).execute(any());
    }

    private void entoncesSeRenombroA(String nombre) {
        verify(renombrarMetrica).execute(new RenombrarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID, nombre));
    }

    private void entoncesNoSeRenombro() {
        verifyNoInteractions(renombrarMetrica);
    }

    private void entoncesSeMovioAlLugar(int lugar) {
        verify(moverMetrica).execute(new MoverMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID, lugar));
    }

    private void entoncesNoSeMovio() {
        verifyNoInteractions(moverMetrica);
    }

    private void dadoQueElPedidoSeEntiende() {
        when(interpretarPedido.execute(any())).thenReturn(PedidoInterpretadoItem.de(
                unPedidoDelFindeInterpretadoComo(LAS_VENTAS_FIADAS_POR_PRODUCTO), SETECIENTOS_CINCUENTA_DEL_PAN, false));
    }

    private void entoncesRespondeSinContenidoYSeCambioElGraficoA(GraficoMetrica grafico) throws Exception {
        respuesta.andExpect(status().isNoContent());
        verify(cambiarGrafico).execute(new CambiarGraficoRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID, grafico));
    }

    private void entoncesNoSeCambioElGrafico() {
        verifyNoInteractions(cambiarGrafico);
    }

    private void dadoUnCuadernoConVentasYStock() {
        when(listarFuentesDelCuaderno.execute(new ListarFuentesDelCuadernoRequest(USUARIO_ID, PROYECTO_ID)))
                .thenReturn(List.of(FuenteMetrica.VENTAS, FuenteMetrica.INVENTARIO));
    }

    private void entoncesRespondeLasFuentesVentasYStock() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("VENTAS"))
                .andExpect(jsonPath("$[1]").value("INVENTARIO"));
    }

    private void dadoQueElPedidoNoSeEntiende(String motivo) {
        when(interpretarPedido.execute(any())).thenThrow(new BusinessRuleException(motivo));
    }

    private void entoncesRespondeLaPropuesta() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.definicion.nombre").value("Ventas del mes"))
                .andExpect(jsonPath("$.resultado.valor").value(750.0))
                .andExpect(jsonPath("$.lectura.delPedido[0]").value("FUENTE"))
                .andExpect(jsonPath("$.lectura.porDefecto[2]").value("PERIODO"))
                .andExpect(jsonPath("$.lectura.noEntendidas[0]").value("finde"))
                .andExpect(jsonPath("$.yaLaTenes").value(false));
    }

    private void entoncesSeInterpreto(String pedido) {
        verify(interpretarPedido).execute(new InterpretarPedidoRequest(USUARIO_ID, PROYECTO_ID, pedido));
    }

    private void entoncesNoSeInterpreto() {
        verifyNoInteractions(interpretarPedido);
    }
}
