package com.example.batch.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializes an execution context with a number of lines particular file contains.
 */
@Component
public class FileSizeInitializer extends StepExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSizeInitializer.class);

    @Value("${stepExecution.keys.fileName}")
    private String filenameKey;

    @Value("${stepExecution.keys.lineNumbers}")
    private String lineNumbersKey;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        final ExecutionContext executionContext = stepExecution.getExecutionContext();
        final String fileName = executionContext.getString(filenameKey);

        Assert.hasText(fileName,
                "File pathname has not been initialized - something terrible happened. Execution details: "
                        + stepExecution.getSummary());
        final Path path;
        try {
            path = Paths.get(new URI(fileName));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Incorrect file name syntax, file does not exist or else.", e);
        }

        /*
         * XXX: Definitely not the fastest solution, but: 1. we have no performance requirements whatsoever, 2. it's brief, readable and doesn't require additional libraries
         */
        final long numberOfLines;
        try {
            numberOfLines = Files.lines(path).count();
            executionContext.putLong(lineNumbersKey, numberOfLines);
        } catch (IOException e) {
            LOGGER.warn(
                    "Unable to count number of lines for {} - progress will not be reported for it.",
                    fileName);
        }
    }
}
