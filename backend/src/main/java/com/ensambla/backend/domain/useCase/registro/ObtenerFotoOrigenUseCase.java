package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerFotoOrigenUseCase {

    private final NegocioService negocioService;
    private final RegistroDatosService registroDatosService;
    private final LoteSubidaService loteSubidaService;
    private final FotoArchivoMapper fotoArchivoMapper;

    public FotoArchivoResponse execute(ObtenerFotoOrigenRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        ArchivoFoto foto = loteSubidaService.leerFoto(negocioId, registro.exigirFotoOrigen());
        return fotoArchivoMapper.toResponse(foto);
    }
}
