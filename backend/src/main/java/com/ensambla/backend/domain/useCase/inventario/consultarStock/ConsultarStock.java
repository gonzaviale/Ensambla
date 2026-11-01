package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ConsultarStock {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final MovimientoStockRepository movimientoStockRepository;

    public ConsultarStockResponse execute(ConsultarStockRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloque.getId(), campos, request.texto(), null, null, request.pagina(), request.tamanio());
        Pagina<Registro> productos = registroRepository.buscar(consulta);
        List<RegistroId> productoIds = productos.contenido().stream().map(Registro::getId).toList();
        Map<RegistroId, BigDecimal> stock = movimientoStockRepository.stockCompletoDe(productoIds, negocioId);
        return ConsultarStockResponse.de(productos, stock, campos);
    }
}
