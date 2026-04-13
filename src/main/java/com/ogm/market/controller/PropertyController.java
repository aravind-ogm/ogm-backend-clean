package com.ogm.market.controller;

import com.ogm.market.ai.EmbeddingService;
import com.ogm.market.dto.BrochureRequest;
import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.exception.ResourceNotFoundException;
import com.ogm.market.model.ContactForm;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import com.ogm.market.service.BrochureService;
import com.ogm.market.auth.EmailService;
import com.ogm.market.service.PropertyService;
import com.ogm.market.service.StorageService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class PropertyController {

    private static final Logger log = LoggerFactory.getLogger(PropertyController.class);

    private final BrochureService brochureService;
    private final EmailService emailService;
    private final PropertyService propertyService;
    private final StorageService storageService;
    private final PropertyRepository propertyRepository;
    private final String backendUrl;

    public PropertyController(
            BrochureService brochureService,
            EmailService emailService,
            PropertyService propertyService,
            StorageService storageService,
            PropertyRepository propertyRepository,
            @Value("${backend.url:}") String backendUrl) {
        this.brochureService = brochureService;
        this.emailService = emailService;
        this.propertyService = propertyService;
        this.storageService = storageService;
        this.propertyRepository = propertyRepository;
        this.backendUrl = backendUrl;
    }


    @PostMapping("/brochure/request")
    public ResponseEntity<?> requestBrochure(@RequestBody BrochureRequest req) {
        log.info("Brochure request for property {}", req.getPropertyId());

        if (req.getName() == null || req.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Name required"));
        if (req.getMobile() == null || req.getMobile().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Mobile required"));

        Property property = propertyRepository.findById(req.getPropertyId()).orElse(null);
        if (property == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Property not found"));

        String brochureFile = property.getBrochureFile();
        if (brochureFile == null || brochureFile.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Brochure not available"));

        if (!brochureService.brochureExists(brochureFile))
            return ResponseEntity.status(404).body(Map.of("error", "Brochure missing on server"));

        brochureService.recordRequest(req);
        String downloadUrl = backendUrl + "/api/brochure/download?file=" + brochureFile;
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }

    @GetMapping("/brochure/download")
    public ResponseEntity<?> downloadBrochure(@RequestParam("file") String file) {
        try {
            InputStreamResource resource =
                    new InputStreamResource(brochureService.getBrochureStream(file));
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + file + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            log.error("Brochure download failed: {}", file);
            return ResponseEntity.status(404).body(Map.of("error", "Brochure not found"));
        }
    }

    @PostMapping("/contact/send")
    public ResponseEntity<String> sendMessage(@RequestBody ContactForm form) {
        try {
            emailService.sendContactEmail(form);
            return ResponseEntity.ok("Email sent successfully");
        } catch (MessagingException ex) {
            log.error("Contact email failed: {}", ex.getMessage());
            return ResponseEntity.status(500).body("Failed to send email");
        }
    }

    @GetMapping("/properties")
    public Page<PropertyResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean rera,
            @RequestParam(required = false) Integer bhk,
            @RequestParam(required = false) String facing,
            @RequestParam(required = false) String furnishing,
            Pageable pageable) {
        return propertyService.listProperties(q, type, minPrice, maxPrice,
                rera, bhk, facing, furnishing, pageable);
    }

    @GetMapping("/properties/{id}")
    public PropertyResponse getProperty(@PathVariable Long id) {
        return propertyService.getProperty(id);
    }

    @GetMapping("/properties/slug/{slug}")
    public ResponseEntity<PropertyResponse> getPropertyBySlug(@PathVariable String slug) {
        return propertyRepository.findBySlugAndActiveTrue(slug)
                .map(p -> ResponseEntity.ok(propertyService.toResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> createProperty(@RequestBody PropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.createProperty(request));
    }

    @PutMapping("/properties/{id}")
    public PropertyResponse updateProperty(@PathVariable Long id,
                                           @RequestBody PropertyRequest request) {
        return propertyService.updateProperty(id, request);
    }

    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/properties/{id}/upload-image")
    public ResponseEntity<String> uploadImage(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storageService.store(file, "properties/" + id));
    }

    @PostMapping("/properties/{id}/upload-images")
    public ResponseEntity<List<String>> uploadImages(@PathVariable Long id,
                                                     @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(storageService.storeMultiple(files, "properties/" + id));
    }

    @PostMapping("/properties/{id}/upload-video")
    public ResponseEntity<String> uploadVideo(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storageService.store(file, "properties/" + id + "/video"));
    }

    @PatchMapping("/properties/{id}/visibility")
    public ResponseEntity<Map<String, Object>> setVisibility(
            @PathVariable Long id,
            @RequestParam boolean active) {

        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found: " + id));

        p.setActive(active);
        propertyRepository.save(p);

        log.info("Property id={} visibility set to active={}", id, active);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "active", active,
                "title", p.getTitle()
        ));
    }
}