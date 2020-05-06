package com.zss.ecom.registration.facade;

import com.zss.ecom.registration.exception.ApplicationException;
import com.zss.ecom.registration.model.RegistrationResponse;
import com.zss.ecom.registration.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.CountDownLatch;

import static com.zss.ecom.registration.config.RegistrationConstants.BACKOFFICE_GROUP_ID;
import static com.zss.ecom.registration.config.RegistrationConstants.BACKOFFICE_TOPIC;

@RestController
@Slf4j
public class RegistrationController {

    private final KafkaTemplate<String, User> kafkaTemplate;

    private final CountDownLatch countDownLatch;

    @Value(value = "${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Autowired
    public RegistrationController(KafkaTemplate<String, User> kafkaTemplate, CountDownLatch countDownLatch){
        this.kafkaTemplate = kafkaTemplate;
        this.countDownLatch = countDownLatch;
    }

    @PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    public RegistrationResponse userRegistration(@RequestBody User user){
      log.info("Received request for user registration {}", user.toString());
      this.sendMessage(user);
      return RegistrationResponse.builder().message("User Registration Submitted").build();
    }

    @KafkaListener(topics = BACKOFFICE_TOPIC, groupId = BACKOFFICE_GROUP_ID)
    public void userBackOfficeListener(String user){
        log.info("Received message from back office {}", user);
        this.countDownLatch.countDown(); //This for test purpose only have remove
    }

    private void sendMessage(User user){
        ListenableFuture<SendResult<String, User>> listenableFuture = kafkaTemplate.send(defaultTopic, user);
        listenableFuture.addCallback((ListenableFutureCallback<? super SendResult<String, User>>) new ListenableFutureCallback<SendResult<String, User>>() {
            @Override
            public void onSuccess(SendResult<String, User> result) {
                log.info("Sent message to back office=[{}] with offset=[{}]", user.toString(), result.getRecordMetadata().offset());
            }
            @Override
            public void onFailure(Throwable ex) {
               throw new ApplicationException(String.format("Unable to send message to back office=[{%s}]", user.toString()), ex);
            }
        });
    }
}
