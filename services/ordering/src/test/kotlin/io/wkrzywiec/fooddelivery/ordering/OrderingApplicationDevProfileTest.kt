package io.wkrzywiec.fooddelivery.ordering

import org.junit.jupiter.api.Test
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@DirtiesContext
@ActiveProfiles("dev")
class OrderingApplicationDevProfileTest: IntegrationTest() {

    @Test
    fun `Spring Context is loaded`() {
    }
}