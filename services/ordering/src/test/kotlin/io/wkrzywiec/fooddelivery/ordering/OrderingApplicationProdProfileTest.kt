package io.wkrzywiec.fooddelivery.ordering

import org.junit.jupiter.api.Test
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@DirtiesContext
@ActiveProfiles("prod")
class OrderingApplicationProdProfileTest: IntegrationTest() {

    @Test
    fun `Spring Context is loaded`() {
    }
}