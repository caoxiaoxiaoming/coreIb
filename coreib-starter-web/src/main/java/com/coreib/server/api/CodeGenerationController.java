package com.coreib.server.api;

import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.ResourcePermission;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/code-generation")
public class CodeGenerationController {
    public static final String RESOURCE = "code-generation";
    private final CurrentPermissionProvider permissions;
    private final CoreIbCodeGenerator generator;
    public CodeGenerationController(CurrentPermissionProvider permissions, CoreIbCodeGenerator generator) { this.permissions=permissions; this.generator=generator; }

    @GetMapping("/tables") public List<CoreIbCodeGenerator.TableSummary> tables() { require(PermissionAction.READ); return generator.tables(); }
    @GetMapping("/preview") public CoreIbCodeGenerator.CodePreview preview(@RequestParam String tableName, @RequestParam(required=false) String moduleName) { require(PermissionAction.READ); return generator.preview(tableName,moduleName); }
    @GetMapping("/download") public ResponseEntity<byte[]> download(@RequestParam String tableName, @RequestParam(required=false) String moduleName) {
        require(PermissionAction.EXPORT);
        String name = (moduleName == null || moduleName.isBlank() ? tableName : moduleName) + ".zip";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(generator.archive(tableName,moduleName));
    }
    private void require(PermissionAction action) {
        ResourcePermission permission=permissions.current().resources().get(RESOURCE);
        if(permission==null||!permission.actions().contains(action)) throw new PermissionDeniedException(RESOURCE,action,"resource-not-granted");
    }
}
