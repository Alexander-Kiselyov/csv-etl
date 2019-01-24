package com.example.batch.util;

import org.springframework.core.io.Resource;

import java.net.URL;

public interface PathUtil {

    /**
     * Calculates a new pathname for an output file.
     *
     * @param destDir destination directory, into which the original file must go
     * @param srcFile original file's pathname in the form of a URI ({@link URL#toExternalForm()}).
     * @return resulting file resource, which is located in destination directory and has a name of an original (source) file
     */
    Resource destinationFile(String destDir, String srcFile);
}
