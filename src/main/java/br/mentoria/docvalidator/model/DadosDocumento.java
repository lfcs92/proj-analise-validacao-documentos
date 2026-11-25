package br.mentoria.docvalidator.model;

import java.time.LocalDate;

/** Dados extraídos do documento. {@code dataValidade} é opcional (nem todo documento tem). */
public record DadosDocumento(
        String nome,
        String cpf,
        LocalDate dataValidade) {

    public static DadosDocumento vazio() {
        return new DadosDocumento(null, null, null);
    }
}
