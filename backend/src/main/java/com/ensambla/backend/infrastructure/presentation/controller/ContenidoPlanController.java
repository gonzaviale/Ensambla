package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidosRequest;
import com.ensambla.backend.infrastructure.plan.ClavesDeContenido;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Planes")
public class ContenidoPlanController {

    private final DesbloquearContenidos desbloquearContenidos;

    @PostMapping("/api/planes/contenidos")
    @Operation(summary = "Consultar y reservar el cupo mensual de contenidos del negocio autenticado")
    public ResponseEntity<AccesoContenidoItem> desbloquear(
            Authentication authentication, @Valid @RequestBody Pedido pedido) {
        Map<String, String> clavesPedidas = ClavesDeContenido.origenesDelPedido(tipoDe(pedido), pedido.claves());
        AccesoContenidoItem acceso = desbloquearClaves(authentication, pedido, clavesPedidas);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(conLasClavesPedidas(acceso, clavesPedidas));
    }

    private AccesoContenidoItem desbloquearClaves(
            Authentication authentication, Pedido pedido, Map<String, String> clavesPedidas) {
        return desbloquearContenidos.execute(new DesbloquearContenidosRequest(
                UsuarioId.de(authentication.getName()), pedido.tipo(), List.copyOf(clavesPedidas.keySet())));
    }

    private TipoContenidoPlan tipoDe(Pedido pedido) {
        return TipoContenidoPlan.valueOf(pedido.tipo());
    }

    private AccesoContenidoItem conLasClavesPedidas(AccesoContenidoItem acceso, Map<String, String> clavesPedidas) {
        List<String> autorizadas = acceso.autorizadas().stream().map(clavesPedidas::get).toList();
        return new AccesoContenidoItem(autorizadas, acceso.usadas(), acceso.limite(), acceso.total(),
                acceso.bloqueadas(), acceso.finPeriodo());
    }

    public record Pedido(@NotBlank @Pattern(regexp = "AVISOS|ZONA") String tipo,
                         @NotNull @Size(max = 500) List<@NotBlank @Size(max = 240) String> claves) {
    }
}
