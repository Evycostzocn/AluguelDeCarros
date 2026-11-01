package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.AvaliacaoFinanceira;
import org.springframework.data.jpa.repository.*;

public interface AvaliacaoFinanceiraRepository extends JpaRepository<AvaliacaoFinanceira, Long> {
    java.util.Optional<AvaliacaoFinanceira> findByPedidoId(Long pedidoId);
}
