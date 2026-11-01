package com.example.AluguelDeCarros.service;

import com.example.AluguelDeCarros.model.*;
import com.example.AluguelDeCarros.repository.UsuarioRepository;
import com.example.AluguelDeCarros.dto.ApiDtos.UsuarioSaida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class IdentidadeService {
    private final UsuarioRepository usuarios;
    public IdentidadeService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    public Usuario usuario(String nome) {
        Usuario u = usuarios.findByUsuario(nome).orElseThrow(() -> new ApiException(401, "Autenticação necessária"));
        if (!u.isAtivo()) throw new ApiException(401, "Conta desativada");
        return u;
    }
    public Cliente cliente(String nome) {
        if (usuario(nome) instanceof Cliente c) return c;
        throw new ApiException(403, "Operação exclusiva de cliente");
    }
    public Agente agente(String nome) {
        if (usuario(nome) instanceof Agente a) return a;
        throw new ApiException(403, "Operação exclusiva de agente");
    }
    public static String perfil(Usuario u) {
        return u instanceof Agente a ? a.getTipo().name() : "CLIENTE";
    }
    public UsuarioSaida atual(String nome) {
        Usuario u = usuario(nome);
        return new UsuarioSaida(u.getId(), u.getUsuario(), perfil(u), u.isAtivo());
    }
}
