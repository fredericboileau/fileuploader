package com.clevertricks;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Stream;
import java.util.Map;

public interface StorageService {
    void init();

    void store(MultipartFile file, String owner);

    Stream<String> loadAll(String owner);

    Resource loadAsResource(String filename, String owner);

    void deleteAll();

    void delete(String filename, String owner);

    Stream<String> listOwners();

    void shareFileWith(String filename, String owner, List<String> userIds);

    void shareFilesWithUser(List<String> filenames, String owner, String userId);

    Map<String, List<String>> listShares(String userId);

    void deleteAllForOwner(String owner);

    default String lookupUsername(String userId) {
        return userId;
    }

    default long getTotalSize(String userId) {
        return 0L;
    }

    default long getMaxFilesSize() {
        return 0L;
    }
}
