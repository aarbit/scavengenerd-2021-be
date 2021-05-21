package com.nerdery.scavengenerd.scavengenerd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer




@SpringBootApplication
open class ScavengenerdApplication {
    @Bean
    open fun corsConfigurer(): WebMvcConfigurer? {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedMethods("POST", "PUT", "GET", "OPTIONS", "DELETE","PATCH","HEAD")
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ScavengenerdApplication>(*args)
}
