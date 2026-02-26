package com.clevertricks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.stream.Stream;

public class PostgresqlStorageService implements StorageService {
    private final String baseUrl = "jdbc:postgresql://";
    private final String host;
    private final int port;
    private final String db;
    private final String dbUrl;

    private final Connection conn;

    @Autowired
    PostgresqlStorageService(StorageProperties properties) {
        this.host = properties.getHost();
        this.port = properties.getPort();
        this.db = properties.getDb();
        this.dbUrl = baseUrl + host + ":" + String.valueOf(port) + "/" + db;

        try {
            this.conn = DriverManager.getConnection(dbUrl, properties.getUsername(), properties.getPassword());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    // check uniqueness
    @Override
    public void store(MultipartFile file, String owner) throws StorageException {
        String sql = "INSERT INTO files (name, owner, data) VALUES (?, ?, ?)";
        if (file.isEmpty()) {
            throw new StorageFileEmptyException("Cannot store empty file");
        }
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, file.getOriginalFilename());
            stmt.setObject(2, UUID.fromString(owner));
            stmt.setBytes(3, file.getBytes());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new StorageFileAlreadyExistsException("File already exists: " + file.getOriginalFilename());
            }
            throw new StorageException("Failed to store file", e);
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }

    }

    @Override
    public Stream<String> loadAll(String owner) {
        String sql = "select name from files where owner = ?";
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
        String sql = "SELECT data from files where name = ? and owner = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, filename);
            stmt.setObject(2, UUID.fromString(owner));
            ResultSet resultset = stmt.executeQuery();

            if (resultset.next()) {
                byte[] data = resultset.getBytes(1);
                return new ByteArrayResource(data);
            }
            throw new StorageFileNotFoundException("Could not read file: " + filename
                    + "\nFor owner:" + owner);
        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve file", e);
        }

    }

    @Override
    public void deleteAll() {
        String sql = "drop table if exists files";
        try {
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new StorageException("Failed to delete files", e);
        }

    }

    @Override
    public void delete(String filename, String owner) {
        String sql = "delete from files where name = ? and owner = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, filename);
            stmt.setObject(2, UUID.fromString(owner));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new StorageException("Failed to delete file");
        }

    }

    @Override
    public void init() {
        System.out.println("initializing");
        String sql = """
                create table if not exists files (
                        id serial primary key,
                        name varchar(255) not null,
                        owner uuid not null,
                        data bytea not null,
                        unique (name, owner)
                )
                """;
        try {
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize storage", e);
        }
    }
}
