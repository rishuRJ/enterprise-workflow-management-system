package com.rishu.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // arrange
        jwtService = new JwtService();
    }

    @Test
    void extractEmail_returnsTheEmailTheTokenWasGeneratedFor() {

        // act
        String token = jwtService.generateToken("raj@gmail.com");

        String email = jwtService.extractEmail(token);

        // assert
        assertThat(email).isEqualTo("raj@gmail.com");
    }


    @Test
    void isTokenValid_returnsTrueForAGeneratedToken() {


        String token = jwtService.generateToken("raj");

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-token","abc.def.ghi"})
    void isTokenValid_returnsFalseForMalformedToken(String token) {


        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {

        String token = jwtService.generateToken("raj@gmail.com");

        String tamperedToken = token+'x';

        assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();

    }
}