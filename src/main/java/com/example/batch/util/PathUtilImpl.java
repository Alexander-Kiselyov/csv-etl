package com.example.batch.util;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

@Component("pathUtils")
public class PathUtilImpl implements PathUtil {

    @Override
    public Resource destinationFile(String destDir, String srcFile) {
        final FileSystemResource srcFileResource = new FileSystemResource(srcFile);
        final File resultingFile = Paths.get(destDir, srcFileResource.getFilename()).toFile();
        return new FileSystemResource(resultingFile);
    }
}
