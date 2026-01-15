// src/main/java/com/example/docsign/web/AdminController.java
package com.example.docsign.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.example.docsign.services.AdminPurgeService;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
public class AdminController {

  private final AdminPurgeService purge;

  public AdminController(AdminPurgeService purge) {
    this.purge = purge;
  }

  @DeleteMapping(value = "/purge", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> purgeAll(@RequestParam(defaultValue = "false") boolean includeSystem) throws Exception {
    var r = purge.purgeAll(includeSystem);
    return Map.of(
        "deletedCsrs", r.deletedCsrs(),
        "deletedCerts", r.deletedCerts(),
        "deletedKeys", r.deletedKeys(),
        "deletedPaths", r.deletedPaths()
    );
  }
}
