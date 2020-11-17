package com.fernison.springboot.webflux.client.app.handler;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.fernison.springboot.webflux.client.app.dto.Producto;
import com.fernison.springboot.webflux.client.app.services.ProductoService;

import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {

	@Autowired
	private ProductoService service;
	
	public Mono<ServerResponse> listar(ServerRequest request) {
		return ServerResponse
			.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.body(service.findAll(), Producto.class);			
	}
	
	public Mono<ServerResponse> ver(ServerRequest request) {
		String id = request.pathVariable("id");
		return service.findById(id)
			.flatMap(p -> ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(p)
				.switchIfEmpty(ServerResponse.notFound().build()))
			.onErrorResume(error -> doOnError(error));	}
	
	public Mono<ServerResponse> crear(ServerRequest request) {
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		return producto.flatMap(p-> {
			if (p.getCreateAt() == null) p.setCreateAt(new Date());
			return service.save(p)
				.flatMap(pr -> {
					return ServerResponse.created(URI.create("/api/client/".concat(pr.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(pr);
				})
				.onErrorResume(error -> {
					WebClientResponseException errorResponse = (WebClientResponseException)error; // Capturar la excepción que se produce. Podrían ser de otro tipo pero en general
																								// los errores Web de SpringWebFlux se propagan así
					if (errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
						return ServerResponse.badRequest()
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue(errorResponse.getResponseBodyAsString());
					} else {
						return Mono.error(errorResponse);
					}
				});
		});
	}
	
	public Mono<ServerResponse> editar(ServerRequest request) {
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		String id = request.pathVariable("id");
		// Su forma
//		return producto.flatMap(p-> {
//			return ServerResponse.created(URI.create("/api/client/".concat(id)))
//				.contentType(MediaType.APPLICATION_JSON)
//				.body(service.update(p, id), Producto.class);
//		});
		//También funciona
		return producto.flatMap(p-> {
			return service.update(p, id)
					.flatMap(pr -> {
						return ServerResponse.created(URI.create("/api/client/".concat(pr.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(pr);
					})
					.onErrorResume(error -> doOnError(error));
			});
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest request) {
		String id = request.pathVariable("id");
		return service.delete(id)
			.then(ServerResponse.noContent().build()) // Se usa then ya que delete devuelve un vacío
			.onErrorResume(error -> doOnError(error));	}
	
	public Mono<ServerResponse> upload(ServerRequest request) {
		String id = request.pathVariable("id");
		return request.multipartData()
			.map(multipart -> multipart.toSingleValueMap().get("file")) // Obtiene el part que nos envían. Se usa map porque primwro hay que convertirlo a FilePart
			.cast(FilePart.class)
			.flatMap(file -> service.upload(file, id))
			.flatMap(p -> {
				return ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(p);
			})
			.onErrorResume(this::doOnError); // Otra forma de invocar al método
	}
	
	// Trata el error
	// Su implementación es recibiendo un ServerResponse que se pasa desde los métodos. Creo que esto es más fácil e intuitivo
	private Mono<ServerResponse> doOnError(Throwable error) {
		WebClientResponseException errorResponse = (WebClientResponseException)error;
		if (errorResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
			// Así se personaliza el error que se quiere devolver
			Map<String, Object> body = new HashMap<String, Object>();
			body.put("error", "No existe el producto. Error: " + errorResponse.getMessage());
			body.put("timestamp", new Date());
			body.put("server_status", errorResponse.getStatusCode().value());
			return ServerResponse.status(HttpStatus.NOT_FOUND)
				.bodyValue(body);
		} else {
			return Mono.error(errorResponse);
		}
	}

}
