package com.example.batch;

import com.example.batch.processing.LineProcessor;
import com.example.batch.progress.FileSizeInitializer;
import com.example.batch.progress.ProgressReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfiguration.class);

    /**
     * Default encoding of source CSV files.
     */
    private static final String DEFAULT_SOURCE_CHARSET = "UTF-8";

    /**
     * @return {@link org.springframework.batch.item.file.FlatFileItemReader} that reads each line of a provided resource
     * "as is," i.e. without any parsing attempts, assuming
     * {@value #DEFAULT_SOURCE_CHARSET} as a default encoding.
     */
    @StepScope
    @Bean
    public ResourceAwareItemReaderItemStream<String> csvItemReader(
            @Value("#{stepExecutionContext[fileName]}") Resource file) {
        return new FlatFileItemReaderBuilder<String>()
                .resource(file)
                .encoding(DEFAULT_SOURCE_CHARSET)
                .lineMapper(new PassThroughLineMapper())
                .saveState(false)
                .build();
    }

    /**
     * @return {@link org.springframework.batch.item.file.FlatFileItemWriter} that writes each line that passed filtration into a provided resource
     * "as is," i.e. without any conversion attempts, assuming
     * {@value #DEFAULT_SOURCE_CHARSET} as a default encoding.
     */
    @StepScope
    @Bean
    public ResourceAwareItemWriterItemStream<Object> csvItemWriter(
            @Value("#{@pathUtils.destinationFile(jobParameters['destinationDirectory'], stepExecutionContext[fileName])}") Resource file) {
        return new FlatFileItemWriterBuilder<>()
                .name("csvAsIsWriter")
                .resource(file)
                .encoding(DEFAULT_SOURCE_CHARSET)
                .lineAggregator(new PassThroughLineAggregator<>())
                .shouldDeleteIfExists(true)
                .transactional(false)
                .saveState(false)
                .build();
    }

    @Bean
    public TaskExecutor partitionedTaskExecutor(
            @Value("#{environment.parallelism}") Integer parallelism) {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        /*
         * Attempt to follow a classic JCiP formula assuming that: 1. we want to have 100% utilization of each CPU, 2. we assume that an each thread will be non-blocked 70% of its execution time (pure speculation since I didn't do any benchmarking, but it'll mostly be occupied with I/O, so waits are inevitable).
         */
        final Integer corePoolSize = Optional.ofNullable(parallelism)
                .orElse((int) (Runtime.getRuntime().availableProcessors() * 1.7));

        LOGGER.info("Requested parallelism degree: {}, resulting degree: {}", parallelism,
                corePoolSize);

        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);

        /*
         * Again, purely concocted buffer of 5 threads. Could try to calculate it based on a number of input files.
         */
        threadPoolTaskExecutor.setMaxPoolSize(corePoolSize + 5);

        /*
         * I'd never used this in production, but in the current case it's acceptable: allows tool to terminate gracefully without any actions from user's side and avoids coupling this component with any kind of Job listeners for shutdown upon completion.
         */
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(true);
        threadPoolTaskExecutor.setKeepAliveSeconds(2);
        return threadPoolTaskExecutor;
    }

    @JobScope
    @Bean
    public Partitioner partitioner(
            @Value("file:#{jobParameters['sourceDirectory']}/*") Resource[] csvFiles) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        partitioner.setResources(csvFiles);
        return partitioner;
    }

    @Bean
    public PartitionHandler partitionHandler(TaskExecutor partitionedTaskExecutor,
            Step singleFileStep) {
        final TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(partitionedTaskExecutor);
        partitionHandler.setStep(singleFileStep);
        return partitionHandler;
    }

    @Bean
    public SimpleStepBuilder<String, Object> simpleStepBuilder(
            StepBuilderFactory stepBuilderFactory, @Value("${settings.chunkSize}") int chunkSize) {
        return stepBuilderFactory.get("singleFileStep")
                .chunk(chunkSize);
    }

    @Bean
    public Step singleFileStep(SimpleStepBuilder<String, Object> simpleStepBuilder,
            ResourceAwareItemReaderItemStream<String> csvItemReader,
            LineProcessor lineProcessor,
            ResourceAwareItemWriterItemStream<Object> csvItemWriter,
            ProgressReporter progressReporter,
            FileSizeInitializer fileSizeInitializer) {
        return simpleStepBuilder
                .reader(csvItemReader).writer(csvItemWriter)
                .processor(lineProcessor)
                .listener(progressReporter)
                .listener(fileSizeInitializer)
                .build();
    }

    @Bean
    public Step partitionedReadFilterWriteStep(StepBuilderFactory stepBuilderFactory,
            Partitioner partitioner, PartitionHandler partitionHandler,
            Step singleFileStep) {
        return stepBuilderFactory.get("readFilterWrite")
                .partitioner("singleFileStep", partitioner)
                .partitionHandler(partitionHandler).step(singleFileStep).build();
    }

    @Bean
    public Job job(JobBuilderFactory jobFactory, Step partitionedReadFilterWriteStep) {
        return jobFactory.get("csvEtl").start(partitionedReadFilterWriteStep)
                .build();
    }

    /**
     * An empty implementation in order to default to in-memory {@link JobRepository} impl.
     *
     * @param dataSource ignored
     */
    @Override
    public void setDataSource(DataSource dataSource) {
    }
}
