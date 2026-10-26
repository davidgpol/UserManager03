package com.hariklabs.usermanager03.config;

import org.springframework.context.annotation.*;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JsonConfiguration {
    @Bean JsonMapperBuilderCustomizer strictJson() {
        return builder -> builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .withCoercionConfig(LogicalType.Textual, c -> c
                .setCoercion(CoercionInputShape.Integer,CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float,CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean,CoercionAction.Fail));
    }
}
