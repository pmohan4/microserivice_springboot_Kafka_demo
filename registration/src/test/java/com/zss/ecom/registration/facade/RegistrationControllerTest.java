package com.zss.ecom.registration.facade;


import com.zss.ecom.registration.exception.ApplicationException;
import com.zss.ecom.registration.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static com.zss.ecom.registration.facade.RegistrationControllerIntTest.getRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private KafkaTemplate<String, User> kafkaTemplate;

    @Mock
    private ListenableFuture<SendResult<String, User>> responseFuture;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    @DisplayName("Test user registration sent failed to back office")
    public void testRegistrationSubmitToFailedToBackOffice() throws Exception {

        String expectedMessage = "Unable to send message to back office=[{User(id=0, title=null, firstName=null, lastName=null, " +
                "email=null, password=null, verifyPassword=null, telephone=0.0, mobileNumber=0.0, customerType=null, " +
                "drugLicenseNo=null, receiveMarketingMails=false, termsAndConditions=false, addresses=null)}]";

        doAnswer(invocationOnMock -> {
            ListenableFutureCallback listenableFutureCallback = invocationOnMock.getArgument(0);
            listenableFutureCallback.onFailure(new RuntimeException());
            return null;
        }).when(responseFuture).addCallback(any(ListenableFutureCallback.class));

        when(kafkaTemplate.send(any(),any())).thenReturn(responseFuture);

        Exception exception = assertThrows(ApplicationException.class,
                () -> {registrationController.userRegistration(User.builder().build());});
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("Test exception handler handles application exceptions")
    public void testExceptionHandlerForExceptions() throws Exception {
       MockMvc mvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setControllerAdvice(new RegistrationControllerAdvice())
                .build();
       MockHttpServletResponse response =  mvc.perform(post("/submit").content(getRequest())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andReturn().getResponse();

          assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
    }
}