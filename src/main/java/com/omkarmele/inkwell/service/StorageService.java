package com.omkarmele.inkwell.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.dto.Dtos.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cloudinary storage for images. The client-supplied file name and Content-Type are never trusted:
 * the type is detected from the file's magic bytes and the stored name is a random UUID.
 */
@Service
@RequiredArgsConstructor
public class StorageService {
    @Value("${app.cloudinary.url:}")
    private String cloudinaryUrl;

    public UploadResponse storeImage(MultipartFile file) {
        if (file.isEmpty()) throw ApiException.badRequest("File is empty");
        if (!StringUtils.hasText(cloudinaryUrl)) {
            throw new IllegalStateException("Cloudinary is not configured; set CLOUDINARY_URL");
        }
        try {
            byte[] bytes = file.getBytes();
            String fileExtension = detectExtension(bytes);
            if (fileExtension == null) throw ApiException.badRequest("Only JPEG, PNG, GIF and WebP images are allowed");
            Map<?, ?> result = new Cloudinary(cloudinaryUrl).uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "inkwell",
                    "public_id", UUID.randomUUID().toString(),
                    "format", fileExtension));
            Object secureUrl = result.get("secure_url");
            if (!(secureUrl instanceof String url) || !StringUtils.hasText(url)) {
                throw new IllegalStateException("Cloudinary upload response did not include a secure URL");
            }
            return new UploadResponse(url);
        } catch (IOException ioException) {
            throw new IllegalStateException("Could not upload image to Cloudinary", ioException);
        }
    }

    private static String detectExtension(byte[] fileBytes) {
        if (fileBytes.length < 12) return null;
        if ((fileBytes[0] & 0xFF) == 0xFF && (fileBytes[1] & 0xFF) == 0xD8 && (fileBytes[2] & 0xFF) == 0xFF) return "jpg";
        if ((fileBytes[0] & 0xFF) == 0x89 && fileBytes[1] == 'P' && fileBytes[2] == 'N' && fileBytes[3] == 'G') return "png";
        if (fileBytes[0] == 'G' && fileBytes[1] == 'I' && fileBytes[2] == 'F' && fileBytes[3] == '8') return "gif";
        if (fileBytes[0] == 'R' && fileBytes[1] == 'I' && fileBytes[2] == 'F' && fileBytes[3] == 'F'
                && fileBytes[8] == 'W' && fileBytes[9] == 'E' && fileBytes[10] == 'B' && fileBytes[11] == 'P') return "webp";
        return null;   // SVG is rejected on purpose (it can carry scripts)
    }
}
