package com.skhu.mid_skhu.app.service.event;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.skhu.mid_skhu.global.exception.ErrorCode;
import com.skhu.mid_skhu.global.exception.model.CustomException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class S3ImageFileServiceImpl implements S3ImageFileService{

    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadImageFile(MultipartFile imageFile, String directory) throws IOException {
        String imageFileName = createImageFileName(imageFile.getOriginalFilename());
        String imageFileUrl = "https://" + bucket + "/" + directory + "/" + imageFileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(imageFile.getContentType());
        metadata.setContentLength(imageFile.getSize());

        amazonS3Client.putObject(bucket, directory + "/" + imageFileName, imageFile.getInputStream(), metadata);

        return imageFileUrl;
    }

    @Override
    public List<String> uploadImageFiles(List<MultipartFile> imageFiles, String directory) {
        List<String> imageFileUrls;

        imageFileUrls = imageFiles.stream()
                .filter(multipartFile -> !multipartFile.isEmpty())
                .map(multipartFile -> {
                    try {
                        return uploadImageFile(multipartFile, directory);
                    } catch (IOException e) {
                        throw new CustomException(ErrorCode.FAILED_UPLOAD_IMAGE_FILE_EXCEPTION,
                                ErrorCode.FAILED_UPLOAD_IMAGE_FILE_EXCEPTION.getMessage());
                    }
                })
                .collect(Collectors.toList());

        return imageFileUrls;
    }

    private String createImageFileName(String imageFileName) {
        String encodedImageFileName;
        try {
            encodedImageFileName = URLEncoder.encode(imageFileName, StandardCharsets.UTF_8.toString());
            return encodedImageFileName.replaceAll("/", "");
        } catch (UnsupportedEncodingException e) {
            throw new CustomException(ErrorCode.FAIL_ENCODING_IMAGE_FILE_NAME,
                    ErrorCode.FAIL_ENCODING_IMAGE_FILE_NAME.getMessage());
        }
    }
}