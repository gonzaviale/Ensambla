package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarReferencias {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final RegistroDatosService registroDatosService;

    public List<ReferenciaItem> execute(ListarReferenciasRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        List<Registro> registros = registroDatosService.buscarReferenciables(
                negocioId, bloque.getId(), campos, request.texto(), request.ids());
        return ReferenciaItem.de(registros, campos);
    }
}
