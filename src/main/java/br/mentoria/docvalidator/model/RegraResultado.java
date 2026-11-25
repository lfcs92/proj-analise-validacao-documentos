package br.mentoria.docvalidator.model;

/** Resultado da aplicação de UMA regra de negócio. */
public record RegraResultado(String regra, boolean atendida, String mensagem) {
}
