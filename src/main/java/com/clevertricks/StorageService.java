package com.clevertricks;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.stream.Stream;

public interface StorageService {
    void init();

    void store(MultipartFile file, String owner);

    Stream<String> loadAll(String owner);

    Resource loadAsResource(String filename, String owner);

    void deleteAll();

    void delete(String filename, String owner);
}
