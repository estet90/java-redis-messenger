package ru.redisMessenger.application.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.Charset;
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
     * @param filePath path to target file
     * @param text of messages
     * @return count lines in new file
     * @throws IOException when directory doesn't exist
     */
    public int writeLines(String filePath, Collection<String> text) throws IOException {
        Path file = Paths.get(filePath);
        Files.write(file, text, Charset.forName("UTF-8"));
        return text.size();
    }

}
