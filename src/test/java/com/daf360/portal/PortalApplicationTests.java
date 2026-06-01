package com.daf360.portal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRsaKeyConfig.class)
class PortalApplicationTests {

	@Test
	void contextLoads() {
	}

}
