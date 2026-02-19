package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate.UserAggregate;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.mapper.UserAggregateMapper;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.repository.UserJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJdbcTest
// @Testcontainers
// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// @Import({ UserJdbcAdapter.class, UserAggregateMapper.class })
class UserJdbcAdapterTest {

    // @Container
    // @ServiceConnection
    // static PostgreSQLContainer<?> postgres = new
    // PostgreSQLContainer<>("postgres:15-alpine");

    // @Autowired
    // private UserJdbcAdapter adapter;

    // @Autowired
    // private UserJdbcRepository repository;

    // @Test
    // void shouldSaveAndLoadUser() {
    // // Arrange
    // User user = User.builder()
    // .id(null) // Let DB generate? Or we generate?
    // // UseCase generates ID. Adapter expects ID.
    // .id(UUID.randomUUID())
    // .email("adapter-test@example.com")
    // .passwordHash("hashed")
    // .firstName("Adapter")
    // .firstLastname("Test")
    // .phoneNumber("999888777")
    // .username("adaptertest")
    // .subscriptionPlanId(null)
    // .build();

    // // Act
    // User saved = adapter.save(user);

    // // Assert
    // assertThat(saved).isNotNull();
    // assertThat(saved.getId()).isNotNull();
    // assertThat(repository.existsById(saved.getId())).isTrue();

    // // Verify Audit Fields (requires AuditorAware bean, which might NOT be in
    // // @DataJdbcTest slice)
    // // Usually we mock AuditorAware or verify it is null if not set up.
    // // We just verify data integrity here.
    // assertThat(saved.getEmail()).isEqualTo("adapter-test@example.com");
    // }

    // @Test
    // void shouldCheckExistence() {
    // // Arrange
    // UUID id = UUID.randomUUID();
    // UserAggregate agg = UserAggregate.builder()
    // .id(id)
    // .email("exist@example.com")
    // .passwordHash("hash")
    // .firstName("Exist")
    // .firstLastname("Test")
    // .phoneNumber("555444333")
    // .username("existuser")
    // .build();
    // repository.save(agg);

    // // Act & Assert
    // assertThat(adapter.existsByEmail("exist@example.com")).isTrue();
    // assertThat(adapter.existsByUsername("existuser")).isTrue();
    // assertThat(adapter.existsByPhoneNumber("555444333")).isTrue();
    // assertThat(adapter.existsByEmail("fail@example.com")).isFalse();
    // }

    // @Test
    // void shouldFindByEmail() {
    // // Arrange
    // UUID id = UUID.randomUUID();
    // String email = "findme@example.com";
    // UserAggregate agg = UserAggregate.builder()
    // .id(id)
    // .email(email)
    // .passwordHash("hash")
    // .firstName("Find")
    // .firstLastname("Me")
    // .phoneNumber("111222333")
    // .username("findme")
    // .build();
    // repository.save(agg);

    // // Act
    // java.util.Optional<User> found = adapter.findByEmail(email);

    // // Assert
    // assertThat(found).isPresent();
    // assertThat(found.get().getEmail()).isEqualTo(email);
    // assertThat(found.get().getId()).isEqualTo(id);
    // }
}
