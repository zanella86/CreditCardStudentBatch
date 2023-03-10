package com.fiap.onescjr.creditcardstudentbatch.integrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Slf4j
@SpringBootApplication
@EnableBatchProcessing
@ConditionalOnProperty(value = "integration.file.students.load", havingValue = "true")
public class StudentBatchChunk {

    @Value("${integration.file.students.delimiter}")
    private String delimiter;

    @Value("${integration.file.students.chunk-size}")
    private int chunkSize;

    @Bean
    public ItemReader<StudentIn> itemReader(@Value("${integration.file.students.source}") Resource resource) throws IOException {
        log.info("Item lido...: " + resource.getFile().getParent());
        return new FlatFileItemReaderBuilder<StudentIn>()
                .name("File reader")
                .resource(resource)
                //.delimited().delimiter(delimiter).names("name", "id", "cardCode")   // TODO: Configurar layout do arquivo
                .delimited().delimiter(delimiter).names("student")
                //.fixedLength().names("student")
                .targetType(StudentIn.class)
                .build();
    }

    @Bean
    public ItemProcessor<StudentIn, StudentOut> itemProcessor() {
        return studentIn -> {
            StudentOut studentOut = new StudentOut();
            studentOut.setName(studentIn.getStudent());
            log.info("Item processado...: " + studentIn.getStudent());
            return studentOut;
        };
    }

    @Bean
    public ItemWriter<StudentOut> itemWriter(DataSource dataSource) throws SQLException {
        log.info("Item escrito...: " + dataSource.getConnection().getSchema());
        return new ItemWriter<StudentOut>() {
            @Override
            public void write(Chunk<? extends StudentOut> chunk) throws Exception {
                log.info(
                        String.format("Lendo chunk %s de %s", chunk.getItems().size(), chunk.size())    // TEST
                );
            }
        };
        /*return new JdbcBatchItemWriterBuilder<StudentOut>()
                .dataSource(dataSource)
                .beanMapped()
                .sql("insert into TB_STUDENT(name, id, cardCode) values (:name, :id, :cardCode)")
                .build();*/
    }

    @Bean
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager platformTransactionManager,
                     ItemReader<StudentIn> itemReader,
                     ItemProcessor<StudentIn, StudentOut> itemProcessor,
                     ItemWriter<StudentOut> itemWriter) {
        return new StepBuilder("Execute step", jobRepository)
                .<StudentIn, StudentOut>chunk(chunkSize, platformTransactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Job job(JobRepository jobRepository, Step step) {
        return new JobBuilder("Execute job", jobRepository).start(step).build();
    }

}
