package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.Cliente;
import org.springframework.data.jpa.repository.*;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpfAndIdNot(String cpf, Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.usuario = :usuario")
    java.util.Optional<Cliente> bloquearPorUsuario(@org.springframework.data.repository.query.Param("usuario") String usuario);
}
