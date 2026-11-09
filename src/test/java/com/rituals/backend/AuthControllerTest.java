package com.rituals.backend;

import com.rituals.backend.controller.AuthController;
import com.rituals.backend.dto.AuthRequest;
import com.rituals.backend.dto.AuthResponse;
import com.rituals.backend.dto.RegisterRequest;
import com.rituals.backend.entity.AppUser;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository.findByEmail("testauth@rituals.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("casedtest@rituals.com").ifPresent(userRepository::delete);
        userRepository.findByUsername("testuser_autotest").ifPresent(userRepository::delete);
        userRepository.findByUsername("loginuser_autotest").ifPresent(userRepository::delete);
        userRepository.findByUsername("wrongpwd_autotest").ifPresent(userRepository::delete);
        userRepository.findByUsername("casetest_user").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT token")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser_autotest");
        request.setEmail("testauth@rituals.com");
        request.setPassword("SecurePass123!");
        request.setGender("Female");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuthResponse);
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertNotNull(authResponse.getJwt());
        assertEquals("testuser_autotest", authResponse.getUsername());

        // Validate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername("testauth@rituals.com");
        assertTrue(jwtUtil.validateToken(authResponse.getJwt(), userDetails));
    }

    @Test
    @DisplayName("Should normalize email (lowercase and trim) and allow case-insensitive login")
    void testEmailCaseInsensitivityAndTrimming() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("casetest_user");
        request.setEmail("  CaseDTest@Rituals.COM  ");
        request.setPassword("SecurePass123!");
        request.setGender("Male");
        request.setDateOfBirth(LocalDate.of(1996, 4, 12));

        ResponseEntity<?> regResponse = authController.registerUser(request);
        assertEquals(HttpStatus.OK, regResponse.getStatusCode());

        // Login with all lowercase email and no whitespace
        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("casedtest@rituals.com");
        loginReq.setPassword("SecurePass123!");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(loginReq);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertTrue(loginResp.getBody() instanceof AuthResponse);

        // Login with uppercase and trailing spaces
        AuthRequest loginReqSpaces = new AuthRequest();
        loginReqSpaces.setEmail("  CASEdtest@RITUALS.COM  ");
        loginReqSpaces.setPassword("SecurePass123!");

        ResponseEntity<?> loginRespSpaces = authController.createAuthenticationToken(loginReqSpaces);
        assertEquals(HttpStatus.OK, loginRespSpaces.getStatusCode());
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser1_autotest");
        request.setEmail("testauth@rituals.com");
        request.setPassword("SecurePass123!");
        request.setGender("Female");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));

        authController.registerUser(request);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("testuser2_autotest");
        duplicate.setEmail("TESTAUTH@rituals.com"); // Case insensitive check
        duplicate.setPassword("SecurePass123!");
        duplicate.setGender("Male");
        duplicate.setDateOfBirth(LocalDate.of(1999, 5, 10));

        ResponseEntity<?> response = authController.registerUser(duplicate);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> map = (Map<?, ?>) response.getBody();
        assertEquals("Email is already taken!", map.get("message"));
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void testRegisterWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("weakuser_autotest");
        request.setEmail("weak@rituals.com");
        request.setPassword("weak");
        request.setGender("Male");
        request.setDateOfBirth(LocalDate.of(2001, 3, 15));

        ResponseEntity<?> response = authController.registerUser(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Should reject registration with invalid email format")
    void testRegisterInvalidEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("invalidemail_autotest");
        request.setEmail("notanemail");
        request.setPassword("SecurePass123!");

        ResponseEntity<?> response = authController.registerUser(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void testLoginSuccess() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("loginuser_autotest");
        registerReq.setEmail("testauth@rituals.com");
        registerReq.setPassword("SecurePass123!");
        registerReq.setGender("Male");
        registerReq.setDateOfBirth(LocalDate.of(1995, 6, 20));

        authController.registerUser(registerReq);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("testauth@rituals.com");
        loginReq.setPassword("SecurePass123!");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(loginReq);

        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertTrue(loginResp.getBody() instanceof AuthResponse);
        AuthResponse authResp = (AuthResponse) loginResp.getBody();
        assertNotNull(authResp.getJwt());
        assertEquals("loginuser_autotest", authResp.getUsername());
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void testLoginWrongPassword() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("wrongpwd_autotest");
        registerReq.setEmail("testauth@rituals.com");
        registerReq.setPassword("SecurePass123!");
        registerReq.setGender("Male");
        registerReq.setDateOfBirth(LocalDate.of(1995, 6, 20));

        authController.registerUser(registerReq);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("testauth@rituals.com");
        loginReq.setPassword("WrongPassword999!");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(loginReq);

        assertEquals(HttpStatus.UNAUTHORIZED, loginResp.getStatusCode());
        assertTrue(loginResp.getBody() instanceof Map);
        Map<?, ?> map = (Map<?, ?>) loginResp.getBody();
        assertEquals("Incorrect email or password", map.get("message"));
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void testLoginNonExistentEmail() {
        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("nonexistent@rituals.com");
        loginReq.setPassword("SomePass123!");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(loginReq);
        assertEquals(HttpStatus.UNAUTHORIZED, loginResp.getStatusCode());
    }

    @Test
    @DisplayName("Should reject login with blank fields")
    void testLoginBlankFields() {
        AuthRequest emptyReq = new AuthRequest();
        emptyReq.setEmail("");
        emptyReq.setPassword("");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(emptyReq);
        assertEquals(HttpStatus.BAD_REQUEST, loginResp.getStatusCode());
    }

    @Test
    @DisplayName("Should reject registration with disposable or throwaway email domain")
    void testRegisterDisposableEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("disposable_user");
        request.setEmail("user123@mailinator.com");
        request.setPassword("SecurePass123!");
        request.setGender("Other");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));

        ResponseEntity<?> response = authController.registerUser(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> map = (Map<?, ?>) response.getBody();
        assertEquals("Please enter a valid, active email address.", map.get("message"));
    }

    @Test
    @DisplayName("Should track lastLoginAt and update it on successful login")
    void testLastLoginTracking() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("loginuser_autotest");
        registerReq.setEmail("testauth@rituals.com");
        registerReq.setPassword("SecurePass123!");
        registerReq.setGender("Male");
        registerReq.setDateOfBirth(LocalDate.of(1995, 6, 20));

        authController.registerUser(registerReq);

        AppUser user = userRepository.findByEmail("testauth@rituals.com").orElseThrow();
        assertNotNull(user.getLastLoginAt());

        // Simulate an old login from 10 days ago
        user.setLastLoginAt(java.time.LocalDateTime.now().minusDays(10));
        userRepository.save(user);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("testauth@rituals.com");
        loginReq.setPassword("SecurePass123!");

        ResponseEntity<?> loginResp = authController.createAuthenticationToken(loginReq);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        // Verify lastLoginAt was updated to recent
        AppUser updatedUser = userRepository.findByEmail("testauth@rituals.com").orElseThrow();
        assertTrue(updatedUser.getLastLoginAt().isAfter(java.time.LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("Prepare fresh user state for shivanimanivannan2@gmail.com")
    void prepareFreshUserForTest() {
        userRepository.findByEmail("shivanimanivannan2@gmail.com").ifPresent(user -> {
            userRepository.delete(user);
        });
        assertFalse(userRepository.existsByEmail("shivanimanivannan2@gmail.com"));
    }
}
