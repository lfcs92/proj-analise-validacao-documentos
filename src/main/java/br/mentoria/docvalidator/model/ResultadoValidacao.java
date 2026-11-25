package br.mentoria.docvalidator.model;

import java.util.List;

public record ResultadoValidacao(StatusValidacao status, List<RegraResultado> regras) {
}
