package com.rishu.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Test-only key, 32+ bytes; never used outside tests
    private static final String TEST_SECRET = "test-only-jwt-secret-at-least-32-bytes-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // arrange
        jwtService = new JwtService(TEST_SECRET);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void constructor_rejectsMissingSecret(String secret) {

        assertThatThrownBy(() -> new JwtService(secret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void constructor_rejectsSecretShorterThan32Bytes() {

        assertThatThrownBy(() -> new JwtService("only-31-bytes-long-secret-value"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithADifferentSecret() {

        JwtService otherService = new JwtService("another-test-only-secret-of-32-plus-bytes");

        String token = otherService.generateToken("raj@gmail.com");

        assertThat(jwtService.isTokenValid(token)).isFalse();
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