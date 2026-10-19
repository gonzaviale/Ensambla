package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.sami.ConversarConSamiUseCase;
import com.ensambla.backend.domain.useCase.sami.CrearProyectoDesdeSamiUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.CrearProyectoSamiRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.proyecto.ProyectoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.sami.SamiPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/sami")
@Tag(name = "Sami")
@RequiredArgsConstructor
public class SamiController {

    private final ConversarConSamiUseCase conversarConSamiUseCase;
    private final CrearProyectoDesdeSamiUseCase crearProyectoDesdeSamiUseCase;
    private final SamiPresentationMapper samiPresentationMapper;
    private final ProyectoPresentationMapper proyectoPresentationMapper;

    @PostMapping("/conversacion")
    @Operation(summary = "Describir el negocio y recibir la siguiente pregunta de Sami o la estructura propuesta")
    public ResponseEntity<ConversarSamiResponse> conversar(Authentication authentication,
                                                           @Valid @RequestBody ConversarSamiRequest body) {
        ConversarSamiResponse response = samiPresentationMapper.toResponse(conversarConSamiUseCase.execute(
                samiPresentationMapper.toRequest(UUID.fromString(authentication.getName()), body)));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/proyectos")
    @Operation(summary = "Crear el proyecto con la plantilla y las columnas que la persona confirmó de la propuesta")
    public ResponseEntity<ProyectoResponse> crearProyecto(Authentication authentication,
                                                          @Valid @RequestBody CrearProyectoSamiRequest body) {
        ProyectoResponse response = proyectoPresentationMapper.toResponse(crearProyectoDesdeSamiUseCase.execute(
                samiPresentationMapper.toCrearProyectoRequest(UUID.fromString(authentication.getName()), body)));
        return ResponseEntity.created(URI.create("/api/proyectos/" + response.id())).body(response);
    }
}
