package com.zss.ecom.registration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Address {

    private int id;

    @NotNull
    private String title;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    private boolean isShippingAddress;

    private boolean isDefaultAddress;

    @NotNull
    private String line1;

    private String line2;

    @NotNull
    private String city;

    @NotNull
    private String town;

    @NotNull
    private String state;
}
