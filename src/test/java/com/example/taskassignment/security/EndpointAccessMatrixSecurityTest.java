package com.example.taskassignment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Endpoint Access Matrix Deny-By-Default Security Tests")
class EndpointAccessMatrixSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private static final Set<String> ALLOWED_PUBLIC_ENDPOINTS = Set.of(
            "POST /api/auth/login",
            "GET /api/health",
            "GET /api/config"
    );

    @Test
    @DisplayName("Every mapped API endpoint except the 3 public ones must reject unauthenticated requests")
    void allNonPublicEndpoints_requireAuthentication() throws Exception {
        Set<String> testedEndpoints = new HashSet<>();

        for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = info.getPatternValues();
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();

            for (String pattern : patterns) {
                // Ignore error handling endpoints or non-api endpoints if any
                if (!pattern.startsWith("/api")) {
                    continue;
                }

                // Replace path variables like {id} or {taskId} with dummy values like "1"
                String samplePath = pattern.replaceAll("\\{[^}]+\\}", "1");

                Set<org.springframework.web.bind.annotation.RequestMethod> effectiveMethods = methods.isEmpty()
                        ? Set.of(org.springframework.web.bind.annotation.RequestMethod.GET)
                        : methods;

                for (org.springframework.web.bind.annotation.RequestMethod method : effectiveMethods) {
                    String endpointKey = method.name() + " " + pattern;
                    testedEndpoints.add(endpointKey);

                    if (ALLOWED_PUBLIC_ENDPOINTS.contains(endpointKey)) {
                        continue;
                    }

                    HttpMethod httpMethod = HttpMethod.valueOf(method.name());

                    // Request without any token must be rejected with 401 Unauthorized
                    mockMvc.perform(request(httpMethod, samplePath)
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isUnauthorized());
                }
            }
        }

        // Verify that we scanned and verified endpoints
        assertThat(testedEndpoints).containsAll(ALLOWED_PUBLIC_ENDPOINTS);
        assertThat(testedEndpoints.size()).isGreaterThan(10);
    }
}

