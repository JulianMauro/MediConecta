package com.mediconecta.modulos.usuario.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 100)
	private String apellido;

	/** Es el identificador con el que el usuario hace login: no se puede repetir. */
	@Column(nullable = false, unique = true, length = 150)
	private String email;

	/** Nunca se guarda la clave en texto plano: aca va el hash BCrypt. */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(unique = true, length = 20)
	private String dni;

	@Column(length = 30)
	private String telefono;

	/** Se guarda como texto ("ADMIN") y no como numero, asi el orden del enum no rompe los datos. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	/** Baja logica: en vez de borrar el registro se desactiva. */
	@Column(nullable = false)
	private boolean activo;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	/** Constructor vacio: lo necesita Hibernate para instanciar la entidad. */
	protected Usuario() {
	}

	public Usuario(String nombre, String apellido, String email, String passwordHash, String dni, String telefono,
			Rol rol) {
		this.nombre = nombre;
		this.apellido = apellido;
		this.email = email;
		this.passwordHash = passwordHash;
		this.dni = dni;
		this.telefono = telefono;
		this.rol = rol;
		this.activo = true;
		this.fechaCreacion = Instant.now();
	}

	/** Actualiza los datos editables. El email y la password se cambian aparte. */
	public void actualizarDatos(String nombre, String apellido, String dni, String telefono, Rol rol) {
		this.nombre = nombre;
		this.apellido = apellido;
		this.dni = dni;
		this.telefono = telefono;
		this.rol = rol;
	}

	public void desactivar() {
		this.activo = false;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDni() {
		return dni;
	}

	public String getTelefono() {
		return telefono;
	}

	public Rol getRol() {
		return rol;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}
}
