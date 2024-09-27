package com.colak.springtutorial.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@Slf4j
public class CSVBatchConfig {

    @Value("classpath:/market-data.csv")
    private Resource csvFile;

    @Bean
    public Job job(JobRepository jobRepository, Step marketDataCsvStep) {
        // We are now required to pass in JobRepository upon using JobBuilder
        return new JobBuilder("job", jobRepository)
                .start(marketDataCsvStep)
                .build();
    }

    @Bean
    public Step faultTolerantMarketDataCsvStep(JobRepository jobRepository) {
        return new StepBuilder("step_first", jobRepository)
                // The ResourcelessTransactionManager in Spring Framework is a transaction manager designed for simulating in-memory transactions without
                // interacting with external resources, often used for testing and lightweight transaction management scenarios.
                // When you use ResourcelessTransactionManager, your batch job steps can run in a transaction-like manner, but there are no actual
                // database commits or rollbacks involved.
                // This transaction manager is typically used for jobs that perform read-only operations or jobs where transaction management is not necessary.
                .<MarketData, MarketData>chunk(4, new ResourcelessTransactionManager())
                .reader(marketDataCsvReader())
                // Chunk processing now processes Chunk datatype instead of a List
                .writer(chunk -> chunk.forEach(item -> log.info("Market Data: {}", item)))
                .build();
    }

    @Bean
    public ItemReader<MarketData> marketDataCsvReader() {
        return new FlatFileItemReaderBuilder<MarketData>()
                .name("marketDataCsvReader") // Reader name
                .resource(csvFile)           // Resource (CSV file)
                .linesToSkip(1)              // Skip the header line
                .delimited()                 // Enable delimited tokenizer
                .delimiter(DelimitedLineTokenizer.DELIMITER_COMMA)              // Specify the delimiter (comma)
                .names("TID", "TickerName", "TickerDescription") // Column names
                .fieldSetMapper(new MarketDataFieldSetMapper()) // Custom FieldSetMapper
                .build();
    }
}