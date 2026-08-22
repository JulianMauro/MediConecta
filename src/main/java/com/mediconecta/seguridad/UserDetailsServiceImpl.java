package com.mediconecta.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


/** Le dice a Spring Security como buscar un usuario en nuestra base. */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return usuarioRepository.findByEmail(email.trim().toLowerCase())
				.map(UserPrincipal::desde)
				.orElseThrow(() -> new UsernameNotFoundException("usuario no encontrado: " + email));
	}
}
