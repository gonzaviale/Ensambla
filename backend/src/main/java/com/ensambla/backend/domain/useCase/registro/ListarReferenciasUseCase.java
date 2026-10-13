package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ListarReferenciasRequest;
import com.ensambla.backend.domain.dto.registro.ReferenciaItem;
import com.ensambla.backend.domain.mapper.registro.ListarReferenciasMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarReferenciasUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    private final ListarReferenciasMapper listarReferenciasMapper;

    public List<ReferenciaItem> execute(ListarReferenciasRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        List<Registro> registros = registroDatosService.buscarReferenciables(
                negocioId, bloque.getId(), campos, request.texto(), request.ids());
        return listarReferenciasMapper.toResponse(registros, campos);
    }
}
