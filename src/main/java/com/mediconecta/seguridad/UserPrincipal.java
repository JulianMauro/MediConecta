package com.mediconecta.seguridad;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;


/**
 * Es el "usuario autenticado" que Spring Security guarda en el contexto durante el request.
 * Adapta nuestra entidad Usuario a la interfaz UserDetails que Spring espera.
 */
public class UserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String passwordHash;
	private final Rol rol;
	private final boolean activo;
	private final Collection<? extends GrantedAuthority> authorities;

	public UserPrincipal(Long id, String email, String passwordHash, Rol rol, boolean activo) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.rol = rol;
		this.activo = activo;
		// Spring Security espera el prefijo "ROLE_": asi hasRole('ADMIN') encuentra "ROLE_ADMIN".
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
	}

	public static UserPrincipal desde(Usuario usuario) {
		return new UserPrincipal(
				usuario.getId(),
				usuario.getEmail(),
				usuario.getPasswordHash(),
				usuario.getRol(),
				usuario.isActivo());
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public Rol getRol() {
		return rol;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	/** Para Spring el "username" es lo que identifica al usuario: en este sistema, el email. */
	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return activo;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
}
