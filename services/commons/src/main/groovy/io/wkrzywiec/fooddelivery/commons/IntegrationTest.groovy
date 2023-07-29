package io.wkrzywiec.fooddelivery.commons

import io.restassured.RestAssured
import io.restassured.module.mockmvc.RestAssuredMockMvc
import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest extends Specification {

    protected static boolean useLocalInfrastructure() {
        // change it to `true` in order to use it with infra from docker-compose.yaml
        false
    }

    @Autowired
    private WebApplicationContext context

    @LocalServerPort
    private Integer port

    def setup() {
        RestAssuredMockMvc.webAppContextSetup(context)
        RestAssured.port = port
    }

}
