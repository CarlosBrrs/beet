package com.beet.backend.shared.infrastructure.config;

import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;
import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.aggregate.SubscriptionAggregateFeatures;
import com.beet.backend.modules.unit.domain.model.UnitType;
import com.beet.backend.shared.domain.model.OperationMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.io.IOException;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableJdbcAuditing
@RequiredArgsConstructor
public class JdbcConfig extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
                new SubscriptionFeaturesWritingConverter(objectMapper),
                new SubscriptionFeaturesReadingConverter(objectMapper),
                new RestaurantSettingsWritingConverter(objectMapper),
                new RestaurantSettingsReadingConverter(objectMapper),
                new RolePermissionsWritingConverter(objectMapper),
                new RolePermissionsReadingConverter(objectMapper),
                new OperationModeWritingConverter(),
                new OperationModeReadingConverter(),
                new UnitTypeWritingConverter(),
                new UnitTypeReadingConverter());
    }

    @WritingConverter
    @RequiredArgsConstructor
    static class SubscriptionFeaturesWritingConverter implements Converter<SubscriptionAggregateFeatures, PGobject> {
        private final ObjectMapper objectMapper;

        @Override
        public PGobject convert(SubscriptionAggregateFeatures source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(json);
                return pgObject;
            } catch (JsonProcessingException | SQLException e) {
                return null;
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    static class SubscriptionFeaturesReadingConverter implements Converter<PGobject, SubscriptionAggregateFeatures> {
        private final ObjectMapper objectMapper;

        @Override
        public SubscriptionAggregateFeatures convert(PGobject source) {
            try {
                return objectMapper.readValue(source.getValue(), SubscriptionAggregateFeatures.class);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @WritingConverter
    @RequiredArgsConstructor
    static class RestaurantSettingsWritingConverter implements Converter<RestaurantSettings, PGobject> {
        private final ObjectMapper objectMapper;

        @Override
        public PGobject convert(RestaurantSettings source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(json);
                return pgObject;
            } catch (JsonProcessingException | SQLException e) {
                return null;
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    static class RestaurantSettingsReadingConverter implements Converter<PGobject, RestaurantSettings> {
        private final ObjectMapper objectMapper;

        @Override
        public RestaurantSettings convert(PGobject source) {
            try {
                return objectMapper.readValue(source.getValue(), RestaurantSettings.class);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @WritingConverter
    @RequiredArgsConstructor
    static class RolePermissionsWritingConverter implements Converter<Permissions, PGobject> {
        private final ObjectMapper objectMapper;

        @Override
        public PGobject convert(Permissions source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(json);
                return pgObject;
            } catch (JsonProcessingException | SQLException e) {
                return null;
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    static class RolePermissionsReadingConverter implements Converter<PGobject, Permissions> {
        private final ObjectMapper objectMapper;

        @Override
        public Permissions convert(PGobject source) {
            try {
                return objectMapper.readValue(source.getValue(), Permissions.class);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @WritingConverter
    static class OperationModeWritingConverter implements Converter<OperationMode, JdbcValue> {

        @Override
        public JdbcValue convert(OperationMode source) {

            return JdbcValue.of(source, JDBCType.OTHER);
        }

        @ReadingConverter
        static class OperationModeReadingConverter implements Converter<String, OperationMode> {

            @Override
            public OperationMode convert(String source) {
                return OperationMode.valueOf(source);
            }
        }
    }

    @ReadingConverter
    static class OperationModeReadingConverter implements Converter<String, OperationMode> {

        @Override
        public OperationMode convert(String source) {
            return OperationMode.valueOf(source);
        }
    }

    @WritingConverter
    static class UnitTypeWritingConverter implements Converter<UnitType, JdbcValue> {

        @Override
        public JdbcValue convert(UnitType source) {
            return JdbcValue.of(source, JDBCType.OTHER);
        }
    }

    @ReadingConverter
    static class UnitTypeReadingConverter implements Converter<String, UnitType> {

        @Override
        public UnitType convert(String source) {
            return UnitType.valueOf(source);
        }
    }

    @PostConstruct
    public void test() {
        System.out.println("JDBC Converters Loaded");
    }
}
