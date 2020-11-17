package com.fernison.springboot.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.fernison.springboot.webflux.app.models.documents.Categoria;
import com.fernison.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Mono;

public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String> {

	public Mono<Categoria> findByNombre(String nombre); // Método que infiere la query a partir del nombre del método

	
}
