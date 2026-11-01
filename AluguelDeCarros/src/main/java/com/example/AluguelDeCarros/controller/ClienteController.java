package com.example.AluguelDeCarros.controller;

import com.example.AluguelDeCarros.dto.ApiDtos.*;
import com.example.AluguelDeCarros.service.ClienteService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService service;
    public ClienteController(ClienteService service) { this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ClienteSaida cadastrar(@Valid @RequestBody Cadastro d) { return service.cadastrar(d); }
    @GetMapping("/me") public ClienteSaida consultar(Authentication a) { return service.consultar(a.getName()); }
    @PutMapping("/me") public ClienteSaida atualizar(Authentication a,@Valid @RequestBody DadosCliente d) { return service.atualizar(a.getName(),d); }
    @DeleteMapping("/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(Authentication a,HttpServletRequest req,HttpServletResponse res) {
        service.desativar(a.getName()); new SecurityContextLogoutHandler().logout(req,res,a);
    }
    @GetMapping("/me/vinculos-renda") public List<RendaSaida> rendas(Authentication a) { return service.rendas(a.getName()); }
    @PostMapping("/me/vinculos-renda") @ResponseStatus(HttpStatus.CREATED)
    public RendaSaida adicionar(Authentication a,@Valid @RequestBody RendaEntrada d) { return service.adicionar(a.getName(),d); }
    @PutMapping("/me/vinculos-renda/{id}")
    public RendaSaida atualizarRenda(Authentication a,@PathVariable Long id,@Valid @RequestBody RendaEntrada d) { return service.atualizarRenda(a.getName(),id,d); }
    @DeleteMapping("/me/vinculos-renda/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(Authentication a,@PathVariable Long id) { service.removerRenda(a.getName(),id); }
}
