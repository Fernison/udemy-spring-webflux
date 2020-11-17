package com.fernison.springboot.webflux.app.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.fernison.springboot.webflux.app.models.documents.Categoria;
import com.fernison.springboot.webflux.app.models.documents.Producto;
import com.fernison.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto") // Persiste en sesion los objetos de tipo producto. Esto vale para no tener qwue estar pasando los ids y que se pueda actualizar directamente
@Controller
public class ProductoController {

	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@Value("${config.uploads.path}")
	private String path;
	
	@Autowired
	private ProductoService service;
	
	@ModelAttribute("categorias") // Con esto se asigna directamente a la vista sin necesidad de hacer model.addAttribute
								  // Se invoca cada vez que se llama a "categorias" desde el formulario. Es buena solución para los select con datos
	public Flux<Categoria> categorias() {
		return service.findAllCategoria();
	}
		
	@GetMapping({"/ver/{id}"})
	public Mono<String> ver(@PathVariable String id, Model model) {
		return service.findById(id)
				.doOnNext(p-> {
					model.addAttribute("producto", p);
					model.addAttribute("titulo", "Detalle de producto");
				})
				.switchIfEmpty(Mono.just(new Producto()))
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					}
					return Mono.just(p);
				})
				.then(Mono.just("ver"))
				.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}
	
	// Devuelve la imagen para mostrarla en el navegador
	@GetMapping({"/uploads/img/{nombreFoto:.+}"}) // ":.+" nos permite pasar la extensión del archivo
	public Mono<ResponseEntity<Resource>> ver(@PathVariable String nombreFoto) throws MalformedURLException {
		Path ruta = Paths.get(path).resolve(nombreFoto).toAbsolutePath();
		Resource imagen = new UrlResource(ruta.toUri());
		// También se puede devolver un Mono de ResponseEntity que contenga la respuesta
		return Mono.just(
				ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imagen.getFilename() + "\"")
					.body(imagen)
				);
	}
	
	@GetMapping({"/listar", "/"})
	public Mono<String> listar(Model model) {
		Flux<Producto> productos = service.findAllConNombreUpperCase();
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return Mono.just("listar"); // "listar" es el nombre de la vista donde se van a mostrar estos datos y que se encuentra en "resources/templates"
	}
	
	@GetMapping("/form")
	public Mono<String> crear(Model model) {
		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Forumulario de producto");
		model.addAttribute("boton", "Crear");
		return Mono.just("form");
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(@Valid Producto producto, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status) { 
		// @Valid indica que se valide el objeto
		// Siempre se usa con BindingResult y el orden de ambos en la signatura es el usado en este metodo
		if (result.hasErrors()) {
			model.addAttribute("titulo", "Errores en el formulario de producto");
			model.addAttribute("boton", "Guardar");
			model.addAttribute("producto", producto);
			// NO haría falta pasarle "producto" ya que tiene el mismo nombre en la signatura del método
			return Mono.just("form");
		} else {
			status.setComplete(); // Elimina el objeto de la sesion. Solo si se usa SessionAttributes
			if (producto.getCreateAt() == null) {
				producto.setCreateAt(new Date());
			}
			Mono<Categoria> categoria = service.findCategoriaById(producto.getCategoria().getId());
			return categoria.flatMap(c -> {
				if (!file.filename().isEmpty()) {
					producto.setFoto(UUID.randomUUID() + "-" + file.filename()
						.replace(" ", "")
						.replace(":", "")
						.replace("\\", "")
					);
				}
				producto.setCategoria(c);
				return service.save(producto);
			})
			.doOnNext(p -> {
				log.info("Producto guardado: " + p.getNombre() + " Id: " + p.getId());
				log.info("Categoria asignada: " + p.getCategoria().getNombre() + " Id: " + p.getCategoria().getId());
			})
			.flatMap(p -> { // Se usa un flatMap porque usamos transferTo que retorna un Mono
				if (!file.filename().isEmpty()) {
					return file.transferTo(new File(path + p.getFoto()));
				}	
				return Mono.empty();
			})
			.then(Mono.just("redirect:/listar?success=producto+guardado+con+éxito")); // Redirige la respuesta a la vista "listar". Se puede poner solo thenReturn("redirect:/listar")
		}
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model) {
		Mono<Producto> productoMono = service.findById(id)
				.doOnNext(p -> log.info("Producto ID; " + p.getNombre()))
				.defaultIfEmpty(new Producto()); // Devuelve un producto vacio si no se ha encontrado el id de la consulta (el Mono retornado es vacío)
		model.addAttribute("titulo", "Editar producto");
		model.addAttribute("boton", "Editar");
		model.addAttribute("producto", productoMono);
		return Mono.just("form");
	}
	
	// Otra forma de editar
	// Al ejecutarse las operaciones del Model en el contexto del flujo, no se guardan en la sesión los atributos lo que puede dar lugar a que se inserten duplicados
	@GetMapping("/form-v2/{id}")
	public Mono<String> editarV2(@PathVariable String id, Model model) {
		return service.findById(id)
				.doOnNext(p -> {
					log.info("Producto ID; " + p.getNombre());
					model.addAttribute("titulo", "Editar producto");
					model.addAttribute("boton", "Editar");
					model.addAttribute("producto", p);
				})
				.defaultIfEmpty(new Producto())
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					} else {
						return Mono.just(p);
					}
				})
				.then(Mono.just("/form"))
				.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable String id, Model model) {
		return service.findById(id)
				.defaultIfEmpty(new Producto())
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					} else {
						return Mono.just(p);
					}
				})
				.flatMap(p -> {
					log.info("Producto a eliminar: " + p.getNombre());
					return service.delete(p);
				})
				.then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
				.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}
	
	// Full -> No hay contrapresión. Todo se envía de golpe
	// Si lo miramos en las herramientas de desarrollador de Chrome, vemos que su TTFB (TimeToFirstByte) es 1.2 m
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		Flux<Producto> productos = service.findAllConNombreUpperCaseConRepeat();
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
	
	// Data driver -> Se hace la contrapresión por número de elementos
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		Flux<Producto> productos = service.findAllConNombreUpperCase()
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
		Flux<Producto> productos = service.findAllConNombreUpperCaseConRepeat();
		productos.subscribe(producto -> log.info(producto.getNombre()));
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
	
}
