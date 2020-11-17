package com.fernison.springboot.webflux.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import org.springframework.web.reactive.function.server.RouterFunction;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.fernison.springboot.webflux.app.handler.ProductoHandler;
import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

@Configuration
public class RouterFunctionConfig {
	
	// Configura las rutas de los handlers reactivos
	
	@Autowired
	private ProductoService service;
	
	// Ejemplo de ruta con el handler incluido
	@Bean
	public RouterFunction<ServerResponse> routes() {	
		return route(GET("/api/v2a/productos").or(GET("/api/v3a/productos")), request -> {
			return ServerResponse.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(service.findAll(), Producto.class);
		});
	}

	// Ejemplo de ruta con handler en otra clase
	@Bean
	public RouterFunction<ServerResponse> routes(ProductoHandler handler) {	
		return route(GET("/api/v2/productos"),handler::listar)
			.andRoute(GET("/api/v2/productos/{id}").and(contentType(MediaType.APPLICATION_JSON)), handler::ver) // El and indica que el contenttype tiene que ser JSON. Si no, da error
			.andRoute(POST("/api/v2/productos"), handler::crear)
			.andRoute(PUT("/api/v2/productos/{id}"), handler::editar)
			.andRoute(DELETE("/api/v2/productos/{id}"), handler::eliminar)
			.andRoute(POST("/api/v2/productos/upload/{id}"), handler::upload)
			.andRoute(POST("/api/v2/productos/crear-con-foto"), handler::crearConFoto);
	}

}
