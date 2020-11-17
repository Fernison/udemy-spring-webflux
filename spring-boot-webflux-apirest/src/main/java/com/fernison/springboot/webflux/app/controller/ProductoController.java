package com.fernison.springboot.webflux.app.controller;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@Autowired
	private ProductoService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	@GetMapping
	public Flux<Producto> listaRetornandoFlux() {
		return service.findAll();
	}
	
	// Otra forma de devolver información no usando directamente un Flux o un Mono de un objeto, si no un Mono de ResponseEntity que contiene toda la info necesaria de la respuesta
	@GetMapping("/listar-response-entity")
	public Mono<ResponseEntity<Flux<Producto>>> listaRetornandoResponseEntity() {
		return Mono.just(
				ResponseEntity.ok() // Por defecto el content-type es application/json
					.body(service.findAll())
			);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Producto>> ver(@PathVariable String id) {
		return service.findById(id)
				.map(p -> ResponseEntity.ok() // Se mapea la respuesta para convertirla en un ResponseEntity<Producto> ya que ResponseEntity no es reactiva
						.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build()); // Si no existe el producto devuelve un 404		
	}
	
	// Otra opción para obtener el producto por Id. El problema es que aquí no se puede controlar si el producto no existe. Devolvería una respuesta vacía
	@GetMapping("/v2/{id}")
	public Mono<ResponseEntity<Mono<Producto>>> verV2(@PathVariable String id) {
		return Mono.just(
				ResponseEntity.ok() // Por defecto el content-type es application/json
					.body(service.findById(id)));
				//.defaultIfEmpty(ResponseEntity.notFound().build()); // Estas opciones no funcionan
				//defaultIfEmpty(Mono.just(ResponseEntity.notFound().build()))));
	}
	
		
	private Mono<ResponseEntity<Map<String, Object>>> internal_crear(@Valid Mono<Producto> monoProducto, Map<String, Object> respuesta) { 
		log.info("internal_crear");
		return monoProducto
			.flatMap(producto -> {
				log.info(producto.toString());
				if (producto.getCreateAt() == null) {
					producto.setCreateAt(new Date());
				}
				return service.save(producto)
						.map(p -> {
							respuesta.put("producto", p);
							respuesta.put("mensaje", "Producto creado con exito");
							respuesta.put("timestamp", new Date());
							return ResponseEntity.created(URI.create("/api/productos/".concat(p.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(respuesta);
							
						});
			})
			.onErrorResume(t -> {
				return Mono.just(t).cast(WebExchangeBindException.class) // Instancia un error de tipo WebExchangeBindException
						.flatMap(e -> Mono.just(e.getFieldErrors())) // Convierte a un Mono<FieldErrors>
						.flatMapMany(errors -> Flux.fromIterable(errors)) // Para poder trabajar con cada campo de los field errors hay que pasarlo a Flux
						.map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage()) // Se trata la lista y se pasa a String por cada elemento de la lista
						.collectList() // Pasa de Flux a Mono para poder retornarlo
						.flatMap(list -> { // Devuelve la respuesra
							respuesta.put("errors", list);
							respuesta.put("status", HttpStatus.BAD_REQUEST.value());
							respuesta.put("timestamp", new Date());
							return Mono.just(ResponseEntity.badRequest().body(respuesta));
						});
			});		
	}
	
	// Valida el producto y devuelve una lista con los posibles errores que se hayan producido
	// Para que funcione hay que enviar un JSON vacío ({ }). Si no se envía nada devuelve un 500
	@PostMapping
	public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto) { 
		// RequestBody permite poblar el objeto producto con los datos que vengan en el body de la peticion
		// Si se pone el @Valid es mejor que el parámetro sea un Publisher así si hay algún error lo podemos capturar con el onError
		Map<String, Object> respuesta = new HashMap<String, Object>();
		
		// Crea y comprueba con @Valid pero no conprueba si ya existe
		//return internal_crear(monoProducto, respuesta);
		
		// Comprueba que no exista ya, da de alta, y valida con @Valid
		return monoProducto
				.flatMap(pr1 -> {	
					// Esto validaría manualmente y sin usar @Valid (da error si se usa)
//					if (pr1 == null || pr1.getNombre() == null) {
//						respuesta.put("errors", "Faltan parámetros en la petición");
//						respuesta.put("status", HttpStatus.BAD_REQUEST.value());
//						respuesta.put("timestamp", new Date());
//						return Mono.just(ResponseEntity.badRequest().body(respuesta));
//					}
					return service.findByNombre(pr1.getNombre())
						.flatMap(p_exists -> {
							log.info(p_exists.toString());
							respuesta.put("errors", "Producto ya existe");
							respuesta.put("status", HttpStatus.OK.value());
							respuesta.put("timestamp", new Date());
							return Mono.just(ResponseEntity.badRequest().body(respuesta));
						})
						.switchIfEmpty(internal_crear(Mono.just(pr1), respuesta));
				})
				.onErrorResume(t -> {
					return Mono.just(t).cast(WebExchangeBindException.class) // Instancia un error de tipo WebExchangeBindException
							.flatMap(e -> Mono.just(e.getFieldErrors())) // Convierte a un Mono<FieldErrors>
							.flatMapMany(errors -> Flux.fromIterable(errors)) // Para poder trabajar con cada campo de los field errors hay que pasarlo a Flux
							.map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage()) // Se trata la lista y se pasa a String por cada elemento de la lista
							.collectList() // Pasa de Flux a Mono para poder retornarlo
							.flatMap(list -> { // Devuelve la respuesra
								respuesta.put("errors", list);
								respuesta.put("status", HttpStatus.BAD_REQUEST.value());
								respuesta.put("timestamp", new Date());
								return Mono.just(ResponseEntity.badRequest().body(respuesta));
							});
				});	

	}
	
	// Este método crea el producto y sube la foto a la vez. No se puede enviar la informacion en JSON ya que es necesario usar un body de tipo form-data en la petición.
	// Por tanto, no se puede usar la anotación @RequestBody
	@PostMapping("/v2")
	public Mono<ResponseEntity<Producto>> crearConFoto(Producto producto, @RequestPart FilePart file) {
		if (producto.getCreateAt() == null) {
			producto.setCreateAt(new Date());
		}
		producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
		.replace(" ", "")
		.replace(":", "")
		.replace("\\", ""));
		return file.transferTo(new File(path + producto.getFoto())).then(service.save(producto))
			.map(p -> ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(p))
			.defaultIfEmpty(ResponseEntity.notFound().build()); // Si no existe el producto devuelve un 404	
	
	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Producto>> editar(@RequestBody Producto producto, @PathVariable String id) {
		return service.findById(id)
				.flatMap(p -> {
					p.setNombre(producto.getNombre());
					p.setPrecio(producto.getPrecio());
					p.setCreateAt(producto.getCreateAt());
					p.setCategoria(producto.getCategoria());
					return service.save(p);
				})
				.map(p -> ResponseEntity.created(URI.create("/api/productos/".concat(p.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build()); // Si no existe el producto devuelve un 404	
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id) {
		// El método devuelve Void (al igual que service.delete). Por tanto, despues de service.delete hay que indicar como es la respuesta ya que si lo hacemos con map, siempre estaría vacío ya que la 
		// respuesta es Void, y no haría ningún tratamiento.
		return service.findById(id)
				.flatMap(p -> {
					return service.delete(p).then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT))); // Se borra y se devuelve un no content
				})
				.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND)); // Si no existe el producto devuelve un 404
		
	}
	
	@PostMapping("/upload/{id}")
	public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file) { // "file" es también como se tiene que llamar el parámetro en la petición
		return service.findById(id)
			.flatMap(p -> {
//				if (!file.filename().isEmpty()) {
					p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
					.replace(" ", "")
					.replace(":", "")
					.replace("\\", ""));
//				}
				return file.transferTo(new File(path + p.getFoto())).then(service.save(p));							
			})
			.map(p -> ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(p))
			.defaultIfEmpty(ResponseEntity.notFound().build()); // Si no existe el producto devuelve un 404	
		
		
		
	}
	
	
}
