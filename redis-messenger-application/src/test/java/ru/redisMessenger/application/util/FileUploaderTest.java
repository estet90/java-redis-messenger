package ru.redisMessenger.application.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * tests for {@link FileUploader}
 */
public class FileUploaderTest {

    @Test
    public void upload() throws IOException {
        String filePath = Configuration.getInstance().getProperty(Configuration.Property.FILE_OUTPUT_DIRECTORY.getPropertyName());
        List<String> lines = new ArrayList<>();
        lines.add("qwe");
        lines.add("rty");
        filePath = filePath.concat("lines.txt");
        int linesCount = new FileUploader().writeLines(filePath, lines);
        assertEquals(linesCount, lines.size());
        boolean deleteStatus = new File(filePath).delete();
        assertTrue(deleteStatus);
    }

    @Test(expected = InvalidPathException.class)
    public void uploadException() throws IOException, InvalidPathException {
        String filePath = "***";
        List<String> lines = new ArrayList<>();
        lines.add("qwe");
        lines.add("rty");
        new FileUploader().writeLines(filePath, lines);
    }

}
