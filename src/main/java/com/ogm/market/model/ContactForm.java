package com.ogm.market.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactForm {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Enter a valid email")
    //@NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;

    //@NotBlank(message = "Message is required")
    private String message;
}
