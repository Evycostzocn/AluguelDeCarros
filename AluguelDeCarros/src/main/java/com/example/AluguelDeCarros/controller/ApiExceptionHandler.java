package com.example.AluguelDeCarros.controller;

import com.example.AluguelDeCarros.dto.ApiDtos.Erro;
import com.example.AluguelDeCarros.service.ApiException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<Erro> erro(int s,String m) { return ResponseEntity.status(s).body(new Erro(s,m)); }
    @ExceptionHandler(ApiException.class) ResponseEntity<Erro> negocio(ApiException e) { return erro(e.getStatus(),e.getMessage()); }
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<Erro> login(AuthenticationException e) { return erro(401,"Credenciais inválidas ou conta desativada"); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,ConstraintViolationException.class,IllegalArgumentException.class})
    ResponseEntity<Erro> invalido(Exception e) { return erro(400,"Dados inválidos; confira os campos enviados"); }
    @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class,IllegalStateException.class})
    ResponseEntity<Erro> conflito(Exception e) { return erro(409,"Operação em conflito com o estado atual ou dados já cadastrados"); }
    @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<Erro> ausente(Exception e) { return erro(404,"Recurso não encontrado"); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Erro> metodo(Exception e) { return erro(405,"Método HTTP não permitido"); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Erro> formato(Exception e) { return erro(415,"Envie o corpo em application/json"); }
    @ExceptionHandler(Exception.class) ResponseEntity<Erro> inesperado(Exception e) { return erro(500,"Não foi possível concluir a operação"); }
}
