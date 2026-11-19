package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        List<Campo> campos = obtenerCampos(bloque, negocioId);
        Pagina<Registro> productos = buscarProductos(consultaDe(request, negocioId, bloque, campos));
        return ConsultarStockResponse.de(productos, stockDe(productos, negocioId), campos);
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

    private ConsultaRegistros consultaDe(ConsultarStockRequest request, NegocioId negocioId, Bloque bloque,
                                         List<Campo> campos) {
        return ConsultaRegistros.armar(
                negocioId, bloque.getId(), campos, request.texto(), null, null, request.pagina(), request.tamanio());
    }

    private Pagina<Registro> buscarProductos(ConsultaRegistros consulta) {
        return registroRepository.buscar(consulta);
    }

    private Map<RegistroId, BigDecimal> stockDe(Pagina<Registro> productos, NegocioId negocioId) {
        List<RegistroId> productoIds = productos.contenido().stream().map(Registro::getId).toList();
        return movimientoStockRepository.stockCompletoDe(productoIds, negocioId);
    }
}
