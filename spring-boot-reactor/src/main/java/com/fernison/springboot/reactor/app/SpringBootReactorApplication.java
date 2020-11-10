package com.fernison.springboot.reactor.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fernison.springboot.reactor.app.models.Comentarios;
import com.fernison.springboot.reactor.app.models.Usuario;
import com.fernison.springboot.reactor.app.models.UsuarioComentarios;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner { // CommandLineRunner para que se ejecute desde consola

	private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ejemploContraPresion();
		//ejemploIntervalDesdeCreate();
		//ejemploIntervalInfinito();
		//ejemploDelayElements();
		//ejemploInterval();
		//ejemploZipWithRangos();
		//ejemploUsuarioComentariosZipWithForma2();
		//ejemploUsuarioComentariosZipWith();
		//ejemploUsuarioComentariosFlatMap();
		//ejemploCollectList();
		//ejemploToString();
		//ejemploFlatMap();
		//ejemploIterable();
	}

	public void ejemploContraPresion() {
		// Dos formas de controlar la contrapresión:
		// Con el método limitRate donde directamente se le dice cuántos elementos se quieren por bloque
		// o
		// Creando un nuevo Subscriber donde tenemos el control de todo		
		Flux.range(1, 10)
			.log() // Muestra trazas del Observable
			.limitRate(2)
			.subscribe();
//		.subscribe(new Subscriber<Integer>() { // Implementamos un nuevo subscriber sobrecargando los métodos del interfaz Subscriber para controlar la contarpresión
//			private Subscription s;
//			private Integer limite = 2; // Tamaño del bloque que queremos pedir
//			private Integer consumido = 0; // Controla cuanto se ha consumido dentro de cada bloque
//			
//			@Override
//			public void onSubscribe(Subscription s) {
//				this.s = s;
//				//s.request(Long.MAX_VALUE); // Indica que envíe el número máximo de elementos posibles de una sola vez
//				s.request(limite); // Pedimos el límite (2)
//			}
//
//			@Override
//			public void onNext(Integer t) {
//				log.info(t.toString());
//				consumido++;
//				if (consumido == limite) {
//					consumido = 0;
//					s.request(limite); // Volvemos a pedir el límite (2)
//				}
//				
//			}
//
//			@Override
//			public void onError(Throwable t) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void onComplete() {
//				// TODO Auto-generated method stub
//				
//			}
//			
//		});
		
		
	}
	
	public void ejemploIntervalDesdeCreate() {
		Flux<Integer> fluxInt = Flux.create(emitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				private Integer contador = 0;				
				@Override
				public void run() {
					emitter.next(++contador);	
					if(contador == 10) { // Si llega a 10 se para el timer y el emitter termina
						timer.cancel();
						emitter.complete();
					}
					if(contador == 5) { // Si llega a 5 se lanza una excepcion (no se ejecutaría el onComplete)
						timer.cancel();
						emitter.error(new InterruptedException("Se ha detenido el flux en 5!"));
					}
					
				}				
			}, 1000, 1000);
		});
				   // doOnNext					// doOnError							// doOnComplete (solo se ejecuta si todo sale bien)
		fluxInt.subscribe(next -> log.info("" + next), error -> log.error(error.getMessage()), () -> log.info("Hemos terminado"));		 	
	}
	
	// Intervalo inifinito
	public void ejemploIntervalInfinito() throws InterruptedException {
		
		CountDownLatch latch = new CountDownLatch(1);
		
		Flux.interval(Duration.ofMillis(10))
			//.doOnTerminate(() -> latch.countDown()) // Esto no funciona ya que el stream no termina
			.flatMap(i -> { // FlatMap devolverá otro observable				
				if (i >= 5) {
					latch.countDown(); // Esto es lo que hace que el CountDownLatch (await) se desbloquee
					return Flux.error(new InterruptedException("Solo hasta 5!"));
				}
				return Flux.just(i);
			})
		  	.map(i -> "Hola " + i)
		  	//.retry(2) // Reintenta si hay alguna excepcion. Reintenta todo. No solo lo que ha dado error
		  	.subscribe(s -> log.info(s), er -> log.error(er.getMessage()));

		latch.await(); // Bloquea la ejecución
		
//		 Flux.interval(Duration.ofSeconds(1)) .map(i -> "Hola " + i) 
//		 .doOnNext(s -> log.info(s)) 
//		 .blockLast(); // Bloquea indefinidamente
		 	
	}
	
	public void ejemploDelayElements() {
		Flux<Integer> rango = Flux.range(1,  12)
			.delayElements(Duration.ofSeconds(1))
			.doOnNext(i -> log.info(i.toString()));
		rango.blockLast();
	}
	
	public void ejemploInterval() { // Se ejecuta en paralelo y en segundo plano. Sin bloqueo
		Flux<Integer> rango = Flux.range(1,  12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));		
		rango.zipWith(retraso, (ra, re) -> ra)
		.doOnTerminate(() -> log.info("Terminate"))
		.doOnNext(i -> log.info(i.toString()))
		//.subscribe();	
		.blockLast(); // Se suscribe al flujo y bloque ahasta que se reciba el ultimo. No es recomendable si se quiere pprocesamiento en paralelo. Lo hacemos para ver el log
	}
	
	// Ejemplo de uso de zipWith con Tuple (aunque se puede usar en cualquier función que devuelva Tuple)
	public void ejemploZipWithRangos() {
		Flux<Integer> rangos = Flux.range(0,  4);
		Flux.just(1, 2, 3, 4) // Flux original
		.map(i -> (i * 2))  
		.zipWith(rangos, (original, nuevo) -> String.format("Primer Flux: %d, Segundo Flux: %d", original, nuevo))
		.subscribe(res -> log.info(res.toString()));
	}
	
	// Ejemplo de uso de zipWith con Tuple (aunque se puede usar en cualquier función que devuelva Tuple)
	public void ejemploUsuarioComentariosZipWithForma2() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
		Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("com 1");
			comentarios.addComentario("com 2");
			comentarios.addComentario("com 3");
			comentarios.addComentario("com 4");
			return comentarios;
		});
		// Esto lo transforma en un Mono de UsuarioComentarios 
		Mono<UsuarioComentarios> usuarioComentarios = usuarioMono.zipWith(comentariosMono) // Esto devuelve un Mono<Tuple> (Tuple de dos posiciones -> usuario y comentarios)
				.map(tuple -> {
					Usuario u = tuple.getT1(); // Primer posición de la tupla
					Comentarios c = tuple.getT2(); // Segunda posición de la tupla
					return new UsuarioComentarios(u, c);
					
				});
		usuarioComentarios.subscribe(uc -> log.info(uc.toString()));
	}
	
	// zipWith -> Combina dos flujos
	// Igual resultado que ejemploUsuarioComentariosFlatMap
	public void ejemploUsuarioComentariosZipWith() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
		Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("com 1");
			comentarios.addComentario("com 2");
			comentarios.addComentario("com 3");
			comentarios.addComentario("com 4");
			return comentarios;
		});
		// Esto lo transforma en un Mono de UsuarioComentarios 
		Mono<UsuarioComentarios> usuarioComentarios = usuarioMono.zipWith(comentariosMono, (usuario, comentariosUsuario) -> new UsuarioComentarios(usuario, comentariosUsuario));
		usuarioComentarios.subscribe(uc -> log.info(uc.toString()));
		
	}
	
	public void ejemploUsuarioComentariosFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
		Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("com 1");
			comentarios.addComentario("com 2");
			comentarios.addComentario("com 3");
			comentarios.addComentario("com 4");
			return comentarios;
		});
		// Creamos un flujo que contenga la información de los dos streams anteriores. Crea un Mono de UsuarioComentarios		
		usuarioMono.flatMap(u -> comentariosMono.map(c -> new UsuarioComentarios(u, c)))
			.subscribe(uc -> log.info(uc.toString()));
		
	}
	
	// Convertir de Flux a Mono con collectList
	// Devuelve un solo objeto que se emite en lugar de con Flux que se emitirían objetos distintos (uno por cada elemento de la lista)
	public void ejemploCollectList() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Velarde"));
		usuariosList.add(new Usuario("Diego", "Gomez"));
		usuariosList.add(new Usuario("Paco", "Garcia"));
		usuariosList.add(new Usuario("Pedro", "Lopez"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));
		
		Flux.fromIterable(usuariosList)
			.collectList() // Pasa de n objetos de una lista a 1 objeto con una lista
			.subscribe(lista -> {
				lista.forEach(e -> log.info(e.toString()));	
			}); 		
	}
	
	// Pasa de Flux de Usuario a Mono de String con flatmap
	public void ejemploToString() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Velarde"));
		usuariosList.add(new Usuario("Diego", "Gomez"));
		usuariosList.add(new Usuario("Paco", "Garcia"));
		usuariosList.add(new Usuario("Pedro", "Lopez"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));
		
		Flux.fromIterable(usuariosList)
				.map(usuario -> usuario.getNombre().toUpperCase().concat(" " + usuario.getApellidos().toUpperCase()))
				.flatMap(nombre -> {
					if(nombre.contains("BRUCE")) {
						return Mono.just(nombre); // Flatmap devuelve otro observable que se une al original
					} else {
						return Mono.empty();
					}
				})
				.map(nombre -> {
					return nombre.toLowerCase();
				})
				.subscribe(nombre -> log.info(nombre.toString())); 
		
	}
	
	// Pasa de Flux de String a Mono de Usuario con flatmap
	public void ejemploFlatMap() throws Exception {
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andres Velarde");
		usuariosList.add("Diego Gomez");
		usuariosList.add("Paco Garcia");
		usuariosList.add("Pedro Lopez");
		usuariosList.add("Bruce Lee");
		usuariosList.add("Bruce Willis");
		
		Flux.fromIterable(usuariosList)
				.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				.flatMap(usuario -> {
					if(usuario.getNombre().equalsIgnoreCase("bruce")) {
						return Mono.just(usuario); // Flatmap devuelve otro observable partir de un observable original
					} else {
						return Mono.empty();
					}
				})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				})
				.subscribe(usuario -> log.info(usuario.toString())); 
		
	}
	
	public void ejemploIterable() throws Exception {
		// Creamos un observable
		//Flux<String> nombres = Flux.just("Andres Velarde", "Diego Gomez", "Paco Garcia", "Pedro Lopez", "Bruce Lee", "Bruce Willis"); // "Just" crea un Flux
		
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andres Velarde");
		usuariosList.add("Diego Gomez");
		usuariosList.add("Paco Garcia");
		usuariosList.add("Pedro Lopez");
		usuariosList.add("Bruce Lee");
		usuariosList.add("Bruce Willis");
		
		Flux<String> nombres = Flux.fromIterable(usuariosList); // Crea un Fllux a partir de una lista
		
		// Los streams son inmutables
		Flux<Usuario> usuarios = nombres.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				.filter(usuario -> usuario.getNombre().equals("BRUCE"))
				.doOnNext(usuario -> {
					if (usuario == null ) {
						throw new RuntimeException("El valor no puede ser vacío");
					}
					log.info(usuario.getNombre().concat(" ").concat(usuario.getApellidos())); // "doOnNext" se ejecuta cuando se recibe o se emite un elemento
				})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				});
		
		usuarios.subscribe(usuario -> log.info(usuario.toString()), error -> log.error(error.getMessage()), new Runnable() {

			@Override
			public void run() {
				log.info("Ha finalizado la ejecución del observable con éxito");
				
			}
			
		}); // Es un Observer que consume lo que emite un Observable. Runnable (es doOnComplete) lo implementamos como una clase anonima para evitar crear otra clase que implemente el interfaz
		
	}

}
