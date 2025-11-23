package com.innosistemas.InnoSistemas.controller;

import com.innosistemas.InnoSistemas.domain.Role;
import com.innosistemas.InnoSistemas.repository.UserRepository;
import com.innosistemas.InnoSistemas.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String tokenType, long expiresInMinutes, MeResponse user) {}

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un JWT Bearer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            var roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            String token = jwtService.generateToken(request.username(), roles);
            var user = userRepository.findByUsername(request.username()).orElseThrow();
            List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
            var me = new MeResponse(user.getId(), user.getUsername(), user.getEmail(), roleNames);
            return ResponseEntity.ok(new LoginResponse(token, "Bearer", 60, me));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Usuario o contraseña incorrectos");
        }
    }

    public record MeResponse(Long id, String username, String email, List<String> roles) {}

    @Operation(summary = "Usuario actual", description = "Devuelve información del usuario autenticado",
            security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        var user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();
        List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        return ResponseEntity.ok(new MeResponse(user.getId(), user.getUsername(), user.getEmail(), roles));
    }

    @Operation(summary = "Cerrar sesión", description = "Logout stateless: el frontend debe descartar el token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
