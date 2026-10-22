package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.dto.plan.DesbloquearContenidosRequest;
import com.ensambla.backend.domain.useCase.plan.DesbloquearContenidosUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Planes")
public class ContenidoPlanController {

    private final DesbloquearContenidosUseCase desbloquearContenidosUseCase;

    @PostMapping("/api/planes/contenidos")
    @Operation(summary = "Consultar y reservar el cupo mensual de contenidos del negocio autenticado")
    public ResponseEntity<AccesoContenidoItem> desbloquear(
            Authentication authentication, @Valid @RequestBody Pedido pedido) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(desbloquearContenidosUseCase.execute(new DesbloquearContenidosRequest(
                        UUID.fromString(authentication.getName()), pedido.tipo(), pedido.claves())));
    }

    public record Pedido(@NotBlank @Pattern(regexp = "AVISOS|ZONA") String tipo,
                         @NotNull @Size(max = 500) List<@NotBlank @Size(max = 240) String> claves) {
    }
}
