package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerFotoOrigen {

    private final NegocioRepository negocioRepository;
    private final RegistroRepository registroRepository;
    private final ArchivoCuadernoPort archivoCuadernoPort;
    private final LoteSubidaRepository loteSubidaRepository;

    public FotoArchivoResponse execute(ObtenerFotoOrigenRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroRepository.obtener(request.registroId(), negocioId);
        FotoCuaderno foto = loteSubidaRepository.findFotoByIdAndNegocioId(registro.exigirFotoOrigen(), negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        ArchivoFoto archivo = archivoCuadernoPort.leerFoto(negocioId, foto);
        return FotoArchivoResponse.de(archivo);
    }
}
