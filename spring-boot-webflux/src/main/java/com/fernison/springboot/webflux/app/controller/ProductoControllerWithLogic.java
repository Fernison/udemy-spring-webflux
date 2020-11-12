package com.fernison.springboot.webflux.app.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;

//@Controller
public class ProductoControllerWithLogic {

	private static final Logger log = LoggerFactory.getLogger(ProductoControllerWithLogic.class);
	@Autowired
	private ProductoService service;
		
	@GetMapping({"/listar", "/"})
	public String listar(Model model) {
		Flux<Producto> productos = service.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				});
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar"; // "listar" es el nombre de la vista donde se van a mostrar estos datos y que se encuentra en "resources/templates"
	}
	
	// Full -> No hay contrapresión. Todo se envía de golpe
	// Si lo miramos en las herramientas de desarrollador de Chrome, vemos que su TTFB (TimeToFirstByte) es 1.2 m
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		Flux<Producto> productos = service.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				})
				.repeat(5000); // Repite el flujo 5000 veces
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
	
	// Data driver -> Se hace la contrapresión por número de elementos
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		Flux<Producto> productos = service.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				})
				.delayElements(Duration.ofSeconds(1)); // Envia los elementos cada segundo
		
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2)); // Envía los elementos a la vista por bloques de 2
		model.addAttribute("titulo", "Listado de productos");
		return "listar"; // "listar" es el nombre de la vista donde se van a mostrar estos datos y que se encuentra en "resources/templates"
	}
		
	// Chunked -> Por tamaño máximos de bytes a enviar cada vez. Recomendable.
	// Activando chunked, incluyendo "spring.thymeleaf.reactive.max-chunk-size = 1024" en application.properties el tiempo es de 59.96 s
	@GetMapping("/listar-chunked")
	public String listarChunked(Model model) {
		Flux<Producto> productos = service.findAll()
				.map(producto -> {
					producto.setNombre(producto.getNombre().toUpperCase());
					return producto;
				})
				.repeat(5000); // Repite el flujo 5000 veces
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
	
}
