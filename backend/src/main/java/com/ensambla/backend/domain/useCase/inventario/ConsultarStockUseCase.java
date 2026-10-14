package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.dto.inventario.ConsultarStockRequest;
import com.ensambla.backend.domain.dto.inventario.ConsultarStockResponse;
import com.ensambla.backend.domain.mapper.inventario.ConsultarStockMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarStockUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    private final InventarioService inventarioService;

    private final ConsultarStockMapper consultarStockMapper;

    public ConsultarStockResponse execute(ConsultarStockRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloque.getId(), campos, request.texto(), null, null, request.pagina(), request.tamanio());
        Pagina<Registro> productos = registroDatosService.listar(consulta);
        List<UUID> productoIds = productos.contenido().stream().map(Registro::getId).toList();
        Map<UUID, BigDecimal> stock = inventarioService.stockDe(productoIds, negocioId);
        return consultarStockMapper.toResponse(productos, stock, campos);
    }
}
