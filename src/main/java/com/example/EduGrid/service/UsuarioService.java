package com.example.EduGrid.service;

import java.text.Normalizer;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.Usuario;
import com.example.EduGrid.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario cadastrar(String nomeCompleto, String email, String senha) {
        String nome = nomeCompleto == null ? "" : nomeCompleto.trim().replaceAll("\\s+", " ");
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (nome.split(" ").length < 2) {
            throw new IllegalArgumentException("Informe nome e sobrenome.");
        }
        if (!emailNormalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalArgumentException("Já existe uma conta cadastrada com este e-mail.");
        }

        Usuario usuario = new Usuario(nome, emailNormalizado, codificarSenha(senha));
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String email, String senha) {
        if (email == null || senha == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(usuario -> verificarSenha(senha, usuario.getSenha()));
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado).orElse(null);
    }

    private String codificarSenha(String senha) {
        byte[] sal = new byte[16];
        new java.security.SecureRandom().nextBytes(sal);
        byte[] hash = derivarSenha(senha, sal, 210_000);
        return "pbkdf2$210000$" + java.util.Base64.getEncoder().encodeToString(sal)
                + "$" + java.util.Base64.getEncoder().encodeToString(hash);
    }

    private boolean verificarSenha(String senha, String armazenada) {
        try {
            String[] partes = armazenada.split("\\$");
            if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
                return false;
            }
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 100_000 || iteracoes > 1_000_000) {
                return false;
            }
            byte[] sal = java.util.Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = java.util.Base64.getDecoder().decode(partes[3]);
            return java.security.MessageDigest.isEqual(hashEsperado, derivarSenha(senha, sal, iteracoes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivarSenha(String senha, byte[] sal, int iteracoes) {
        javax.crypto.spec.PBEKeySpec especificacao = new javax.crypto.spec.PBEKeySpec(
                senha.toCharArray(), sal, iteracoes, 256);
        try {
            return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível proteger a senha.", e);
        } finally {
            especificacao.clearPassword();
        }
    }

    public static String iniciais(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return "EG";
        }
        String[] partes = Normalizer.normalize(nomeCompleto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }
}
