package br.mentoria.docvalidator.dto;

import br.mentoria.docvalidator.model.DadosDocumento;
import br.mentoria.docvalidator.model.RegraResultado;
import br.mentoria.docvalidator.model.StatusValidacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentoResponse(
        UUID id,
        String arquivo,
        StatusValidacao status,
        DadosDocumento dados,
        List<RegraResultado> regras,
        Instant criadoEm) {
}
