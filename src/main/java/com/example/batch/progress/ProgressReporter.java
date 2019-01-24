package com.example.batch.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.ChunkListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writes log records regarding how many lines were processed.
 */
@Component
public class ProgressReporter extends ChunkListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSizeInitializer.class);

    @Value("${stepExecution.keys.fileName}")
    private String filenameKey;

    @Value("${stepExecution.keys.lineNumbers}")
    private String lineNumbersKey;

    @Override
    public void afterChunk(ChunkContext context) {
        final StepExecution stepExecution = context.getStepContext().getStepExecution();
        final ExecutionContext executionContext = stepExecution.getExecutionContext();
        LOGGER.info("{}: {} out of {} lines processed.", executionContext
                .getString(filenameKey), stepExecution.getReadCount(), executionContext
                .getLong(lineNumbersKey));
    }
}
