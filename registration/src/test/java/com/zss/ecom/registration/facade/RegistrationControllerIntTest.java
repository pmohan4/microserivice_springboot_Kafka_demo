package com.zss.ecom.registration.facade;

import com.zss.ecom.registration.model.User;
import kafka.server.KafkaServer;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.zss.ecom.registration.config.RegistrationConstants.BACKOFFICE_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1,
        controlledShutdown = true,
        brokerProperties={
                "log.dir=target/embedded-kafka"
        })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationControllerIntTest {

    @Value(value = "${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountDownLatch latch;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    @DisplayName("Test user registration submitted successfully to back office - kafka producer")
    public void testRegistrationSubmitToBackOfficeSuccessfully() throws Exception {

        Consumer<String, User> consumer = buildConsumer();
        consumer.subscribe(Collections.singleton(defaultTopic));

        this.mockMvc.perform(post("/submit").content(getRequest())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"message\": \"User Registration Submitted\"}"));

        ConsumerRecord<String, User> singleRecord = KafkaTestUtils.getSingleRecord(consumer, defaultTopic);
        assertNotNull(singleRecord);
        assertEquals(getExpected(), singleRecord.value().toString());
    }

    @Test
    @DisplayName("Test received user update from back office - kafka consumer")
    public void testUserUpdateReceivedFromBackOffice() throws Exception {

        Producer<String, User> producer = buildProducer();
        producer.send(new ProducerRecord<>(BACKOFFICE_TOPIC, User.builder().id(1).build()));
        producer.flush();
        latch.await(5L, TimeUnit.SECONDS); //or can use Await package to wait for response
        assertEquals(0, latch.getCount());
    }

    @Test
    @DisplayName("Test invalid content type received for user registration")
    public void testInvalidContentTypeReceivedForRegistration() throws Exception {
        this.mockMvc.perform(post("/submit").content("<xml><xml>")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(status().is(415));
    }

    private String getExpected() {
        return "User(id=0, title=mr, firstName=TestUser, lastName=TestLast, email=testEmailAddress@gmail.com, password=P@ssw0rd, " +
                "verifyPassword=P@ssw0rd, telephone=1.23456792E8, mobileNumber=9.8745631E9, customerType=STOCKIST, drugLicenseNo=654, " +
                "receiveMarketingMails=true, termsAndConditions=true, addresses=null)";
    }

    public static String getRequest() throws IOException {
        File file = new ClassPathResource("userRequest.json").getFile();
        return FileUtils.readFileToString(file);
    }

    private Consumer<String, User> buildConsumer() {
        final Map<String, Object> consumerProps = KafkaTestUtils
                .consumerProps(defaultTopic, "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final DefaultKafkaConsumerFactory<String, User> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(User.class));
        return consumerFactory.createConsumer();
    }

    private Producer<String, User> buildProducer() {
        final Map<String, Object> producerProps = KafkaTestUtils
                .producerProps(embeddedKafkaBroker);

        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        final DefaultKafkaProducerFactory<String, User> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        return producerFactory.createProducer();
    }

    @AfterAll
    public void tearDown(){
        embeddedKafkaBroker.getKafkaServers().forEach(KafkaServer::shutdown);
        embeddedKafkaBroker.getKafkaServers().forEach(KafkaServer::awaitShutdown);
    }
}