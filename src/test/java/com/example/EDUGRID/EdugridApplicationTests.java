package com.example.EduGrid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Este teste requer o MySQL local configurado em application.properties.")
@SpringBootTest(properties = {
        "edugrid.dados.adequar-na-inicializacao=false"
})
class EdugridApplicationTests {

	@Test
	void contextLoads() {
	}

}
