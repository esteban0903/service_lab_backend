package com.example.taskassignment.security;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(username = "admin@demo.com", roles = {"ADMIN"})
public @interface WithMockAdmin {
}

