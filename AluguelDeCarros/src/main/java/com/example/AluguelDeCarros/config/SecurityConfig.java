package com.example.AluguelDeCarros.config;

import com.example.AluguelDeCarros.repository.UsuarioRepository;
import com.example.AluguelDeCarros.service.IdentidadeService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return nome -> {
            var u=usuarios.findByUsuario(nome).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
            String perfil=IdentidadeService.perfil(u);
            String[] roles=perfil.equals("CLIENTE") ? new String[]{"CLIENTE"} : new String[]{"AGENTE",perfil};
            return User.withUsername(u.getUsuario()).password(u.getSenhaHash()).roles(roles).disabled(!u.isAtivo()).build();
        };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository context,
        CsrfTokenRepository csrf, UsuarioRepository usuarios, @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.cors(c -> c.configurationSource(cors))
            .csrf(c -> c.csrfTokenRepository(csrf))
            .securityContext(c -> c.securityContextRepository(context).requireExplicitSave(true))
            .requestCache(c -> c.disable())
            .formLogin(c -> c.disable()).httpBasic(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET,"/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/auth/login","/api/clientes").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/clientes/me/**").hasRole("CLIENTE")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> erro(res,401,"Autenticação necessária"))
                .accessDeniedHandler((req,res,ex) -> erro(res,403,"Acesso negado ou token CSRF inválido")))
            .logout(l -> l.logoutUrl("/api/auth/logout").invalidateHttpSession(true)
                .clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req,res,a) -> res.setStatus(204)))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                    var auth=SecurityContextHolder.getContext().getAuthentication();
                    if (auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                        boolean ativo=usuarios.findByUsuario(auth.getName()).map(u -> u.isAtivo()).orElse(false);
                        if (!ativo) {
                            if (req.getSession(false)!=null) req.getSession(false).invalidate();
                            SecurityContextHolder.clearContext();
                            erro(res,401,"Conta desativada"); return;
                        }
                    }
                    chain.doFilter(req,res);
                }
            }, AuthorizationFilter.class);
        return http.build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") List<String> origins) {
        CorsConfiguration c=new CorsConfiguration();
        c.setAllowedOrigins(origins); c.setAllowCredentials(true);
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",c); return source;
    }
    private static void erro(HttpServletResponse response,int status,String mensagem) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":"+status+",\"mensagem\":\""+mensagem+"\"}");
    }
}
