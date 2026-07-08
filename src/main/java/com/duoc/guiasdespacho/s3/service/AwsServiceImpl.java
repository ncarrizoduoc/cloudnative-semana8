package com.duoc.guiasdespacho.s3.service;

import com.amazonaws.services.s3.AmazonS3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.guiasdespacho.s3.repository.S3Repository;
import com.amazonaws.util.StringUtils;
import com.duoc.guiasdespacho.exception.ResourceNotFoundException;
import com.duoc.guiasdespacho.s3.model.Asset;
import com.duoc.guiasdespacho.model.Guia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.duoc.guiasdespacho.config.CONSTANTS.*;

@Service
public class AwsServiceImpl implements AwsService {

    private final AmazonS3 s3Client;

    private final static Logger log = LoggerFactory.getLogger(AwsServiceImpl.class);

    @Autowired
    private S3Repository s3Repository;

    AwsServiceImpl(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public List<Asset> getS3Files(String bucket){
        return s3Repository.listObjectsInBucket(bucket);
    }

    public String getS3FileContent(String bucketName, String fileName) throws IOException{
        return getAsString(s3Repository.getObject(bucketName, fileName));
    }

    private static String getAsString(InputStream is) throws IOException{
        if (is == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try{
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StringUtils.UTF8));
            String line;
            while((line = reader.readLine()) != null){
                sb.append(line);
            }
        } finally {
            is.close();
        }
        return sb.toString();
    }

    @Override
    public byte[] downloadFile(String bucketName, String fileName) throws IOException{
        return s3Repository.downloadFile(bucketName, fileName);
    }

    @Override
    public void moveObject(String bucketName, String fileKey, String destinationFileKey){
        s3Repository.moveObject(bucketName, fileKey, destinationFileKey);
    }

    @Override
    public void deleteObject(String bucketName, String fileKey){
        s3Repository.deleteObject(bucketName, fileKey);
    }

    @Override
    public String uploadFile(String bucketName, String filePath, MultipartFile file){
        File fileObj = convertMultiPartFileToFile(file);
        //String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return s3Repository.uploadFile(bucketName, filePath, fileObj);
    }

    private File convertMultiPartFileToFile(MultipartFile file){
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)){
            fos.write(file.getBytes());
        } catch (IOException e){
            log.error("Error converting multipartfile to file", e);
        }
        return convertedFile;
    }

    public String subirGuia(String bucketName, Guia guia){
        String fileKey = generarFileKeyGuia(guia);
        s3Repository.subirGuia(bucketName, fileKey, guia);
        return fileKey;
    }
    
    // Metodo que genera una clave (key) de S3 para una guia de despacho
    // La clave se genera usando la fecha, transportista e ID de la guia
    private String generarFileKeyGuia(Guia guia){
        //Se formatea fecha de despacho para nombre de directorio en bucket S3
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATEFOLDERFORMAT);
        String fecha = guia.getFecha().format(formatter);

        //Se obtiene ID de transportista
        Long transportista = guia.getTransportista().getId();
        
        //ID de guia
        Long id = guia.getId();

        //Se obtiene el fileKey de la guia de despacho usando las variables anteriores
        String fileKey = String.format("%s/transportista%d/guia%d.txt", fecha, transportista, id);

        return fileKey;
    }

    public void validarBucket(String bucketName) throws ResourceNotFoundException{
        if(!s3Client.doesBucketExistV2(bucketName)){
            throw new ResourceNotFoundException("No existe el bucket con nombre: " + bucketName);
        }
    }

    // Metodo que filtra las guias del bucket S3 por fecha y/o transportista, devolviendo el listado
    // de objetos que cumplen con los filtros
    public List<Asset> filtrarGuias(String bucketName, String fecha, Long transportista) throws IOException{
        List<Asset> guias = new ArrayList<>();
        List<Asset> objetosBucket = getS3Files(bucketName);

        // Si no se ha ingresado una fecha o transportista para filtrar, se retornan todos los objetos
        if (fecha == null && transportista == null){
            return objetosBucket;
        }

        // Se crea una regexp que se usara para filtrar los objetos del bucket por su key
        // Formato expresion "^fecha*/transportista\\d+/.*";
        String regexp = "^";

        // Se agrega la fecha a la regexp (si no hay fecha se admite cualquier valor)
        if (fecha != null) {
            String[] datosFecha = fecha.trim().split("/");
            String fechaFormateada = datosFecha[2] + datosFecha[1] + datosFecha[0];
            regexp += fechaFormateada + "/";
        } else {
            regexp += ".*/";
        }

        // Se agrega el ID de transportista a la regexp (si no hay transportista, se admite cualquier valor)
        regexp += "transportista";
        if (transportista != null){
            regexp += transportista + "/.*";
        } else {
            regexp += "\\d+/.*";
        }

        // Se filtran los objetos del bucket por key usando la regexp
        for (Asset asset : objetosBucket){
            if (asset.getKey().matches(regexp)){
                guias.add(asset);
            }
        }

        return guias;
    }

    
}
