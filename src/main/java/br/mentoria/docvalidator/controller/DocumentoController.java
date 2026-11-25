package br.mentoria.docvalidator.controller;

import br.mentoria.docvalidator.dto.DocumentoResponse;
import br.mentoria.docvalidator.model.StatusValidacao;
import br.mentoria.docvalidator.service.DocumentoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    /** Recebe o arquivo, roda o pipeline completo e devolve o resultado. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoResponse enviar(@RequestParam("arquivo") MultipartFile arquivo) {
        return service.processar(arquivo);
    }

    @GetMapping("/{id}")
    public DocumentoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    /** Consulta de resultados, com filtro opcional: /api/documentos?status=APROVADO */
    @GetMapping
    public List<DocumentoResponse> listar(@RequestParam(required = false) StatusValidacao status) {
        return service.listar(status);
    }
}
