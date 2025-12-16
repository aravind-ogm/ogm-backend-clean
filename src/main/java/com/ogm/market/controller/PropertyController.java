package com.ogm.market.controller;

import com.ogm.market.dto.BrochureRequest;
import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.model.ContactForm;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import com.ogm.market.service.BrochureService;
import com.ogm.market.service.EmailService;
import com.ogm.market.service.PropertyService;
import com.ogm.market.service.StorageService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RequestMapping("/api")
class PropertyController {

    private static final Logger log = LoggerFactory.getLogger(PropertyController.class);

    private final BrochureService brochureService;
    private final EmailService emailService;
    private final PropertyService propertyService;
    private final StorageService storageService;

    @Autowired
    private PropertyRepository propertyRepository;

    public PropertyController(
            BrochureService brochureService,
            EmailService emailService,
            PropertyService propertyService,
            StorageService storageService
    ) {
        this.brochureService = brochureService;
        this.emailService = emailService;
        this.propertyService = propertyService;
        this.storageService = storageService;
    }

    // ======================================================================
//                           BROCHURE ENDPOINTS
// ======================================================================
    @PostMapping("/brochure/request")
    public ResponseEntity<?> requestBrochure(@RequestBody BrochureRequest req) {

        log.info("Received brochure request: name={}, mobile={}, email={}, propertyId={}",
                req.getName(), req.getMobile(), req.getEmail(), req.getPropertyId());

        // ------------------ VALIDATION ------------------
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name required"));
        }

        if (req.getMobile() == null || req.getMobile().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mobile required"));
        }

        // ------------------ FETCH PROPERTY ------------------
        Property property = propertyRepository.findById(req.getPropertyId())
                .orElse(null);

        if (property == null) {
            log.warn("Brochure request failed — Property not found: {}", req.getPropertyId());
            return ResponseEntity.badRequest().body(Map.of("error", "Property not found"));
        }

        String brochureFile = property.getBrochureFile(); // <--- from DB

        if (brochureFile == null || brochureFile.isBlank()) {
            log.warn("Brochure missing for property {}", property.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Brochure not available"));
        }

        // ------------------ CHECK FILE EXISTS ------------------
        if (!brochureService.brochureExists(brochureFile)) {
            log.error("Brochure file not found on server: {}", brochureFile);
            return ResponseEntity.status(404).body(Map.of("error", "Brochure file missing on server"));
        }

        brochureService.recordRequest(req);

        // Return API download endpoint URL
        String downloadUrl = "/api/brochure/download?file=" + brochureFile;

        log.info("Brochure ready for download: {}", downloadUrl);

        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }

    // ======================================================================
//                        DOWNLOAD BROCHURE (Dynamic)
// ======================================================================
    @GetMapping("/brochure/download")
    public ResponseEntity<?> downloadBrochure(@RequestParam("file") String file) {

        log.info("Brochure download requested: {}", file);

        if (file == null || file.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file name"));
        }

        Path path = brochureService.getBrochurePath(file);

        if (!Files.exists(path)) {
            log.error("Brochure not found on disk: {}", path);
            return ResponseEntity.status(404).body(Map.of("error", "File not found"));
        }

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + file);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(Files.size(path))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to stream brochure file {}: {}", file, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Server error while downloading"));
        }
    }


    // ======================================================================
    //                           CONTACT ENDPOINTS
    // ======================================================================
    @PostMapping("/contact/send")
    public ResponseEntity<String> sendMessage(@RequestBody ContactForm form) {
        log.info("Contact form submission received: name={}, mobile={}, email={}",
                form.getName(), form.getMobile(), form.getEmail());

        try {
            emailService.sendContactEmail(form);
            log.info("Contact email sent successfully to admin for {}", form.getEmail());
            return ResponseEntity.ok("Email sent successfully");
        } catch (MessagingException ex) {
            log.error("Failed to send contact email: {}", ex.getMessage());
            return ResponseEntity.status(500)
                    .body("Failed to send email: " + ex.getMessage());
        }
    }

    // ======================================================================
    //                        PROPERTY CRUD & SEARCH
    // ======================================================================
    @GetMapping("/properties")
    public Page<PropertyResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minSqft,
            @RequestParam(required = false) Integer maxSqft,
            @RequestParam(required = false) Boolean rera,
            @RequestParam(required = false) Integer bhk,
            @RequestParam(required = false) String facing,
            @RequestParam(required = false) String furnishing,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {

        log.info("Property search request: q={}, type={}, minPrice={}, maxPrice={}, bhk={}",
                q, type, minPrice, maxPrice, bhk);

        Pageable pageable = PageRequest.of(page, size);

        return propertyService.listProperties(
                q, type, minPrice, maxPrice,
                minSqft, maxSqft, rera, bhk, facing, furnishing,
                pageable
        );
    }

    @GetMapping("/properties/{id}")
    public PropertyResponse getProperty(@PathVariable Long id) {
        log.info("Fetching property details for ID={}", id);
        return propertyService.getProperty(id);
    }

    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> createProperty(@RequestBody PropertyRequest request) {
        log.info("Creating new property: title={}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.createProperty(request));
    }

    @PutMapping("/properties/{id}")
    public PropertyResponse updateProperty(@PathVariable Long id,
                                           @RequestBody PropertyRequest request) {
        log.info("Updating property ID={}", id);
        return propertyService.updateProperty(id, request);
    }

    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        log.warn("Deleting property ID={}", id);
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }

    // ======================================================================
    //                      PROPERTY IMAGES & VIDEO UPLOAD
    // ======================================================================
    @PostMapping("/properties/{id}/upload-image")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading single image for property ID={} | file={}", id, file.getOriginalFilename());
        String path = storageService.store(file, "properties/" + id);
        return ResponseEntity.ok(path);
    }

    @PostMapping("/properties/{id}/upload-images")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files
    ) {
        log.info("Uploading {} images for property ID={}", files.size(), id);
        return ResponseEntity.ok(storageService.storeMultiple(files, "properties/" + id));
    }

    @PostMapping("/properties/{id}/upload-video")
    public ResponseEntity<String> uploadVideo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading video for property ID={} | file={}", id, file.getOriginalFilename());
        String path = storageService.store(file, "properties/" + id + "/video");
        return ResponseEntity.ok(path);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        if (body.get("username").equals("admin") &&
                body.get("password").equals("admin123")) {

            session.setAttribute("loggedIn", true);
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
