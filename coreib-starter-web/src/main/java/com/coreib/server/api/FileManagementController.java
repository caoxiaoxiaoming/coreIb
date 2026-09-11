package com.coreib.server.api;

import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.ResourcePermission;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileManagementController {
    public static final String RESOURCE = "file-management";
    private final CurrentPermissionProvider permissions;
    private final CoreIbFileStorage storage;

    public FileManagementController(CurrentPermissionProvider permissions, CoreIbFileStorage storage) {
        this.permissions = permissions; this.storage = storage;
    }

    @GetMapping public List<CoreIbFileStorage.StoredFile> files() { require(PermissionAction.READ); return storage.files(); }
    @PostMapping public CoreIbFileStorage.StoredFile upload(@RequestParam("file") MultipartFile file) throws IOException {
        require(PermissionAction.CREATE); return storage.store(file, permissions.current().subjectId());
    }
    @GetMapping("/{id}/content") public ResponseEntity<org.springframework.core.io.Resource> content(@PathVariable String id) {
        require(PermissionAction.READ);
        CoreIbFileStorage.FileContent content = storage.content(id);
        String type = content.metadata().contentType();
        MediaType mediaType;
        try { mediaType = type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(content.metadata().originalName(), StandardCharsets.UTF_8).build().toString())
                .body(content.resource());
    }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) throws IOException { require(PermissionAction.DELETE); storage.delete(id); }

    private void require(PermissionAction action) {
        ResourcePermission permission = permissions.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(action)) throw new PermissionDeniedException(RESOURCE, action, "resource-not-granted");
    }
}
