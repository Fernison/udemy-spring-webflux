package com.fernison.springboot.webflux.app.models.documents;

import java.util.Date;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

@Document(collection = "productos")
public class Producto {
	
	@Id
	private String id;
	
	@NotEmpty //Para Strings
	private String nombre;
	
	@NotNull // Que no sea nulo
	private Double precio;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd") // Para adaptar la fecha a lo que maneja nuestra aplicación y no usar el formato estándar de Spring
	private Date createAt;
	
	@Valid
	private Categoria categoria;
	
	private String foto;
		
	public Producto(String nombre, Double precio) {
		this.nombre = nombre;
		this.precio = precio;
	}
	public Producto(String nombre, Double precio, Categoria categoria) {
		this(nombre, precio);
		this.categoria = categoria;
	}
	public Producto() { } // Es necesario crear un constructor vacío para que JPA/Mongo lo use por debajo. En este caso no se usa JPA porque es la implementaciónr reactiva
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public Double getPrecio() {
		return precio;
	}
	public void setPrecio(Double precio) {
		this.precio = precio;
	}
	public Date getCreateAt() {
		return createAt;
	}
	public void setCreateAt(Date createAt) {
		this.createAt = createAt;
	}
	
	public Categoria getCategoria() {
		return categoria;
	}
	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}
	
	public String getFoto() {
		return foto;
	}
	public void setFoto(String foto) {
		this.foto = foto;
	}
	@Override
	public String toString() {
		return "Producto [id=" + id + ", nombre=" + nombre + ", precio=" + precio + ", createAt=" + createAt
				+ ", categoria=" + categoria + ", foto=" + foto + "]";
	}
	
	
}
