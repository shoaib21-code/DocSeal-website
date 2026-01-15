// src/main/java/com/example/docsign/web/SignController.java
package com.example.docsign.web;

import com.example.docsign.services.PdfSignService;
import com.example.docsign.web.dto.PdfSignRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    private final PdfSignService pdfSignService;

    public SignController(PdfSignService pdfSignService) {
        this.pdfSignService = pdfSignService;
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> signPdf(@ModelAttribute PdfSignRequest req) throws Exception {
        byte[] input = req.pdfFile().getBytes();
        byte[] signed = pdfSignService.signVisibleText(input, req.keyRef(), req.certRef(), req.hashAlgo(), req.sigAlgo());

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.setContentDisposition(ContentDisposition.attachment()
                .filename(suffix(req.pdfFile().getOriginalFilename(), "-signed.pdf"))
                .build());
        return new ResponseEntity<>(signed, h, HttpStatus.OK);
    }

    private static String suffix(String name, String add) {
        if (name == null)
            return "signed.pdf";
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return base + add;
    }
}
