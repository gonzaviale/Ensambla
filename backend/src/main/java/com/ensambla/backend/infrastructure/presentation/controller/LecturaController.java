package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLectura;
import com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo.ConsultarConsumo;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagen;
import com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura.ObtenerLectura;
import com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura.ReintentarLectura;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Lecturas de cuadernos")
public class LecturaController {

    private final ObtenerLectura obtenerLectura;
    private final ReintentarLectura reintentarLectura;
    private final ConfirmarLectura confirmarLectura;
    private final ConsultarConsumo consultarConsumo;
    private final LeerImagen leerImagen;
    private final LecturaPresentationMapper lecturaPresentationMapper;

    @GetMapping("/api/lotes/{loteId}/lectura")
    @Operation(summary = "Consultar el procesamiento y la propuesta del lote del negocio actual")
    public ResponseEntity<LecturaResponse> obtener(Authentication authentication, @PathVariable LoteId loteId) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(obtenerLectura.execute(
                lecturaPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), loteId))));
    }

    @PostMapping("/api/lotes/{loteId}/reintentar")
    @Operation(summary = "Reencolar una lectura fallida del negocio actual")
    public ResponseEntity<Void> reintentar(Authentication authentication, @PathVariable LoteId loteId) {
        reintentarLectura.execute(
                lecturaPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), loteId));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/lotes/{loteId}/confirmar")
    @Operation(summary = "Guardar lo que la persona revisó de una lectura: crea el bloque, sus campos y los registros")
    public ResponseEntity<ConfirmacionResponse> confirmar(Authentication authentication,
                                                          @PathVariable LoteId loteId,
                                                          @Valid @RequestBody ConfirmacionRequest body) {
        ConfirmacionResponse response = lecturaPresentationMapper.toResponse(confirmarLectura.execute(
                lecturaPresentationMapper.toConfirmarRequest(UsuarioId.de(authentication.getName()), loteId, body)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/api/importaciones/lectura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Leer una foto con IA y registrar su consumo, sin crear las filas propuestas")
    public ResponseEntity<LecturaImagenResponse> leerImagen(Authentication authentication,
                                                            @RequestPart("imagen") MultipartFile imagen,
                                                            @RequestPart("pedido") @Valid PedidoLecturaRequest pedido) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(leerImagen.execute(
                lecturaPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), imagen, pedido)), pedido));
    }

    @GetMapping("/api/ia/consumo")
    @Operation(summary = "Consultar el consumo de IA y las páginas disponibles del día UTC")
    public ResponseEntity<ConsumoResponse> consumo(Authentication authentication) {
        return ResponseEntity.ok(lecturaPresentationMapper.toResponse(
                consultarConsumo.execute(UsuarioId.de(authentication.getName()))));
    }
}
