package io.wkrzywiec.fooddelivery.ordering

import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("prod")
class OrderingApplicationProdProfileTest: IntegrationTest() {

    @Test
    fun `Spring Context is loaded`() {
    }
}