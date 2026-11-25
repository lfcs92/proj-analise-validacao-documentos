package br.mentoria.docvalidator.service.extracao;

import br.mentoria.docvalidator.model.DadosDocumento;

/** Transforma o texto bruto do OCR em dados estruturados. */
public interface ExtratorDados {

    DadosDocumento extrair(String textoOcr);
}
