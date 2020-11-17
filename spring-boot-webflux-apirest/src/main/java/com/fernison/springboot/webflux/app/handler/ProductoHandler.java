package com.fernison.springboot.webflux.app.handler;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.fernison.springboot.webflux.app.models.documents.Categoria;
import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {
	
	@Autowired
	private ProductoService service;
	
	@Autowired
	private Validator validator;
	
	@Value("${config.uploads.path}")
	private String path;
	
	public Mono<ServerResponse> listar(ServerRequest request) {
		return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.findAll(), Producto.class);
	}

	public Mono<ServerResponse> ver(ServerRequest request) {
		return service.findById(request.pathVariable("id"))
			.flatMap(p -> {
				return ServerResponse.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(fromValue(p)); // Escribe un objeto como parte de una ServerResponse reactiva
			})
			.switchIfEmpty(ServerResponse.notFound().build());

	}
	
	public Mono<ServerResponse> crear(ServerRequest request) {
		// Convertir los datos del request en un Mono<Producto>
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		return producto
			.flatMap(p -> {
				// Creamos el objeto Validator
				Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
				validator.validate(p, errors);
				if (errors.hasErrors()) {
					return Flux.fromIterable(errors.getFieldErrors()) // Devuelve un Flux con los errores 
						.map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage()) // Mapeamos cada error con un mensaje. Devuelve un Flux
						.collectList() // Pasamos el Flux a Mono
						.flatMap(list -> ServerResponse.badRequest().body(fromValue(list))); // Devolvemos la respuesta con la lista de errores
				} else {				
					if (p.getCreateAt() == null) {
						p.setCreateAt(new Date());
					}
					return service.findByNombre(p.getNombre())
						.flatMap(p_exists -> {
							Map<String, Object> respuesta = new HashMap<String, Object>();
							respuesta.put("errors", "Producto ya existe");
							respuesta.put("timestamp", new Date());
							return ServerResponse.ok()
									.contentType(MediaType.APPLICATION_JSON)
									.body(fromValue(respuesta));
						})
						.switchIfEmpty(
							 service.save(p)
								.flatMap(pdb -> ServerResponse.created(URI.create("/api/v2/productos/".concat(pdb.getId())))
													.contentType(MediaType.APPLICATION_JSON)
													.body(fromValue(pdb))
								)
						);										
				}
			});						
	}
	
	public Mono<ServerResponse> crearConFoto(ServerRequest request) {
		// Parar obtener la información del Producto que viene en el formData
		Mono<Producto> producto = request.multipartData().map(multipart -> {
			FormFieldPart nombre = (FormFieldPart)multipart.toSingleValueMap().get("nombre");
			FormFieldPart precio = (FormFieldPart)multipart.toSingleValueMap().get("precio");
			FormFieldPart categoriaId = (FormFieldPart)multipart.toSingleValueMap().get("categoria.id");
			FormFieldPart categoriaNombre = (FormFieldPart)multipart.toSingleValueMap().get("categoria.nombre");
			Categoria categoria = new Categoria(categoriaNombre.value());
			categoria.setId(categoriaId.value());
			return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
		});
		
		return request.multipartData()
			.map(multipart -> multipart.toSingleValueMap().get("file"))
			.cast(FilePart.class)
			.flatMap(file -> {
				 return producto
						.flatMap(p -> {
							p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
								.replace(" ", "")
								.replace(":", "")
								.replace("\\", ""));
							p.setCreateAt(new Date());
							return file.transferTo(new File(path + p.getFoto())).then(service.save(p));		
						})
						.flatMap(p -> {
							return ServerResponse.ok()
									.contentType(MediaType.APPLICATION_JSON)
									.body(fromValue(p));
						});		
		
		});
	}
	
	public Mono<ServerResponse> editar(ServerRequest request) {
		// Su forma, combinando el flujo de entrada con el flujo que obtenemos de la bbdd
//		Mono<Producto> producto = request.bodyToMono(Producto.class);
//		String id = request.pathVariable("id");
//		Mono<Producto> productoDb = service.findById(id);
//		return productoDb.zipWith(producto,  (db, req) -> { // db es el flujo de la bbdd y req el de la peticion
//			db.setNombre(req.getNombre());
//			db.setPrecio(req.getPrecio());
//			db.setCategoria(req.getCategoria());
//			return db;						
//		})
//		.flatMap(p -> {
//			return ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
//					.contentType(MediaType.APPLICATION_JSON)
//					.body(service.save(p), Producto.class)
//					.switchIfEmpty(ServerResponse.notFound().build());
//		});								
	
		// Mi forma -> Funciona
		Mono<Producto> monoProducto = request.bodyToMono(Producto.class);
		return monoProducto
			.flatMap(producto -> {
				return service.findById(request.pathVariable("id"))
					.flatMap(p -> {
						p.setNombre(producto.getNombre());
						p.setPrecio(producto.getPrecio());
						p.setCreateAt(producto.getCreateAt());
						p.setCategoria(producto.getCategoria());
						return service.save(p);
					})
					.flatMap(p -> {
						return ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(fromValue(p));
					})
					.switchIfEmpty(ServerResponse.notFound().build());	
			});
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest request) {
		return service.findById(request.pathVariable("id"))
			.flatMap(p -> {
				return service.delete(p).then(ServerResponse.noContent().build()); // Se usa then porque delete devuelve un Mono<Void> y es necesario ejecutar luego más acciones
			})
			.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> upload(ServerRequest request) {
		// Las 4 primeras lineas es para obtener el FilePart de la peticion
		return request.multipartData()
			.map(multipart -> multipart.toSingleValueMap().get("file"))
			.cast(FilePart.class)
			.flatMap(file -> {
				return service.findById(request.pathVariable("id"))
						.flatMap(p -> {
							p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
								.replace(" ", "")
								.replace(":", "")
								.replace("\\", ""));
							return file.transferTo(new File(path + p.getFoto())).then(service.save(p));		
						})
						.flatMap(p -> {
							return ServerResponse.ok()
									.contentType(MediaType.APPLICATION_JSON)
									.body(fromValue(p));
						})
						.switchIfEmpty(ServerResponse.notFound().build());		
		
		});
	}

	
}
