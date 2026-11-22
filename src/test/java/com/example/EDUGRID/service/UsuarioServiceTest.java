package com.example.EduGrid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Usuario;
import com.example.EduGrid.repository.UsuarioRepository;

class UsuarioServiceTest {

    @Test
    void cadastraComEmailNormalizadoESenhaProtegidaEPermiteLogin() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.existsByEmailIgnoreCase("ana.silva@escola.edu")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioService service = new UsuarioService(repository);

        Usuario usuario = service.cadastrar("  Ana   Silva ", " Ana.Silva@Escola.edu ", "senha-segura-1");
        when(repository.findByEmailIgnoreCase("ANA.SILVA@ESCOLA.EDU"))
                .thenReturn(Optional.of(usuario));

        assertEquals("Ana Silva", usuario.getNomeCompleto());
        assertEquals("ana.silva@escola.edu", usuario.getEmail());
        assertNotEquals("senha-segura-1", usuario.getSenha());
        assertTrue(usuario.getSenha().startsWith("pbkdf2$"));
        assertTrue(service.autenticar("ANA.SILVA@ESCOLA.EDU", "senha-segura-1").isPresent());
        assertTrue(service.autenticar("ANA.SILVA@ESCOLA.EDU", "senha-incorreta").isEmpty());
    }

    @Test
    void iniciaisUsamPrimeiraLetraDoNomeESobrenome() {
        assertEquals("IS", UsuarioService.iniciais("Ícaro da Silva"));
        assertEquals("E", UsuarioService.iniciais("EduGrid"));
        assertEquals("EG", UsuarioService.iniciais(" "));
    }

    @Test
    void rejeitaNomeSemSobrenome() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        UsuarioService service = new UsuarioService(repository);

        assertFalse(cadastroValido(service));
    }

    private boolean cadastroValido(UsuarioService service) {
        try {
            service.cadastrar("Ana", "ana@escola.edu", "senha-segura-1");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
