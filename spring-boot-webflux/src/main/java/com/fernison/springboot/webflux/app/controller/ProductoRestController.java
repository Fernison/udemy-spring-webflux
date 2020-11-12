package com.fernison.springboot.webflux.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fernison.springboot.webflux.app.models.dao.ProductoDao;
import com.fernison.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	@Autowired
	private ProductoDao dao;
	
	@GetMapping
	public Flux<Producto> index() {
		Flux<Producto> productos = dao.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				})
				.doOnNext(producto -> log.info(producto.toString()));
		return productos;
	}
	
	// Forma fácil y eficiente
//	@GetMapping("/{id}")
//	public Mono<Producto> showEasy(@PathVariable String id) {
//		Mono<Producto> producto = dao.findById(id);
//		return producto;
//	}
	
	// Forma más compleja
	@GetMapping("/{id}")
	public Mono<Producto> showFilter(@PathVariable String id) {
		return dao.findAll()
				.filter(producto -> producto.getId().equals(id))
				.next(); // "next" retorna el Mono de la consulta ya que devuelve el primer elemento de un Flux como Mono
	}
}
