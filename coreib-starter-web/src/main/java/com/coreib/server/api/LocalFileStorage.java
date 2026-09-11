package com.coreib.server.api;

import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Local binary storage with portable JDBC metadata when a datasource is active. */
final class LocalFileStorage implements CoreIbFileStorage {
    private final Path root;
    private final JdbcTemplate jdbc;
    private final Map<String, StoredRecord> memory = new LinkedHashMap<>();

    LocalFileStorage(Path root, JdbcTemplate jdbc) {
        this.root = root.toAbsolutePath().normalize();
        this.jdbc = jdbc;
        try { Files.createDirectories(this.root); }
        catch (IOException exception) { throw new IllegalStateException("Unable to initialize file storage", exception); }
    }

    @Override public synchronized List<StoredFile> files() {
        if (jdbc != null) {
            return jdbc.query("SELECT id,original_name,content_type,size_bytes,uploader_id,created_at FROM coreib_file_object ORDER BY created_at DESC",
                    (r,n) -> new StoredFile(r.getString(1),r.getString(2),r.getString(3),r.getLong(4),r.getString(5),r.getTimestamp(6).toInstant()));
        }
        return memory.values().stream().map(StoredRecord::metadata).sorted(Comparator.comparing(StoredFile::createdAt).reversed()).toList();
    }

    @Override public synchronized StoredFile store(MultipartFile file, String uploaderId) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File must not be empty");
        String id = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String storageName = id + extension(originalName);
        Path target = resolveStorageName(storageName);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        StoredFile metadata = new StoredFile(id, originalName, file.getContentType(), file.getSize(), uploaderId, Instant.now());
        try {
            if (jdbc != null) jdbc.update("INSERT INTO coreib_file_object (id,original_name,storage_name,content_type,size_bytes,uploader_id,created_at) VALUES (?,?,?,?,?,?,?)",
                    id,originalName,storageName,file.getContentType(),file.getSize(),uploaderId,Timestamp.from(metadata.createdAt()));
            else memory.put(id, new StoredRecord(metadata, storageName));
        } catch (RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return metadata;
    }

    @Override public synchronized FileContent content(String id) {
        StoredRecord record = find(id);
        Path path = resolveStorageName(record.storageName());
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Stored file content not found: " + id);
        return new FileContent(record.metadata(), new FileSystemResource(path));
    }

    @Override public synchronized void delete(String id) throws IOException {
        StoredRecord record = find(id);
        if (jdbc != null) jdbc.update("DELETE FROM coreib_file_object WHERE id=?", id); else memory.remove(id);
        Files.deleteIfExists(resolveStorageName(record.storageName()));
    }

    private StoredRecord find(String id) {
        if (jdbc == null) {
            StoredRecord result = memory.get(id);
            if (result == null) throw new IllegalArgumentException("Unknown file: " + id);
            return result;
        }
        List<StoredRecord> results = jdbc.query("SELECT original_name,storage_name,content_type,size_bytes,uploader_id,created_at FROM coreib_file_object WHERE id=?",
                (r,n) -> new StoredRecord(new StoredFile(id,r.getString(1),r.getString(3),r.getLong(4),r.getString(5),r.getTimestamp(6).toInstant()),r.getString(2)), id);
        if (results.isEmpty()) throw new IllegalArgumentException("Unknown file: " + id);
        return results.get(0);
    }

    private Path resolveStorageName(String storageName) {
        Path result = root.resolve(storageName).normalize();
        if (!result.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return result;
    }
    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) return "";
        String value = name.substring(index).replaceAll("[^A-Za-z0-9.]", "");
        return value.length() > 16 ? "" : value;
    }
    private record StoredRecord(StoredFile metadata, String storageName) { }
}
