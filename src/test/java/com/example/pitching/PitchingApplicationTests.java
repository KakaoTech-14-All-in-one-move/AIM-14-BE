package com.example.pitching;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class PitchingApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void generateAndVerifyPassword() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String rawPassword = "password123";
		String hashedPassword = encoder.encode(rawPassword);
		System.out.println("New hashed password: " + hashedPassword);

		boolean matches = encoder.matches(rawPassword, hashedPassword);
		System.out.println("Verification with new hash: " + matches);
	}
}
