package com.duoc.guiasdespacho.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.duoc.guiasdespacho.dto.GuiaRequest;
import com.duoc.guiasdespacho.model.Guia;
import com.duoc.guiasdespacho.s3.service.AwsService;
import com.duoc.guiasdespacho.service.GuiaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {
    @Autowired
    private GuiaService guiaService;

    @Autowired
    AwsService awsService;

    @GetMapping
    public ResponseEntity<List<Guia>> listarGuias(){
        return ResponseEntity.ok(guiaService.listarGuias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guia> buscarGuia(@PathVariable @PositiveOrZero Long id){
        return guiaService.buscarGuia(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Guia> registrarGuia(@Valid @RequestBody GuiaRequest request){
        //Creacion de la guia en base de datos
        Guia creado = guiaService.registrarGuia(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(creado.getId())
            .toUri();

        return ResponseEntity.created(location).body(creado);
    }

    @PostMapping("/download")
    public ResponseEntity<ByteArrayResource> registrarYDescargarGuia(
        @Valid @RequestBody GuiaRequest request
    ){
        // Se guarda la guia y se transforma la guia retornada por el servicio a Bytes
        Guia creado = guiaService.registrarGuia(request);
        byte[] data = creado.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(data);

        //Generar respuesta con archivo descargable
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(creado.getId())
            .toUri();
        return ResponseEntity
            .created(location)
            .contentLength(data.length)
            .contentType(MediaType.TEXT_PLAIN)
            .header("Content-Disposition", "attachment; filename\"" + creado.getId() + "\"")
            .body(resource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guia> modificarGuia(
                                            @PathVariable @PositiveOrZero Long id, 
                                            @Valid @RequestBody GuiaRequest request){
        
        //Se modifica guia en base de datos
        Optional<Guia> response = guiaService.modificarGuia(id, request);
        return response
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
        
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable @PositiveOrZero Long id){
        //Se elimina la guia de la base de datos (si existe) y se envia respuesta
        return guiaService.eliminarGuia(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

}
