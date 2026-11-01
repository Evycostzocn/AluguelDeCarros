package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.Usuario;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    java.util.Optional<Usuario> findByUsuario(String usuario);
    boolean existsByUsuario(String usuario);
}
