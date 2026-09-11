package com.coreib.server.api;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

interface CoreIbFileStorage {
    List<StoredFile> files();
    StoredFile store(MultipartFile file, String uploaderId) throws IOException;
    FileContent content(String id);
    void delete(String id) throws IOException;

    record StoredFile(String id, String originalName, String contentType, long sizeBytes,
                      String uploaderId, Instant createdAt) { }
    record FileContent(StoredFile metadata, Resource resource) { }
}
