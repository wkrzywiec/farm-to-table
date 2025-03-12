package io.wkrzywiec.fooddelivery.ordering

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication(
    scanBasePackages = ["io.wkrzywiec.fooddelivery.ordering", "io.wkrzywiec.fooddelivery.commons"
    ]
)
class OrderingApplication {

    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(OrderingApplication::class.java, *args)
        }
    }
}
