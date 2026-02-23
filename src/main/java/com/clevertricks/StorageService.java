package com.clevertricks;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

public interface StorageService {
    void init();

    void store(MultipartFile file);

    Stream<String> loadAll();

    Resource loadAsResource(String filename);

    void deleteAll();
}
