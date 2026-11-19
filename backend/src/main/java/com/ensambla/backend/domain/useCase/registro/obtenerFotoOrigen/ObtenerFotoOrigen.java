package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro registro = obtenerRegistro(request.registroId(), negocioId);
        FotoCuaderno foto = obtenerFotoDeOrigen(registro, negocioId);
        return FotoArchivoResponse.de(leerArchivo(negocioId, foto));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Registro obtenerRegistro(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private FotoCuaderno obtenerFotoDeOrigen(Registro registro, NegocioId negocioId) {
        return loteSubidaRepository.findFotoByIdAndNegocioId(registro.exigirFotoOrigen(), negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
    }

    private ArchivoFoto leerArchivo(NegocioId negocioId, FotoCuaderno foto) {
        return archivoCuadernoPort.leerFoto(negocioId, foto);
    }
}
