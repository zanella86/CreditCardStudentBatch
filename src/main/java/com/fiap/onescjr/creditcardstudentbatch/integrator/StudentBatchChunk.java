package com.fiap.onescjr.creditcardstudentbatch.integrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "integration.file.students.load", havingValue = "true")
public class StudentBatchChunk {

    @Value("${integration.file.students.chunk-size}")
    private int chunkSize;

    @Bean
    public FixedLengthTokenizer fixedLengthTokenizer() {
        FixedLengthTokenizer fixedLengthTokenizer = new FixedLengthTokenizer();
        fixedLengthTokenizer.setNames("name", "id", "cardCode");
        fixedLengthTokenizer.setColumns(
                new Range(1,41),    //name
                new Range(42,49),   //id
                new Range(50,55)    //cardCode
        );
        return fixedLengthTokenizer;
    }

    private RecordSeparatorPolicy removeBlankLine() {
        return new SimpleRecordSeparatorPolicy() {
            @Override
            public boolean isEndOfRecord(String line) {
                if(line.trim().length() == 0) {
                    return false;
                }
                return super.isEndOfRecord(line);
            }
        };
    }

    @Bean
    public ItemReader<StudentIn> itemReader(
            @Value("${integration.file.students.source}") Resource resource,
            FixedLengthTokenizer fixedLengthTokenizer) throws IOException {

        log.info("Arquivo lido...: " + resource.getFile().getPath());
        return new FlatFileItemReaderBuilder<StudentIn>()
                .name("FileReader")
                .resource(resource)
                .strict(true)
                .comments("--") // FIXME: Nem todos os comentários são removidos..
                .recordSeparatorPolicy(removeBlankLine())
                .lineTokenizer(fixedLengthTokenizer)
                .targetType(StudentIn.class)
                .build();
    }

    @Bean
    public ItemProcessor<StudentIn, StudentOut> itemProcessor() {
        return studentIn -> {
            StudentOut studentOut = new StudentOut();
            studentOut.setName(studentIn.getName().trim());
            studentOut.setId(Long.valueOf(studentIn.getId()));
            studentOut.setCardCode(studentIn.getCardCode());
            log.info(String.format(
                    "Registro processado...: %s | %s | %s",
                    studentIn.getName(), studentIn.getId(), studentIn.getCardCode())
            );
            return studentOut;
        };
    }

    @Bean
    public ItemWriter<StudentOut> itemWriter(DataSource dataSource) throws SQLException {
        return new JdbcBatchItemWriterBuilder<StudentOut>()
                .dataSource(dataSource)
                .beanMapped()
                .sql("insert into TB_STUDENT(name, id, card_code) values (:name, :id, :cardCode)")
                .build();
    }

    @Bean
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager platformTransactionManager,
                     ItemReader<StudentIn> itemReader,
                     ItemProcessor<StudentIn, StudentOut> itemProcessor,
                     ItemWriter<StudentOut> itemWriter) throws ParseException {

        log.info("Chuck Size.....: " + chunkSize);
        return new StepBuilder("ExecuteStep", jobRepository)
                .<StudentIn, StudentOut>chunk(chunkSize, platformTransactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Job job(JobRepository jobRepository, Step step) {
        return new JobBuilder("ExecuteJob", jobRepository).start(step).build();
    }

}
