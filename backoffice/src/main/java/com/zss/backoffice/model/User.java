package com.zss.backoffice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class User {

    private int id;

    @NotNull
    private String title;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @NotNull
    private String email;

    @NotNull
    private String password;

    @NotNull
    private String verifyPassword;

    private float telephone;

    @NotNull
    private float mobileNumber;

    private String customerType;

    private String drugLicenseNo;

    private boolean receiveMarketingMails;

    private boolean termsAndConditions;

    @NotNull
    ArrayList<Address> addresses;

}
