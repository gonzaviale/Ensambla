package com.ensambla.backend.domain.useCase.inventario.listarMovimientos;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Los últimos {@value #MAXIMO_MOVIMIENTOS} movimientos de los productos de un bloque, los más nuevos primero. */
@RequiredArgsConstructor
public class ListarMovimientos {

    static final int MAXIMO_MOVIMIENTOS = 100;

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final MovimientoStockRepository movimientoStockRepository;

    public List<MovimientoItem> execute(ListarMovimientosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        List<Campo> campos = obtenerCampos(bloque, negocioId);
        List<MovimientoStock> movimientos = obtenerMovimientosRecientes(request, bloque, negocioId);
        List<Registro> productos = obtenerProductosDe(movimientos, bloque, negocioId);
        return MovimientoItem.de(movimientos, productos, campos);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private List<Campo> obtenerCampos(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private List<MovimientoStock> obtenerMovimientosRecientes(ListarMovimientosRequest request, Bloque bloque,
                                                              NegocioId negocioId) {
        return movimientoStockRepository.findRecientes(
                bloque.getId(), negocioId, request.registroId(), MAXIMO_MOVIMIENTOS);
    }

    private List<Registro> obtenerProductosDe(List<MovimientoStock> movimientos, Bloque bloque, NegocioId negocioId) {
        List<RegistroId> productoIds = movimientos.stream().map(MovimientoStock::getRegistroId).distinct().toList();
        return registroRepository.findAllByIdsAndBloque(productoIds, bloque.getId(), negocioId);
    }
}
