package br.mentoria.docvalidator;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DocValidatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocValidatorApplication.class, args);
    }

    /** Relógio injetável: permite testar a regra "emissão < 90 dias" com data fixa. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
