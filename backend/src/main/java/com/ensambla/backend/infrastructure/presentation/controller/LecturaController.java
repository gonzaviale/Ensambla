package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.procesamiento.ConfirmarLecturaUseCase;
import com.ensambla.backend.domain.useCase.procesamiento.ConsultarConsumoUseCase;
import com.ensambla.backend.domain.useCase.procesamiento.LeerImagenUseCase;
import com.ensambla.backend.domain.useCase.procesamiento.ObtenerLecturaUseCase;
import com.ensambla.backend.domain.useCase.procesamiento.ReintentarLecturaUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConfirmacionRequest;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConfirmacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConsumoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.PedidoLecturaRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.procesamiento.LecturaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Lecturas de cuadernos")
public class LecturaController {

    private final ObtenerLecturaUseCase obtenerLecturaUseCase;
    private final ReintentarLecturaUseCase reintentarLecturaUseCase;
    private final ConfirmarLecturaUseCase confirmarLecturaUseCase;
    private final ConsultarConsumoUseCase consultarConsumoUseCase;
    private final LeerImagenUseCase leerImagenUseCase;
    private final LecturaPresentationMapper lecturaPresentationMapper;

    @GetMapping("/api/lotes/{loteId}/lectura")
    @Operation(summary = "Consultar el procesamiento y la propuesta del lote del negocio actual")
    public ResponseEntity<LecturaResponse> obtener(Authentication authentication, @PathVariable UUID loteId) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(obtenerLecturaUseCase.execute(
                lecturaPresentationMapper.toRequest(UUID.fromString(authentication.getName()), loteId))));
    }

    @PostMapping("/api/lotes/{loteId}/reintentar")
    @Operation(summary = "Reencolar una lectura fallida del negocio actual")
    public ResponseEntity<Void> reintentar(Authentication authentication, @PathVariable UUID loteId) {
        reintentarLecturaUseCase.execute(
                lecturaPresentationMapper.toRequest(UUID.fromString(authentication.getName()), loteId));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/lotes/{loteId}/confirmar")
    @Operation(summary = "Guardar lo que la persona revisó de una lectura: crea el bloque, sus campos y los registros")
    public ResponseEntity<ConfirmacionResponse> confirmar(Authentication authentication,
                                                          @PathVariable UUID loteId,
                                                          @Valid @RequestBody ConfirmacionRequest body) {
        ConfirmacionResponse response = lecturaPresentationMapper.toResponse(confirmarLecturaUseCase.execute(
                lecturaPresentationMapper.toConfirmarRequest(UUID.fromString(authentication.getName()), loteId, body)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/api/importaciones/lectura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Leer una foto contra un bloque con IA: devuelve registros y columnas nuevas para revisar, "
            + "sin guardar nada")
    public ResponseEntity<LecturaImagenResponse> leerImagen(@RequestPart("imagen") MultipartFile imagen,
                                                            @RequestPart("pedido") @Valid PedidoLecturaRequest pedido) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(
                leerImagenUseCase.execute(lecturaPresentationMapper.toRequest(imagen, pedido)), pedido));
    }

    @GetMapping("/api/ia/consumo")
    @Operation(summary = "Consultar el consumo de IA y las páginas disponibles del día UTC")
    public ResponseEntity<ConsumoResponse> consumo(Authentication authentication) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(
                consultarConsumoUseCase.execute(UUID.fromString(authentication.getName()))));
    }
}
