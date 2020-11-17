package com.fernison.springboot.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.fernison.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Mono;

public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {
	
	public Mono<Producto> findByNombre(String nombre); // Método que infiere la query a partir del nombre del método
	
	@Query("{ 'nombre':?0 }") // Consulta para buscar por nombre
	public Mono<Producto> obtenerPorNombre(String nombre); // La query se indica manualmente

}
