package com.ogm.market.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    String store(MultipartFile file, String subfolder);
    List<String> storeMultiple(List<MultipartFile> files, String subfolder);
    void delete(String path);
}
