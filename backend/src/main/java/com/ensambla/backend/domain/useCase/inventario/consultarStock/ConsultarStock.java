package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ConsultarStock {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    private final InventarioService inventarioService;

    public ConsultarStockResponse execute(ConsultarStockRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloque.getId(), campos, request.texto(), null, null, request.pagina(), request.tamanio());
        Pagina<Registro> productos = registroDatosService.listar(consulta);
        List<RegistroId> productoIds = productos.contenido().stream().map(Registro::getId).toList();
        Map<RegistroId, BigDecimal> stock = inventarioService.stockDe(productoIds, negocioId);
        return ConsultarStockResponse.de(productos, stock, campos);
    }
}
