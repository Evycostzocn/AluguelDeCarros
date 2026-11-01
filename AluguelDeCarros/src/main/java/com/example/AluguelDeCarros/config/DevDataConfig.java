package com.example.AluguelDeCarros.config;

import com.example.AluguelDeCarros.model.*;
import com.example.AluguelDeCarros.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("dev")
public class DevDataConfig {
    @Bean CommandLineRunner dadosDev(Environment env, UsuarioRepository usuarios, AgenteRepository agentes,
        AutomovelRepository automoveis, PasswordEncoder encoder, TransactionTemplate tx) {
        return args -> {
            if (!env.getProperty("app.dev.seed",Boolean.class,false)) return;
            String empresaSenha=senha(env,"DEV_EMPRESA_PASSWORD");
            String bancoSenha=senha(env,"DEV_BANCO_PASSWORD");
            tx.executeWithoutResult(status -> {
                Agente empresa=agente("demo.empresa",empresaSenha,"Empresa demonstração","11222333000181",TipoAgente.EMPRESA,usuarios,agentes,encoder);
                agente("demo.banco",bancoSenha,"Banco demonstração","11444777000161",TipoAgente.BANCO,usuarios,agentes,encoder);
                if (!automoveis.existsByMatricula("DEMO-001")) automoveis.save(new Automovel("DEMO-001",2024,"Fiat","Argo","DEM1A01",null,empresa));
                if (!automoveis.existsByMatricula("DEMO-002")) automoveis.save(new Automovel("DEMO-002",2023,"Volkswagen","Polo","DEM1A02",null,empresa));
            });
        };
    }
    private String senha(Environment env,String chave) {
        String s=env.getProperty(chave);
        if (s==null || s.isBlank() || s.length()<8 || s.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Configure a variável "+chave+" com 8 caracteres ou mais e até 72 bytes");
        return s;
    }
    private Agente agente(String nome,String senha,String razao,String cnpj,TipoAgente tipo,UsuarioRepository usuarios,AgenteRepository agentes,PasswordEncoder encoder) {
        var existente=usuarios.findByUsuario(nome);
        if (existente.isPresent()) {
            if (existente.get() instanceof Agente a && a.getTipo()==tipo) return a;
            throw new IllegalStateException("Login de demonstração já utilizado por perfil incompatível");
        }
        return agentes.save(new Agente(nome,encoder.encode(senha),razao,cnpj,tipo));
    }
}
