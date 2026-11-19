package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        List<Campo> campos = obtenerCamposDelBloque(bloque, negocioId);
        List<Registro> registros = buscarReferenciables(negocioId, bloque, campos, request);
        return ReferenciaItem.de(registros, campos);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private List<Campo> obtenerCamposDelBloque(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private List<Registro> buscarReferenciables(NegocioId negocioId, Bloque bloque, List<Campo> campos,
                                                ListarReferenciasRequest request) {
        return registroDatosService.buscarReferenciables(
                negocioId, bloque.getId(), campos, request.texto(), request.ids());
    }
}
