package com.example.AluguelDeCarros.repository;

import com.example.AluguelDeCarros.model.Automovel;
import org.springframework.data.jpa.repository.*;

public interface AutomovelRepository extends JpaRepository<Automovel, Long> {
    boolean existsByMatricula(String matricula);
}
