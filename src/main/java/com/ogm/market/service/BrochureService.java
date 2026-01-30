package com.ogm.market.service;

import com.ogm.market.dto.BrochureRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class BrochureService {

    // Check if brochure exists in classpath
    public boolean brochureExists(String filename) {
        try {
            ClassPathResource resource =
                    new ClassPathResource("static/brochures/" + filename);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }

    // Return InputStream for download
    public InputStream getBrochureStream(String filename) throws IOException {
        ClassPathResource resource =
                new ClassPathResource("static/brochures/" + filename);

        if (!resource.exists()) {
            throw new IOException("Brochure not found");
        }

        return resource.getInputStream();
    }

    // Save lead / request (optional)
    public void recordRequest(BrochureRequest req) {
        System.out.println("Brochure request saved: " + req.getMobile());
    }
}
