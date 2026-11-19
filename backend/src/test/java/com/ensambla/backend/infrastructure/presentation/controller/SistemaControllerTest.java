package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.sistema.SistemaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SistemaControllerTest {

    private ResultActions resultado;

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final String SISTEMA_ID = "d0000000-0000-4000-8000-000000000001";

    private final ListarSistemas listarSistemas = mock(ListarSistemas.class);
    private final GuardarSistema guardarSistema = mock(GuardarSistema.class);
    private final EliminarSistema eliminarSistema = mock(EliminarSistema.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new SistemaController(listarSistemas, guardarSistema,
                eliminarSistema, new SistemaPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los sistemas con la forma que valida el front y omite lo que falta")
    void gETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta() throws Exception {
        dadoElEscenarioDeGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta();

        cuandoSeEjecutaGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta();

        entoncesSeVerificaGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta();
    }

    @Test
    @DisplayName("GET manda a qué hora se leyeron los sistemas para que el cliente la devuelva al guardar")
    void gETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAlGuardar() throws Exception {
        dadoElEscenarioDeGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl();

        cuandoSeEjecutaGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl();

        entoncesSeVerificaGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl();
    }

    @Test
    @DisplayName("PUT con X-Leido-En pasa la hora de lectura para no borrar lo que Sami cargó después")
    void pUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues() throws Exception {
        dadoElEscenarioDePUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues();

        cuandoSeEjecutaPUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues();

        entoncesSeVerificaPUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues();
    }

    @Test
    @DisplayName("PUT sin X-Leido-En reemplaza el sistema exacto, como antes")
    void pUTSinXLeidoEnReemplazaElSistemaExactoComoAntes() throws Exception {
        dadoElEscenarioDePUTSinXLeidoEnReemplazaElSistemaExactoComoAntes();

        cuandoSeEjecutaPUTSinXLeidoEnReemplazaElSistemaExactoComoAntes();

        entoncesSeVerificaPUTSinXLeidoEnReemplazaElSistemaExactoComoAntes();
    }

    @Test
    @DisplayName("PUT con una hora de lectura mal escrita responde 400")
    void pUTConUnaHoraDeLecturaMalEscritaResponde400() throws Exception {
        cuandoSeEjecutaPUTConUnaHoraDeLecturaMalEscritaResponde400();

        entoncesSeVerificaPUTConUnaHoraDeLecturaMalEscritaResponde400();
    }

    @Test
    @DisplayName("PUT guarda el sistema entero con el id de la URL y el usuario de la sesión")
    void pUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDePUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion();

        cuandoSeEjecutaPUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion();

        entoncesSeVerificaPUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("PUT acepta un sistema sin opciones ni demo")
    void pUTAceptaUnSistemaSinOpcionesNiDemo() throws Exception {
        dadoElEscenarioDePUTAceptaUnSistemaSinOpcionesNiDemo();

        cuandoSeEjecutaPUTAceptaUnSistemaSinOpcionesNiDemo();

        entoncesSeVerificaPUTAceptaUnSistemaSinOpcionesNiDemo();
    }

    @Test
    @DisplayName("PUT rechaza un sistema sin secciones sin tocar el caso de uso")
    void pUTRechazaUnSistemaSinSeccionesSinTocarElCasoDeUso() throws Exception {
        cuandoSeEjecutaPUTRechazaUnSistemaSinSeccionesSinTocarElCasoDeUso();

        entoncesSeVerificaPUTRechazaUnSistemaSinSeccionesSinTocarElCasoDeUso();
    }

    @Test
    @DisplayName("PUT de un sistema ajeno responde 404")
    void pUTDeUnSistemaAjenoResponde404() throws Exception {
        dadoElEscenarioDePUTDeUnSistemaAjenoResponde404();

        cuandoSeEjecutaPUTDeUnSistemaAjenoResponde404();

        entoncesSeVerificaPUTDeUnSistemaAjenoResponde404();
    }

    @Test
    @DisplayName("DELETE responde 204 y delega con el id de la URL")
    void dELETEResponde204YDelegaConElIdDeLaURL() throws Exception {
        cuandoSeEjecutaDELETEResponde204YDelegaConElIdDeLaURL();

        entoncesSeVerificaDELETEResponde204YDelegaConElIdDeLaURL();
    }

    // --- dado ---
    private String dadoBody() {
        return """
                {"id":"%s","name":"Mi almacén","createdAt":"2026-01-10T09:00:00Z","updatedAt":"2026-10-21T10:00:00Z",
                 "template":"shop","layout":"mosaic","options":{"movements":true,"expenses":false},"demoAlmacen":true,
                 "sections":[{"id":"s1","name":"Vista general","blocks":[
                   {"id":"b1","type":"inventory","name":"Productos","description":"","color":"orange","width":"half",
                    "fields":[{"key":"name","label":"Producto","type":"text","required":true,"min":0}],
                    "rows":[{"id":"r1","name":"Harina","price":1200}],"content":""}]}]}
                """.formatted(SISTEMA_ID);
    }

    private SistemaItem dadoItem() {
        BloqueItem inventario = new BloqueItem("b1", "inventory", "Productos", "", "orange", "half",
                List.of(new CampoItem("name", "Producto", "text", true, null, null, null),
                        new CampoItem("price", "Precio", "currency", false, null, BigDecimal.ZERO, null)),
                List.of(Map.of("id", "r1", "name", "Harina")), "", false, null);
        return new SistemaItem(SISTEMA_ID, "Mi almacén", "2026-01-10T09:00:00Z", "2026-10-21T10:00:00Z",
                List.of(new SeccionItem("s1", "Vista general", List.of(inventario))), "shop",
                new OpcionesItem(true, false), "mosaic", false);
    }

    // --- dado ---

    private void dadoElEscenarioDeGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta() throws Exception {
        when(listarSistemas.execute(any())).thenReturn(List.of(dadoItem()));
    }

    private void dadoElEscenarioDeGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl() throws Exception {
        when(listarSistemas.execute(any())).thenReturn(List.of(dadoItem()));
    }

    private void dadoElEscenarioDePUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues() throws Exception {
        when(guardarSistema.execute(any())).thenReturn(dadoItem());
    }

    private void dadoElEscenarioDePUTSinXLeidoEnReemplazaElSistemaExactoComoAntes() throws Exception {
        when(guardarSistema.execute(any())).thenReturn(dadoItem());
    }

    private void dadoElEscenarioDePUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion() throws Exception {
        when(guardarSistema.execute(any())).thenReturn(dadoItem());
    }

    private void dadoElEscenarioDePUTAceptaUnSistemaSinOpcionesNiDemo() throws Exception {
        when(guardarSistema.execute(any())).thenReturn(dadoItem());
    }

    private void dadoElEscenarioDePUTDeUnSistemaAjenoResponde404() throws Exception {
        when(guardarSistema.execute(any())).thenThrow(new ResourceNotFoundException("Sistema no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeEjecutaGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta() throws Exception {
        resultado = mockMvc.perform(get("/api/sistemas").principal(principal));
    }

    private void cuandoSeEjecutaGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl() throws Exception {
        resultado = mockMvc.perform(get("/api/sistemas").principal(principal));
    }

    private void cuandoSeEjecutaPUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .header(SistemaController.HEADER_LEIDO_EN, "2026-10-27T15:00:00Z")
                                .contentType("application/json").content(dadoBody()));
    }

    private void cuandoSeEjecutaPUTSinXLeidoEnReemplazaElSistemaExactoComoAntes() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .contentType("application/json").content(dadoBody()));
    }

    private void cuandoSeEjecutaPUTConUnaHoraDeLecturaMalEscritaResponde400() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .header(SistemaController.HEADER_LEIDO_EN, "ayer")
                                .contentType("application/json").content(dadoBody()));
    }

    private void cuandoSeEjecutaPUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", "e" + SISTEMA_ID).principal(principal)
                                .contentType("application/json").content(dadoBody()));
    }

    private void cuandoSeEjecutaPUTAceptaUnSistemaSinOpcionesNiDemo() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .contentType("application/json")
                                .content("{\"id\":\"" + SISTEMA_ID + "\",\"name\":\"Solo\",\"sections\":"
                                        + "[{\"id\":\"s1\",\"name\":\"Vista\",\"blocks\":[]}]}"));
    }

    private void cuandoSeEjecutaPUTRechazaUnSistemaSinSeccionesSinTocarElCasoDeUso() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .contentType("application/json")
                                .content("{\"id\":\"" + SISTEMA_ID + "\",\"name\":\"Solo\",\"sections\":[]}"));
    }

    private void cuandoSeEjecutaPUTDeUnSistemaAjenoResponde404() throws Exception {
        resultado = mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                                .contentType("application/json").content(dadoBody()));
    }

    private void cuandoSeEjecutaDELETEResponde204YDelegaConElIdDeLaURL() throws Exception {
        resultado = mockMvc.perform(delete("/api/sistemas/{id}", "e" + SISTEMA_ID).principal(principal));
    }

    // --- entonces ---

    private void entoncesSeVerificaGETListaLosSistemasConLaFormaQueValidaElFrontYOmiteLoQueFalta() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").value(SISTEMA_ID))
                        .andExpect(jsonPath("$[0].name").value("Mi almacén"))
                        .andExpect(jsonPath("$[0].createdAt").value("2026-01-10T09:00:00Z"))
                        .andExpect(jsonPath("$[0].template").value("shop"))
                        .andExpect(jsonPath("$[0].layout").value("mosaic"))
                        .andExpect(jsonPath("$[0].options.movements").value(true))
                        .andExpect(jsonPath("$[0].demoAlmacen").value(false))
                        .andExpect(jsonPath("$[0].sections[0].name").value("Vista general"))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].type").value("inventory"))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].stockDirection").doesNotExist())
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].key").value("name"))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].required").value(true))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].options").doesNotExist())
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[1].min").value(0))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].name").value("Harina"))
                        .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].price").doesNotExist());
        verify(listarSistemas).execute(new ListarSistemasRequest(USUARIO_ID));
    }

    private void entoncesSeVerificaGETMandaAQueHoraSeLeyeronLosSistemasParaQueElClienteLaDevuelvaAl() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(header().string(SistemaController.HEADER_LEIDO_EN, org.hamcrest.Matchers.matchesPattern(
                                "20[0-9]{2}-[0-9]{2}-[0-9]{2}T[0-9:.]+Z")));
    }

    private void entoncesSeVerificaPUTConXLeidoEnPasaLaHoraDeLecturaParaNoBorrarLoQueSamiCargoDespues() throws Exception {
        resultado.andExpect(status().isOk());
        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistema).execute(captor.capture());
        assertThat(captor.getValue().leidoEn()).isEqualTo(java.time.OffsetDateTime.parse("2026-10-27T15:00:00Z"));
    }

    private void entoncesSeVerificaPUTSinXLeidoEnReemplazaElSistemaExactoComoAntes() throws Exception {
        resultado.andExpect(status().isOk());
        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistema).execute(captor.capture());
        assertThat(captor.getValue().leidoEn()).isNull();
    }

    private void entoncesSeVerificaPUTConUnaHoraDeLecturaMalEscritaResponde400() throws Exception {
        resultado.andExpect(status().isBadRequest());
        verifyNoInteractions(guardarSistema);
    }

    private void entoncesSeVerificaPUTGuardaElSistemaEnteroConElIdDeLaURLYElUsuarioDeLaSesion() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(SISTEMA_ID));
        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistema).execute(captor.capture());
        GuardarSistemaRequest request = captor.getValue();
        assertThat(request.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.id()).isEqualTo("e" + SISTEMA_ID);
        SistemaItem sistema = request.sistema();
        assertThat(sistema.template()).isEqualTo("shop");
        assertThat(sistema.options()).isEqualTo(new OpcionesItem(true, false));
        assertThat(sistema.demoAlmacen()).isTrue();
        BloqueItem bloque = sistema.sections().getFirst().blocks().getFirst();
        assertThat(bloque.fields()).singleElement().satisfies(campo -> {
                    assertThat(campo.key()).isEqualTo("name");
                    assertThat(campo.label()).isEqualTo("Producto");
                    assertThat(campo.required()).isTrue();
                    assertThat(campo.min()).isEqualByComparingTo("0");
                });
        assertThat(bloque.rows()).singleElement().satisfies(fila ->
                        assertThat(fila).containsEntry("name", "Harina").containsEntry("price", 1200));
    }

    private void entoncesSeVerificaPUTAceptaUnSistemaSinOpcionesNiDemo() throws Exception {
        resultado.andExpect(status().isOk());
        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistema).execute(captor.capture());
        assertThat(captor.getValue().sistema().options()).isNull();
        assertThat(captor.getValue().sistema().demoAlmacen()).isFalse();
    }

    private void entoncesSeVerificaPUTRechazaUnSistemaSinSeccionesSinTocarElCasoDeUso() throws Exception {
        resultado.andExpect(status().isBadRequest());
        verifyNoInteractions(guardarSistema);
    }

    private void entoncesSeVerificaPUTDeUnSistemaAjenoResponde404() throws Exception {
        resultado.andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaDELETEResponde204YDelegaConElIdDeLaURL() throws Exception {
        resultado.andExpect(status().isNoContent());
        verify(eliminarSistema).execute(new EliminarSistemaRequest(USUARIO_ID, "e" + SISTEMA_ID));
    }
}
