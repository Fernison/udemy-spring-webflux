package com.fernison.springboot.webflux.app;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fernison.springboot.webflux.app.models.documents.Categoria;
import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Mono;

// Con esta anotacion también se arranca el contexto de la aplicación 
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Arrancará un servidor para pruebas en un puerto random
@AutoConfigureWebTestClient // Si se mockea el servidor es necesario usar esta anotación para cargar el contexto de Spring
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Mockea el servidor
class SpringBootWebfluxApirestApplicationTests {

	@Autowired
	private WebTestClient client; // Para probar los endpoints de manera reactiva
	
	@Autowired
	private ProductoService service;
	
	@Value("${config.base.endpoint}")
	private String url;
	
	@Test
	@Order(1)
	public void listarTest() {		
		client.get()
			.uri(url)
			.accept(MediaType.APPLICATION_JSON)
			.exchange() // Hace la peticion
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBodyList(Producto.class)
//			.consumeWith(response -> {
//				// Esto permite trabajar manualmente con la respuesta
//				List<Producto> productos = response.getResponseBody(); // Sabe que es un List de productos porque se lo hemos dicho en "expectBodyList"
//				productos.forEach(p -> {
//					System.out.println(p.getNombre());
//				});
//				Assertions.assertTrue(productos.size() == 6); // Así se pueden hacer assertion manuales
//			})
			.hasSize(7) // Número de elementos de la lista. Si los ejecuto todos a la vez me sale uno mñas. Entiendo que es porque se ejecutan los tests a la vez. Lo de @Order no hace diferencia
			;
	}
	
	
	@Test
	//@Order(2)
	public void verTest() {	
		Producto producto = service.findByNombre("tv 6").block(); // Es necesario usar el block ya que necesitamos que la ejecución sea síncrona dentro de los tests		
		client.get()
			.uri(url + "/{id}", Collections.singletonMap("id", producto.getId())) 
			.header("Content-Type", MediaType.APPLICATION_JSON.toString()) // Content-Type de la petición
			.accept(MediaType.APPLICATION_JSON) // COntent+Type aceptado para la petición pero no indica que se esté enviando
			.exchange() // Hace la peticion
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody(Producto.class)
			.consumeWith(response -> {
				Producto p = response.getResponseBody();
				Assertions.assertTrue(p.getId() != null && !p.getId().trim().equals(""));
				Assertions.assertTrue(p.getNombre().equals("tv 6"));
			})
//			.expectBody()
//			.jsonPath("$.id").isNotEmpty()
//			.jsonPath("$.nombre").isEqualTo("tv 3");
			;
	}
	
	@Test
	//@Order(3)
	public void crearTest() {	
		
		Categoria categoria = service.findCategoriaByNombre("C2").block();
		Producto producto = new Producto("tv 100", 1000.34, categoria);
		
		client.post()
			.uri(url) 
			.header("Content-Type", MediaType.APPLICATION_JSON.toString())
			.accept(MediaType.APPLICATION_JSON)
			.body(Mono.just(producto), Producto.class)
			.exchange() // Hace la peticion
			.expectStatus().isCreated()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
//			.expectBody(Producto.class)
//			.consumeWith(response -> {
//				Producto p = response.getResponseBody();
//				Assertions.assertTrue(p.getId() != null && !p.getId().trim().equals(""));
//				Assertions.assertTrue(p.getNombre().equals("tv 100"));
//				Assertions.assertTrue(p.getCategoria().getNombre().equals("C2"));
//			})
//			.expectBody()
//			.jsonPath("$.id").isNotEmpty()
//			.jsonPath("$.nombre").isEqualTo("tv 100")
//			.jsonPath("$.categoria.nombre").isEqualTo("C2")
			// Compatible con RestController ya que lao que devuelve es distinto a lo que devuelve el handler de crear
			.expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() { })
			.consumeWith(response -> {
				Object o = response.getResponseBody().get("producto");
				Producto p = new ObjectMapper().convertValue(o, Producto.class);
				Assertions.assertTrue(p.getId() != null && !p.getId().trim().equals(""));
				Assertions.assertTrue(p.getNombre().equals("tv 100"));
				Assertions.assertTrue(p.getCategoria().getNombre().equals("C2"));
			})
//			.expectBody()
//			.jsonPath("$.producto.id").isNotEmpty()
//			.jsonPath("$.producto.nombre").isEqualTo("tv 100")
//			.jsonPath("$.producto.categoria.nombre").isEqualTo("C2")
			;
	}

	@Test
	//@Order(4)
	public void editarTest() {	
		Producto producto = service.findByNombre("tv 3").block(); // Es necesario usar el block ya que necesitamos que la ejecución sea síncrona dentro de los tests	
		Categoria categoria = service.findCategoriaByNombre("C4").block();
		Producto productoEditado = new Producto("tv 200", 1000.34, categoria);
		
		client.put()
			.uri(url + "/{id}", Collections.singletonMap("id", producto.getId())) 
			.header("Content-Type", MediaType.APPLICATION_JSON.toString()) // Content-Type de la petición
			.accept(MediaType.APPLICATION_JSON) // COntent+Type aceptado para la petición pero no indica que se esté enviando
			.body(Mono.just(productoEditado), Producto.class)
			.exchange() // Hace la peticion
			.expectStatus().isCreated()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody(Producto.class)
			.consumeWith(response -> {
				Producto p = response.getResponseBody();
				Assertions.assertTrue(p.getId() != null && p.getId().trim().equals(producto.getId()));
				Assertions.assertTrue(p.getNombre().equals("tv 200"));
				Assertions.assertTrue(p.getCategoria().getNombre().equals("C4"));
			})
//			.expectBody()
//			.jsonPath("$.id").isEqualTo(producto.getId());
//			.jsonPath("$.nombre").isEqualTo("tv 100");
			;
	}
	
	@Test
	//@Order(5)
	public void eliminarTest() {	
		Producto producto = service.findByNombre("tv 200").block(); // Es necesario usar el block ya que necesitamos que la ejecución sea síncrona dentro de los tests	
		
		client.delete()
			.uri(url + "/{id}", Collections.singletonMap("id", producto.getId())) 
			.header("Content-Type", MediaType.APPLICATION_JSON.toString()) // Content-Type de la petición
			.accept(MediaType.APPLICATION_JSON) // COntent+Type aceptado para la petición pero no indica que se esté enviando
			.exchange() // Hace la peticion
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();
		// Comprobamos que se haya borrado el registro
		client.get()
			.uri(url + "/{id}", Collections.singletonMap("id", producto.getId())) 
			.header("Content-Type", MediaType.APPLICATION_JSON.toString()) // Content-Type de la petición
			.accept(MediaType.APPLICATION_JSON) // COntent+Type aceptado para la petición pero no indica que se esté enviando
			.exchange() // Hace la peticion
			.expectStatus().isNotFound()
			.expectBody()
			.isEmpty();
	}
	
}
