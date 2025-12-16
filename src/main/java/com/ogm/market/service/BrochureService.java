package com.ogm.market.service;

import com.ogm.market.dto.BrochureRequest;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BrochureService {

    // Folder where brochure files are stored
    private final Path brochureDir = Paths.get("src/main/resources/static/brochures");

    // Check if brochure exists
    public boolean brochureExists(String filename) {
        return Files.exists(brochureDir.resolve(filename));
    }

    // Return full brochure path for download
    public Path getBrochurePath(String filename) {
        return brochureDir.resolve(filename);
    }

    // Save lead / request (optional)
    public void recordRequest(BrochureRequest req) {
        // TODO: save this to DB later
        System.out.println("Brochure request saved: " + req.getMobile());
    }
}
