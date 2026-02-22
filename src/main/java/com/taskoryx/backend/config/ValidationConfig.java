package com.taskoryx.backend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;

/**
 * Configuration for Validation Messages with UTF-8 encoding
 * Allows Vietnamese characters to be displayed correctly in validation messages
 */
@Configuration
public class ValidationConfig {

    /**
     * Configure MessageSource to read ValidationMessages.properties with UTF-8 encoding
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();

        // Set base name for validation messages
        messageSource.setBasename("classpath:ValidationMessages");

        // Set encoding to UTF-8 to support Vietnamese characters
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

        // Cache messages for 1 hour (3600 seconds)
        messageSource.setCacheSeconds(3600);

        return messageSource;
    }

    /**
     * Configure LocalValidatorFactoryBean to use our custom MessageSource
     */
    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}
