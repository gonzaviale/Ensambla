package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerFotoOrigen {

    private final NegocioService negocioService;
    private final RegistroDatosService registroDatosService;
    private final LoteSubidaService loteSubidaService;
    private final LoteSubidaRepository loteSubidaRepository;

    public FotoArchivoResponse execute(ObtenerFotoOrigenRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        FotoCuaderno foto = loteSubidaRepository.findFotoByIdAndNegocioId(registro.exigirFotoOrigen(), negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        ArchivoFoto archivo = loteSubidaService.leer(negocioId, foto);
        return FotoArchivoResponse.de(archivo);
    }
}
