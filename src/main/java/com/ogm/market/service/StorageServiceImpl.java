package com.ogm.market.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${storage.location:uploads}")
    private String storageLocation;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storageLocation));
    }

    @Override
    public String store(MultipartFile file, String subfolder) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int i = filename.lastIndexOf('.');
        if (i >= 0) ext = filename.substring(i);
        String storedName = UUID.randomUUID() + ext;
        Path dir = Paths.get(storageLocation).resolve(subfolder != null ? subfolder : "");
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // return path relative to storageLocation root, e.g. /uploads/properties/uuid.jpg
            return "/" + dir.resolve(storedName).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public List<String> storeMultiple(List<MultipartFile> files, String subfolder) {
        return files.stream().map(f -> store(f, subfolder)).collect(Collectors.toList());
    }

    @Override
    public void delete(String path) {
        try {
            Path p = Paths.get(path.startsWith("/") ? path.substring(1) : path);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            // ignore
        }
    }
}
