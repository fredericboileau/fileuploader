package com.clevertricks;

import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileSystemUtils;

public class PostgresqlFileSystemStorageService implements StorageService {

    private final String baseUrl = "jdbc:postgresql://";
    private final String host;
    private final int port;
    private final String db;
    private final String dbUrl;

    private final Connection conn;

    private final Path rootLocation;

    @Autowired
    PostgresqlFileSystemStorageService(StorageProperties properties) {
        this.host = properties.getHost();
        this.port = properties.getPort();
        this.db = properties.getDb();
        this.dbUrl = baseUrl + host + ":" + String.valueOf(port) + "/" + db;

        if (properties.getLocation().trim().length() == 0) {
            throw new StorageException("File upload location cannot be Empty");
        }
        this.rootLocation = Paths.get(properties.getLocation());

        try {
            this.conn = DriverManager.getConnection(dbUrl, properties.getUsername(), properties.getPassword());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void store(MultipartFile file, String owner) {
        var sql = "insert into filepaths (name, owner) values (?,?)";
        if (file.isEmpty()) {
            throw new StorageFileEmptyException("Cannot store empty file");
        }
        try {
            Files.createDirectories(Paths.get(rootLocation.toString(), owner));
            var originalFilename = file.getOriginalFilename();
            if (!Paths.get(originalFilename).getFileName().toString().equals(originalFilename)) {
                throw new StorageException("Filename cannot contain path separators");
            }
            var destinationFile = Paths.get(rootLocation.toString(), owner, originalFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile);
            }
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, file.getOriginalFilename());
                stmt.setObject(2, UUID.fromString(owner));
                stmt.executeUpdate();
            } catch (SQLException e) {
                Files.deleteIfExists(destinationFile);
                if ("23505".equals(e.getSQLState())) {
                    throw new StorageFileAlreadyExistsException(
                            "File already exists: " + originalFilename);

                }
            }

        } catch (IOException e) {
            throw new StorageException("Could not create owner subdirectory");
        }
    }

    @Override
    public Stream<String> loadAll(String owner) {
        var sql = "select name from filepaths where owner = ?";
        Stream.Builder<String> builder = Stream.builder();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setObject(1, UUID.fromString(owner));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                builder.add(rs.getString(1));
            }
            return builder.build();
        } catch (SQLException e) {
            throw new StorageException("Could not retrieve filename list", e);
        }

    }

    @Override
    public Resource loadAsResource(String filename, String owner) {
        try {
            Path file = Paths.get(rootLocation.toString(), owner, filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }

    }

    @Override
    public void deleteAll() {
        String sql = "drop table if exists filepaths";
        try {
            if (Files.exists(rootLocation) && !FileSystemUtils.deleteRecursively(rootLocation.toFile())) {
                throw new StorageException("Could not delete all files");
            }
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new StorageException("Could not delete table filepaths");
        }
    }

    @Override
    public void delete(String filename, String owner) {
        String sql = "delete from filepaths where name = ? and owner = ?";
        try {
            if (Files.exists(Paths.get(rootLocation.toString(), owner, filename))) {
                Files.delete(Paths.get(rootLocation.toString(), owner, filename));
            } else {
                System.out.println("File doesn't exist, trying to restore consistency");
            }
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, filename);
            stmt.setObject(2, UUID.fromString(owner));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(
                    "Deleted the file from filesystem but not the corresponding entry in DB");
        } catch (IOException e) {
            throw new StorageException("Could not delete file");
        }
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            String sql = """
                    create table if not exists filepaths (
                            id serial primary key,
                            name varchar(255) not null,
                            owner uuid not null,
                            unique (name, owner)
                            )
                    """;
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize storage", e);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
