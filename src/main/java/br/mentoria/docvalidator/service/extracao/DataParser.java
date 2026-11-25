package br.mentoria.docvalidator.service.extracao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Converte datas em formatos comuns (dd/MM/yyyy e yyyy-MM-dd) para LocalDate. */
public final class DataParser {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private DataParser() {}

    public static LocalDate parse(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        try {
            return t.contains("/") ? LocalDate.parse(t, BR) : LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            return null; // data ilegível = campo ausente (regra de negócio reprova depois)
        }
    }
}
