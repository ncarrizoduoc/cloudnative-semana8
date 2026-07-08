package com.duoc.guiasdespacho.s3.repository;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.duoc.guiasdespacho.s3.model.Asset;
import com.duoc.guiasdespacho.model.Guia;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface S3Repository {

    List<Asset> listObjectsInBucket(String bucket);

    S3ObjectInputStream getObject(String bucketName, String fileName) throws IOException;

    Asset getObjectInfo(String bucketName, String fileKey) throws IOException;

    byte[] downloadFile(String bucketname, String fileName) throws IOException;

    void moveObject(String bucketName, String fileKey, String destinationFileKey);

    void deleteObject(String bucketName, String fileKey);

    String uploadFile(String bucketName, String fileKey, File fileObj);

    void subirGuia(String bucketName, String fileKey, Guia guia);


}
