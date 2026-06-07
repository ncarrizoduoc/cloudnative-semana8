package com.duoc.guiasdespacho.controller;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.duoc.guiasdespacho.dto.GuiaRequest;
import com.duoc.guiasdespacho.model.Asset;
import com.duoc.guiasdespacho.model.Guia;
import com.duoc.guiasdespacho.service.AwsService;
import com.duoc.guiasdespacho.service.GuiaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
    public ResponseEntity<Guia> buscarGuia(@PathVariable @PositiveOrZero int id){
        return guiaService.buscarGuia(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/new")
    public ResponseEntity<Guia> registrarGuia(
                                            @RequestParam("bucketName") String bucketName,
                                            @Valid @RequestBody GuiaRequest request) throws IOException{
        Guia creado = guiaService.registrarGuia(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(creado.getId())
            .toUri();

        //Se guarda el archivo de la guia al EFS
        guiaService.escribirGuiaAEfs(creado);
        
        //Se sube la guia como archivo .txt al bucket S3
        awsService.subirGuia(bucketName, creado);

        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guia> modificarGuia(
                                            @RequestParam String bucketName,
                                            @PathVariable @PositiveOrZero int id, 
                                            @Valid @RequestBody GuiaRequest request){
        
        //Se valida que el bucket S3 exista
        awsService.validarBucket(bucketName);
        //Se crea una copia de la guia de despacho original
        Optional<Guia> guiaBuscar = guiaService.buscarGuia(id);
        Guia guiaOriginal = new Guia();
        if (!guiaBuscar.isEmpty()){
            guiaOriginal = SerializationUtils.clone(guiaBuscar.get());
        }

        //Se modifica guia en base de datos
        Optional<Guia> response = guiaService.modificarGuia(id, request);
        if (!response.isEmpty()){
            Guia guia = response.get();
            //Se elimina la guia anterior del bucket S3
            awsService.eliminarGuia(bucketName, guiaOriginal);
            //Se sube la nueva guia al bucket S3
            awsService.subirGuia(bucketName, guia);
        }
        
        return response
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
        
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@RequestParam String bucketName, @PathVariable @PositiveOrZero int id){
        //Se valida que el bucket exista
        awsService.validarBucket(bucketName);
        //Se elimina la guia del bucket S3
        Optional<Guia> guiaBuscar = guiaService.buscarGuia(id);
        if (!guiaBuscar.isEmpty()){
            Guia guia = guiaBuscar.get();
            awsService.eliminarGuia(bucketName, guia);
        }

        //Se elimina la guia de la base de datos (si existe) y se envia respuesta
        return guiaService.eliminarGuia(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    @GetMapping("/filtrarGuias")
    public ResponseEntity<List<Asset>> filtrarGuias(
            @RequestParam String bucketName,
            @RequestParam @Pattern(regexp="[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]", message = "La fecha debe tener el formato dd/MM/yyy") String fecha, 
            @RequestParam int transportista) throws IOException{
        List<Guia> guias = guiaService.filtrarGuias(fecha, transportista);
        List<Asset> lista = new ArrayList<>();
        for (Guia guia : guias){
            lista.add(awsService.buscarGuia(bucketName, guia));
        }
        return ResponseEntity.ok(lista);
    }

}
