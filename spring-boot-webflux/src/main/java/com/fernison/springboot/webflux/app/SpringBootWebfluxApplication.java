package com.fernison.springboot.webflux.app;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.fernison.springboot.webflux.app.models.dao.ProductoDao;
import com.fernison.springboot.webflux.app.models.documents.Categoria;
import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

	@Autowired
	private ProductoService service;
	
	@Autowired
	private ReactiveMongoTemplate mongoTemplate;
	
	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		mongoTemplate.dropCollection("productos").subscribe(); // Borra toda la collection
		mongoTemplate.dropCollection("categorias").subscribe(); // Borra toda la collection
		Categoria c1 = new Categoria("C1");
		Categoria c2 = new Categoria("C2");
		Categoria c3 = new Categoria("C3");
		Categoria c4 = new Categoria("C4");

		Flux.just(
				c1, c2, c3, c4				
				)
			.flatMap(categoria -> {
				return service.saveCategoria(categoria);				
			})
			.doOnNext(categoria -> log.info("Insert: " +categoria.toString()))
			.thenMany( // Esto se usa para deviolver un Flux, en este caso, el Flux de productos. De esta forma creamos todo de una vez y en orden y solo hay un subscribe
					Flux.just(
							new Producto("tv 1", 111.89, c1),
							new Producto("tv 2", 222.89, c2),
							new Producto("tv 3", 333.89, c3),
							new Producto("tv 4", 444.89, c4),
							new Producto("tv 5", 555.89, c1),
							new Producto("tv 6", 666.89, c4)				
							)
						//.map(producto -> dao.save(producto))
						.flatMap(producto -> {
							producto.setCreateAt(new Date());
							return service.save(producto);				
						}) 
						.doOnNext(producto -> log.info("Insert: " + producto))
					)
			.subscribe();
		
//		Flux.just(
//				new Producto("tv 1", 111.89),
//				new Producto("tv 2", 222.89),
//				new Producto("tv 3", 333.89),
//				new Producto("tv 4", 444.89),
//				new Producto("tv 5", 555.89),
//				new Producto("tv 6", 666.89)				
//				)
//			//.map(producto -> dao.save(producto))
//			.flatMap(producto -> {
//				producto.setCreateAt(new Date());
//				return service.save(producto);				
//			}) // Usamos flatMap porque, devuelve Producto (lo aplana). Si usaramos map devolvería un Mono<Producto>
//				// FlatMap applies a function to each emitted element, but this function returns the type of the Observable. En este caso Producto
//			.subscribe(producto -> log.info("Insert: " + producto));
		
	}

}
