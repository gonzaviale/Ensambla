package com.ensambla.backend.domain.useCase.inventario.listarMovimientos;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Los últimos {@value #MAXIMO_MOVIMIENTOS} movimientos de los productos de un bloque, los más nuevos primero. */
@RequiredArgsConstructor
public class ListarMovimientos {

    static final int MAXIMO_MOVIMIENTOS = 100;

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroRepository registroRepository;

    private final MovimientoStockRepository movimientoStockRepository;

    public List<MovimientoItem> execute(ListarMovimientosRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        List<MovimientoStock> movimientos = movimientoStockRepository.findRecientes(
                bloque.getId(), negocioId, request.registroId(), MAXIMO_MOVIMIENTOS);
        List<RegistroId> productoIds = movimientos.stream().map(MovimientoStock::getRegistroId).distinct().toList();
        List<Registro> productos = registroRepository.findAllByIdsAndBloque(productoIds, bloque.getId(), negocioId);
        return MovimientoItem.de(movimientos, productos, campos);
    }
}
