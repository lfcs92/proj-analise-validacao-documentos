package br.mentoria.docvalidator.repository;

import br.mentoria.docvalidator.entity.DocumentoProcessado;
import br.mentoria.docvalidator.model.StatusValidacao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<DocumentoProcessado, UUID> {

    List<DocumentoProcessado> findAllByOrderByCriadoEmDesc();

    List<DocumentoProcessado> findByStatusOrderByCriadoEmDesc(StatusValidacao status);
}
