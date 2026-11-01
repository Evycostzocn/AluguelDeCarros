package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.ContratoAluguel;
import org.springframework.data.jpa.repository.*;

public interface ContratoAluguelRepository extends JpaRepository<ContratoAluguel, Long> {
    java.util.Optional<ContratoAluguel> findByPedidoId(Long pedidoId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContratoAluguel c where c.id = :id")
    java.util.Optional<ContratoAluguel> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
