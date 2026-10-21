package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesRequest;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse.DireccionItem;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse.RubroItem;
import com.ensambla.backend.domain.dto.ubicacion.ResolverLocalidadRequest;
import com.ensambla.backend.domain.dto.ubicacion.ResolverLocalidadResponse;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.useCase.ubicacion.BuscarDireccionesUseCase;
import com.ensambla.backend.domain.useCase.ubicacion.ListarRubrosUseCase;
import com.ensambla.backend.domain.useCase.ubicacion.ResolverLocalidadUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.ubicacion.UbicacionPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UbicacionControllerTest {

    private static final String CONSULTA = "Varela 1903, San Justo";
    private static final BuscarDireccionesRequest REQUEST = new BuscarDireccionesRequest(CONSULTA);
    private static final ResolverLocalidadRequest PUNTO = new ResolverLocalidadRequest(
            new BigDecimal("-34.6701"), new BigDecimal("-58.5625"));
    private BuscarDireccionesUseCase buscarDireccionesUseCase;
    private ResolverLocalidadUseCase resolverLocalidadUseCase;
    private ListarRubrosUseCase listarRubrosUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        buscarDireccionesUseCase = mock(BuscarDireccionesUseCase.class);
        resolverLocalidadUseCase = mock(ResolverLocalidadUseCase.class);
        listarRubrosUseCase = mock(ListarRubrosUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UbicacionController(
                        buscarDireccionesUseCase, resolverLocalidadUseCase, listarRubrosUseCase,
                        new UbicacionPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("La búsqueda devuelve opciones completas y prohíbe la caché del navegador")
    void buscarShouldReturnDirectionsWithoutBrowserCache() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of(
                new DireccionItem(CONSULTA, new BigDecimal("-34.67"), new BigDecimal("-58.56"), "San Justo"))));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.resultados[0].direccion").value(CONSULTA))
                .andExpect(jsonPath("$.resultados[0].latitud").value(-34.67))
                .andExpect(jsonPath("$.resultados[0].longitud").value(-58.56))
                .andExpect(jsonPath("$.resultados[0].localidad").value("San Justo"));

        verify(buscarDireccionesUseCase).execute(REQUEST);
    }

    @Test
    @DisplayName("La localidad del punto se devuelve sin caché del navegador")
    void resolverLocalidadShouldReturnLocalitiesWithoutBrowserCache() throws Exception {
        when(resolverLocalidadUseCase.execute(PUNTO)).thenReturn(
                new ResolverLocalidadResponse("San Justo", List.of("San Justo", "Villa Luzuriaga")));

        mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.localidad").value("San Justo"))
                .andExpect(jsonPath("$.localidades[1]").value("Villa Luzuriaga"));
    }

    @Test
    @DisplayName("Un punto sin localidad reconocida responde localidad nula y lista vacía")
    void resolverLocalidadShouldReturnEmptyLocalities() throws Exception {
        when(resolverLocalidadUseCase.execute(PUNTO)).thenReturn(new ResolverLocalidadResponse(null, List.of()));

        mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localidad").isEmpty())
                .andExpect(jsonPath("$.localidades").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"latitud\":-34.6}", "{\"longitud\":-58.5}",
            "{\"latitud\":91,\"longitud\":0}", "{\"latitud\":-91,\"longitud\":0}",
            "{\"latitud\":0,\"longitud\":181}", "{\"latitud\":0,\"longitud\":-181}"
    })
    @DisplayName("Un punto inválido no llega al caso de uso")
    void resolverLocalidadShouldRejectInvalidPoint(String body) throws Exception {
        mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resolverLocalidadUseCase);
    }

    @Test
    @DisplayName("El proveedor caído al resolver la localidad responde 503")
    void resolverLocalidadShouldReturn503WhenProviderUnavailable() throws Exception {
        when(resolverLocalidadUseCase.execute(PUNTO)).thenThrow(new ServicioUbicacionException("Sin servicio"));

        mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Sin servicio"));
    }

    @Test
    @DisplayName("La búsqueda sin coincidencias responde con una lista vacía")
    void buscarShouldReturnEmptyResults() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of()));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultados").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"consulta\":null}", "{\"consulta\":\"    \"}", "{\"consulta\":\"abcd\"}"})
    @DisplayName("Una dirección inválida no llega al caso de uso")
    void buscarShouldRejectInvalidBody(String body) throws Exception {
        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.consulta").exists());

        verifyNoInteractions(buscarDireccionesUseCase);
    }

    @Test
    @DisplayName("El proveedor caído responde 503 con el formato de error de la aplicación")
    void buscarShouldReturn503WhenProviderUnavailable() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenThrow(new ServicioUbicacionException("Sin servicio"));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Sin servicio"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("El catálogo devuelve código estable y nombre legible")
    void listarRubrosShouldReturnCatalog() throws Exception {
        when(listarRubrosUseCase.execute()).thenReturn(new ListarRubrosResponse(List.of(
                new RubroItem("ALMACEN", "Almacén"), new RubroItem("OTRO", "Otro"))));

        mockMvc.perform(get("/api/ubicacion/rubros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rubros[0].codigo").value("ALMACEN"))
                .andExpect(jsonPath("$.rubros[0].nombre").value("Almacén"))
                .andExpect(jsonPath("$.rubros[1].codigo").value("OTRO"));
    }

    @Test
    @DisplayName("El listado respeta un catálogo vacío")
    void listarRubrosShouldReturnEmptyCatalog() throws Exception {
        when(listarRubrosUseCase.execute()).thenReturn(new ListarRubrosResponse(List.of()));

        mockMvc.perform(get("/api/ubicacion/rubros"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rubros").isEmpty());
    }
}
