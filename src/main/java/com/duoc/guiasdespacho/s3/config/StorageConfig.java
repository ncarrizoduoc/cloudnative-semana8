package com.duoc.guiasdespacho.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class StorageConfig {

    @Value("#{environment.AWS_ACCESS_KEY_ID}")
    private String accessKey;

    @Value("#{environment.AWS_SECRET_ACCESS_KEY}")
    private String secretAccessKey;

    @Value("#{environment.AWS_SESSION_TOKEN}")
    private String sessionToken;

    @Value("#{environment.REGION}")
    private String region;

    @Bean
    public AmazonS3 s3Client(){
        AWSCredentials credentials = new BasicSessionCredentials(accessKey, secretAccessKey, sessionToken);
        return AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion(region).build();
    }

    

}
