package com.ensambla.backend.support;

import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public final class SesionHttpDePrueba {

    private final MockHttpSession sesion;
    private final String tokenCsrf;

    private SesionHttpDePrueba(MockHttpSession sesion, String tokenCsrf) {
        this.sesion = sesion;
        this.tokenCsrf = tokenCsrf;
    }

    public static SesionHttpDePrueba anonima(MockMvc mockMvc) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        guardarTokenCsrf(sesion);
        return new SesionHttpDePrueba(sesion, pedirTokenCsrf(mockMvc, sesion));
    }

    public static SesionHttpDePrueba autenticada(MockMvc mockMvc, String usuarioId) throws Exception {
        SesionHttpDePrueba sesionHttp = anonima(mockMvc);
        sesionHttp.iniciarComo(usuarioId);
        return sesionHttp;
    }

    public void iniciarComo(String usuarioId) {
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contextoDe(usuarioId));
    }

    public MockHttpSession sesion() {
        return sesion;
    }

    public String tokenCsrf() {
        return tokenCsrf;
    }

    private static void guardarTokenCsrf(MockHttpSession sesion) {
        MockHttpServletRequest pedido = new MockHttpServletRequest();
        pedido.setSession(sesion);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), pedido, new MockHttpServletResponse());
    }

    private static String pedirTokenCsrf(MockMvc mockMvc, MockHttpSession sesion) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/auth/csrf").session(sesion))
                .andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readValue(cuerpo, CsrfResponse.class).token();
    }

    private static SecurityContext contextoDe(String usuarioId) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuarioId, null, List.of()));
        return contexto;
    }
}
