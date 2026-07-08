package com.duoc.guiasdespacho.s3.controller;

import static com.duoc.guiasdespacho.config.CONSTANTS.DATEFORMAT;
import static com.duoc.guiasdespacho.config.CONSTANTS.DATEREGEXP;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;

import com.duoc.guiasdespacho.s3.service.AwsServiceImpl;

import jakarta.validation.constraints.Pattern;

import com.duoc.guiasdespacho.s3.model.Asset;

@RestController
@RequestMapping("/s3")
public class AwsController {

    @Autowired
    private AwsServiceImpl awsService;

    @GetMapping("/getS3FileContent")
    public ResponseEntity<String> getS3FileContent(@RequestParam(value = "bucketName") String bucketName, @RequestParam(value = "fileKey") String fileKey) throws IOException{
        return new ResponseEntity<>(awsService.getS3FileContent(bucketName, fileKey), HttpStatus.OK);
    }

    @GetMapping("/listS3Files")
    public ResponseEntity<List<Asset>> getS3Files(@RequestParam(value = "bucketName") String bucketName) throws IOException{
        List<Asset> list = new ArrayList<>();
        HttpStatus status = HttpStatus.OK;
        try {
            list = awsService.getS3Files(bucketName);
        } catch (Exception e){
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(list, status);
    }

    @GetMapping("/downloadS3File")
    public ResponseEntity<ByteArrayResource> downloadS3File(@RequestParam(value = "bucketName") String bucketName,
                                                            @RequestParam(value = "fileKey") String fileKey) throws IOException{
        byte[] data = awsService.downloadFile(bucketName, fileKey);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity
            .ok()
            .contentLength(data.length)
            .header("Content-type", "application/octet-stream")
            .header("Content-disposition", "attachment; filename=\"" + fileKey + "\"")
            .body(resource);
    }

    @DeleteMapping("/deleteObject")
    public ResponseEntity<String> deleteFile(@RequestParam(value = "bucketName") String bucketName, @RequestParam(value = "fileKey") String fileKey){
        awsService.deleteObject(bucketName, fileKey);
        return new ResponseEntity<>("File deleted", HttpStatus.OK);
    }

    @PutMapping("/moveFile")
    public ResponseEntity<String> moveFile(@RequestParam(value = "bucketName") String bucketName,
                                            @RequestParam(value = "fileKey") String fileKey,
                                            @RequestParam(value = "fileKeyDest") String fileKeyDest){
        awsService.moveObject(bucketName, fileKey, fileKeyDest);
        return new ResponseEntity<>("File moved", HttpStatus.OK);
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<String> uploadFile(@RequestParam(value = "bucketName") String bucketName,
                                            @RequestParam(value = "fileKey") String fileKey,
                                            @RequestParam(value = "file") MultipartFile file){
        return new ResponseEntity<>(awsService.uploadFile(bucketName, fileKey, file), HttpStatus.OK);
    }
    
    @GetMapping("/filtrarGuias")
    public ResponseEntity<List<Asset>> filtrarGuias(
            @RequestParam String bucketName,
            @RequestParam(required = false) @Pattern(regexp=DATEREGEXP, message = "La fecha debe tener el formato " + DATEFORMAT) String fecha, 
            @RequestParam (required = false) Long transportista) throws IOException{
        
        List<Asset> lista = awsService.filtrarGuias(bucketName, fecha, transportista);
        return ResponseEntity.ok(lista);
    }

}
