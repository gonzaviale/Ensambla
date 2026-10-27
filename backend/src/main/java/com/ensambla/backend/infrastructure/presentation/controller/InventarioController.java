package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStock;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientos;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimiento;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoRegistradoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.PaginaStockResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.inventario.InventarioPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloques/{bloqueId}")
@Tag(name = "Inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final RegistrarMovimiento registrarMovimiento;

    private final ListarMovimientos listarMovimientos;

    private final ConsultarStock consultarStock;

    private final InventarioPresentationMapper presentationMapper;

    @PostMapping("/movimientos")
    @Operation(summary = "Registrar una entrada o una salida de un producto del bloque; el stock es la suma")
    public ResponseEntity<MovimientoRegistradoResponse> registrar(Authentication authentication,
                                                                  @PathVariable BloqueId bloqueId,
                                                                  @RequestBody MovimientoRequest request) {
        MovimientoRegistradoResponse response = presentationMapper.toResponse(registrarMovimiento.execute(
                presentationMapper.toRegistrarRequest(usuarioDe(authentication), bloqueId, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Últimos movimientos (hasta 100) de los productos del bloque, o de uno solo")
    public ResponseEntity<List<MovimientoResponse>> movimientos(Authentication authentication,
                                                                @PathVariable BloqueId bloqueId,
                                                                @RequestParam(required = false) RegistroId registroId) {
        List<MovimientoResponse> response = presentationMapper.toResponse(listarMovimientos.execute(
                presentationMapper.toListarMovimientosRequest(usuarioDe(authentication), bloqueId, registroId)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock")
    @Operation(summary = "Stock actual de los productos del bloque, con búsqueda de texto y paginación")
    public ResponseEntity<PaginaStockResponse> stock(Authentication authentication,
                                                     @PathVariable BloqueId bloqueId,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Integer pagina,
                                                     @RequestParam(required = false) Integer tamanio) {
        PaginaStockResponse response = presentationMapper.toResponse(consultarStock.execute(
                presentationMapper.toStockRequest(usuarioDe(authentication), bloqueId, q, pagina, tamanio)));
        return ResponseEntity.ok(response);
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
