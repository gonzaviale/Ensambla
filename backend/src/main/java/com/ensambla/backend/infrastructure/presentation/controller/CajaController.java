package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CerrarCajaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CierreCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.ConsultarCajaWebResponse;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CajaPresentationMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCaja;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobro;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobroCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.ListarVentasCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.RegistrarCobroWebRequest;

@RestController
@RequestMapping("/api/caja")
@Tag(name = "Caja", description = "Ventas y cobros del negocio de la sesión")
@RequiredArgsConstructor
@Validated
public class CajaController {

    private final CobrarVenta cobrarVenta;
    private final CajaPresentationMapper cajaPresentationMapper;
    private final ConsultarCaja consultarCaja;
    private final CerrarCaja cerrarCaja;
    private final ObtenerCierre obtenerCierre;
    private final ListarVentasCaja listarVentasCaja;
    private final RegistrarCobro registrarCobro;

    @GetMapping("/ventas")
    @Operation(summary = "Consultar ventas y saldos actuales para las cuentas de clientes")
    public ResponseEntity<ListarVentasCajaWebResponse> listar(Authentication authentication,
            @RequestParam ProyectoId proyectoId, @RequestParam BloqueId inventarioId) {
        return ResponseEntity.ok(cajaPresentationMapper.toListadoResponse(listarVentasCaja.execute(
                cajaPresentationMapper.toListado(UsuarioId.de(authentication.getName()), proyectoId, inventarioId))));
    }

    @PostMapping("/cobros")
    @Operation(summary = "Registrar un pago de deudas del mismo cliente una sola vez")
    public ResponseEntity<CobroCajaWebResponse> registrarCobro(Authentication authentication,
            @Valid @RequestBody RegistrarCobroWebRequest request) {
        return ResponseEntity.ok(cajaPresentationMapper.toCobroResponse(registrarCobro.execute(
                cajaPresentationMapper.toCobro(UsuarioId.de(authentication.getName()), request))));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Consultar ventas del día, período abierto y cierres guardados")
    public ResponseEntity<ConsultarCajaWebResponse> resumen(Authentication authentication,
            @RequestParam ProyectoId proyectoId, @RequestParam BloqueId inventarioId,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina) {
        return ResponseEntity.ok(cajaPresentationMapper.toConsultaResponse(consultarCaja.execute(
                cajaPresentationMapper.toConsulta(UsuarioId.de(authentication.getName()),
                        proyectoId, inventarioId, fecha, pagina))));
    }

    @PostMapping("/cierres")
    @Operation(summary = "Guardar el cierre de un período una sola vez")
    public ResponseEntity<CierreCajaWebResponse> cerrar(Authentication authentication,
            @Valid @RequestBody CerrarCajaWebRequest request) {
        return ResponseEntity.ok(cajaPresentationMapper.toCierreResponse(cerrarCaja.execute(
                cajaPresentationMapper.toCierre(UsuarioId.de(authentication.getName()), request))));
    }

    @GetMapping("/cierres/{id}")
    @Operation(summary = "Ver el detalle histórico e inmutable de un cierre del negocio")
    public ResponseEntity<CierreCajaWebResponse> obtener(Authentication authentication,
            @PathVariable CierreCajaId id) {
        return ResponseEntity.ok(cajaPresentationMapper.toCierreResponse(obtenerCierre.execute(
                cajaPresentationMapper.toObtenerCierre(UsuarioId.de(authentication.getName()), id))));
    }

    @PostMapping("/ventas")
    @Operation(summary = "Confirmar una compra una sola vez, con su pago y descuento de existencias")
    public ResponseEntity<CobrarVentaWebResponse> cobrar(Authentication authentication,
            @Valid @RequestBody CobrarVentaWebRequest request) {
        return ResponseEntity.ok(cajaPresentationMapper.toResponse(cobrarVenta.execute(
                cajaPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), request))));
    }
}
