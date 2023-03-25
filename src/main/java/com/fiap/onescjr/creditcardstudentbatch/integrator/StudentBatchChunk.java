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
        fixedLengthTokenizer.setNames("register");
        fixedLengthTokenizer.setColumns(
                new Range(1,55) // Posição do registro completo
        );
        return fixedLengthTokenizer;
    }

    private RecordSeparatorPolicy removeInvalidLine() {
        return new SimpleRecordSeparatorPolicy() {
            @Override
            public boolean isEndOfRecord(String line) {
                if(line.trim().length() == 0 /* || line.trim().contains("\u001A") */ ) {
                    return false;   // Desconsidera as linhas sem dados/inválidas
                }
                return true;
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
                .encoding("UTF-8")
                .strict(true)
                /*
                BUG do Spring Batch 5.0.1?
                    Somente o primeiro comentário "..-A-.." está sendo removido do arquivo, no entanto,
                    .comments("--"), de acordo com a documentação deveria ignorar todos os comentários do arquivo
                 */
                //.comments("--", "---------------------------B---------------------------") //
                .recordSeparatorPolicy(removeInvalidLine())
                .lineTokenizer(fixedLengthTokenizer)
                .targetType(StudentIn.class)    // Objeto para "Stage" dos dados
                .build();
    }

    @Bean
    public ItemProcessor<StudentIn, StudentOut> itemProcessor() {
        return studentIn -> {
            StudentOut studentOut = new StudentOut();

            // Alternativa ao FlatFileItemReaderBuilder.comments("-")
            if(studentIn.getRegister().startsWith("-")) {
                log.warn("[COMENTÁRIO REMOVIDO]: " + studentIn.getRegister());
                return null;    // Desconsidera as linhas com comentários
            }

            /*if(studentIn.getRegister().contains("\u001A")) {
                log.warn("FINAL DO ARQUIVO");
                return null;    // FIXME O caractér \u001A ([SUB]) não é reconhecido no final do arquivo
            }*/

            studentOut.setName(studentIn.getRegister().substring(0,40));
            studentOut.setId(Long.valueOf(studentIn.getRegister().substring(41,48)));
            studentOut.setCardCode(studentIn.getRegister().substring(49,55));
            log.info(String.format(
                    "Registro processado...: %s | %s | %s",
                    studentOut.getName(), studentOut.getId(), studentOut.getCardCode())
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
