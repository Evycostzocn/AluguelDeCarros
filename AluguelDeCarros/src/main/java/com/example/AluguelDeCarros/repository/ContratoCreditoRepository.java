package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.ContratoCredito;
import org.springframework.data.jpa.repository.*;

public interface ContratoCreditoRepository extends JpaRepository<ContratoCredito, Long> {
    java.util.Optional<ContratoCredito> findByContratoId(Long contratoId);
}
