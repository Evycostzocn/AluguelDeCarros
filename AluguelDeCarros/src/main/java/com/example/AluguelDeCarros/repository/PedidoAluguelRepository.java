package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.PedidoAluguel;
import org.springframework.data.jpa.repository.*;

public interface PedidoAluguelRepository extends JpaRepository<PedidoAluguel, Long> {
    java.util.List<PedidoAluguel> findByClienteIdOrderByIdDesc(Long clienteId);
    java.util.List<PedidoAluguel> findAllByOrderByIdDesc();
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoAluguel p where p.id = :id")
    java.util.Optional<PedidoAluguel> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
