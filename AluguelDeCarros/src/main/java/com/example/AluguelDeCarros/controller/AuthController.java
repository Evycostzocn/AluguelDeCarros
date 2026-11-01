package com.example.AluguelDeCarros.controller;

import com.example.AluguelDeCarros.dto.ApiDtos.*;
import com.example.AluguelDeCarros.service.IdentidadeService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository repository;
    private final CsrfTokenRepository csrfRepository;
    private final IdentidadeService identidade;
    public AuthController(AuthenticationManager manager,SecurityContextRepository repository,CsrfTokenRepository csrfRepository,IdentidadeService identidade) {
        this.manager=manager; this.repository=repository; this.csrfRepository=csrfRepository; this.identidade=identidade;
    }
    @GetMapping("/csrf") public CsrfSaida csrf(CsrfToken token) { return new CsrfSaida(token.getHeaderName(),token.getToken()); }
    @PostMapping("/login") public UsuarioSaida login(@Valid @RequestBody Login dados,HttpServletRequest request,HttpServletResponse response) {
        Authentication auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(dados.usuario(),dados.senha()));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,request,response);
        csrfRepository.saveToken(null,request,response);
        var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context,request,response);
        return identidade.atual(auth.getName());
    }
    @GetMapping("/me") public UsuarioSaida me(Authentication auth) { return identidade.atual(auth.getName()); }
}
