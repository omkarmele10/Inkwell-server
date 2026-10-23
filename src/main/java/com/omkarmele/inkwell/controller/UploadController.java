package com.omkarmele.inkwell.controller;

import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.dto.Dtos.UploadResponse;
import com.omkarmele.inkwell.security.AuthUtil;
import com.omkarmele.inkwell.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        AuthUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(storageService.storeImage(file));
    }
}
