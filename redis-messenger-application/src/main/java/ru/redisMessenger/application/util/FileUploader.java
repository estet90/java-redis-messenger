package ru.redisMessenger.application.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * uploader
 */
@Log4j2
public class FileUploader {

    /**
     * upload
     * @param filePath {@link String} path to target file
     * @param text {@link Collection<String>} of messages
     * @return int count lines in new file as int
     * @throws IOException when directory is incorrect
     */
    public int writeLines(String filePath, Collection<String> text) throws IOException {
        Path file = Paths.get(filePath);
        Files.write(file, text, StandardCharsets.UTF_8);
        return text.size();
    }

}
