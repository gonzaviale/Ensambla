package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.dto.inventario.ListarMovimientosRequest;
import com.ensambla.backend.domain.dto.inventario.MovimientoItem;
import com.ensambla.backend.domain.mapper.inventario.ListarMovimientosMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarMovimientosUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    private final InventarioService inventarioService;

    private final ListarMovimientosMapper listarMovimientosMapper;

    public List<MovimientoItem> execute(ListarMovimientosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        List<MovimientoStock> movimientos = inventarioService.listarRecientes(
                bloque.getId(), negocioId, request.registroId());
        List<UUID> productoIds = movimientos.stream().map(MovimientoStock::getRegistroId).distinct().toList();
        List<Registro> productos = registroDatosService.obtenerVarios(productoIds, bloque.getId(), negocioId);
        return listarMovimientosMapper.toResponse(movimientos, productos, campos);
    }
}
