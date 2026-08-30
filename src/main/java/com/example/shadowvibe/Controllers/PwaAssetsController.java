package com.example.shadowvibe.Controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PwaAssetsController {

    @GetMapping(value = "/sw.js", produces = "application/javascript")
    public ResponseEntity<Resource> serviceWorker() {
        return ResponseEntity.ok(new ClassPathResource("static/sw.js"));
    }

    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    public ResponseEntity<Resource> manifest() {
        return ResponseEntity.ok(new ClassPathResource("static/manifest.webmanifest"));
    }
}
