# /api/me, PermissionEvaluator & GlobalExceptionHandler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/me` (DB-backed, Caffeine-cached), a custom `PermissionEvaluator` for `@PreAuthorize("hasPermission(null,'GET_USERS')")`, and a global exception handler that never exposes stack traces to clients.

**Architecture:** `MeController` reads `userId` from the JWT-issued `UsernamePasswordAuthenticationToken` principal (already set by `JwtAuthFilter`), delegates to `UserService.getUserInfo(userId)` which is `@Cacheable` for 5 minutes via Caffeine. `PermissionEvaluatorImpl` checks the `PERM_<X>` authorities set by `JwtAuthFilter`. `GlobalExceptionHandler` maps Spring Security + Jakarta exceptions to fixed JSON shapes with no stack trace.

**Tech Stack:** Spring Boot 4.0.5, Spring Security 6+, Spring Cache + Caffeine, JJWT 0.12.6, H2 (test), Jakarta Persistence, Lombok, Mockito.

---

## File Structure

**New files:**
- `src/main/java/com/daf360/portal/entity/Pays.java` — minimal JPA entity for Pays table (needed for `iso_code`)
- `src/main/java/com/daf360/portal/repository/PaysRepository.java` — `findById`
- `src/main/java/com/daf360/portal/dto/MeResponse.java` — safe response DTO (no sensitive fields)
- `src/main/java/com/daf360/portal/service/UserService.java` — `getUserInfo(Long)` with `@Cacheable`
- `src/main/java/com/daf360/portal/controller/MeController.java` — `GET /api/me`
- `src/main/java/com/daf360/portal/security/PermissionEvaluatorImpl.java` — checks `PERM_<X>` authorities
- `src/main/java/com/daf360/portal/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`
- `src/main/java/com/daf360/portal/config/CacheConfig.java` — Caffeine cache manager, 5-min TTL
- `src/test/java/com/daf360/portal/service/UserServiceTest.java` — unit tests, sensitive-field leak checks

**Modified files:**
- `pom.xml` — add `spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine`
- `src/main/java/com/daf360/portal/config/SecurityConfig.java` — add `MethodSecurityExpressionHandler` bean wired to `PermissionEvaluatorImpl`
- `src/main/java/com/daf360/portal/service/UserSyncService.java` — add `@CacheEvict` on `syncUser` to evict stale cache entry on re-login
- `src/main/java/com/daf360/portal/controller/UserApiController.java` — remove the old `/api/me` handler (now owned by `MeController`)

---

### Task 1: Caffeine cache dependencies and CacheConfig

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/daf360/portal/config/CacheConfig.java`

- [ ] **Step 1: Add Caffeine dependencies to pom.xml**

In `pom.xml`, add inside `<dependencies>` after the H2 test dependency:

```xml
<!-- Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

- [ ] **Step 2: Verify the dependency resolves**

Run:
```
mvn dependency:resolve -q
```
Expected: `BUILD SUCCESS` with no errors about missing artifacts.

- [ ] **Step 3: Write the CacheConfig**

Create `src/main/java/com/daf360/portal/config/CacheConfig.java`:

```java
package com.daf360.portal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userInfo");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
        );
        return manager;
    }
}
```

- [ ] **Step 4: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/daf360/portal/config/CacheConfig.java
git commit -m "feat: add Caffeine cache infrastructure (5-min userInfo cache)"
```

---

### Task 2: Pays entity and PaysRepository

**Files:**
- Create: `src/main/java/com/daf360/portal/entity/Pays.java`
- Create: `src/main/java/com/daf360/portal/repository/PaysRepository.java`

- [ ] **Step 1: Create Pays entity**

Create `src/main/java/com/daf360/portal/entity/Pays.java`:

```java
package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Pays")
public class Pays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Column(name = "nom_fr", length = 100)
    private String nomFr;
}
```

- [ ] **Step 2: Create PaysRepository**

Create `src/main/java/com/daf360/portal/repository/PaysRepository.java`:

```java
package com.daf360.portal.repository;

import com.daf360.portal.entity.Pays;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaysRepository extends JpaRepository<Pays, Long> {
}
```

- [ ] **Step 3: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/daf360/portal/entity/Pays.java \
        src/main/java/com/daf360/portal/repository/PaysRepository.java
git commit -m "feat: add Pays entity and PaysRepository for iso_code lookup"
```

---

### Task 3: MeResponse DTO

**Files:**
- Create: `src/main/java/com/daf360/portal/dto/MeResponse.java`

- [ ] **Step 1: Create MeResponse**

Create `src/main/java/com/daf360/portal/dto/MeResponse.java`:

```java
package com.daf360.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Safe user info — NEVER include password, refreshToken, azureOid, ms365* tokens
@Data
@Builder
public class MeResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String azureUpn;
    private Long roleId;
    private String roleName;
    private List<String> permissions;
    private Long paysId;
    private String isoCode;
    private String employeeId;
}
```

- [ ] **Step 2: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/daf360/portal/dto/MeResponse.java
git commit -m "feat: add MeResponse DTO (no sensitive fields)"
```

---

### Task 4: UserService with caching + UserSyncService cache eviction

**Files:**
- Create: `src/main/java/com/daf360/portal/service/UserService.java`
- Modify: `src/main/java/com/daf360/portal/service/UserSyncService.java`

- [ ] **Step 1: Create UserService**

Create `src/main/java/com/daf360/portal/service/UserService.java`:

```java
package com.daf360.portal.service;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PaysRepository paysRepository;

    @Cacheable(value = "userInfo", key = "#userId")
    @Transactional(readOnly = true)
    public MeResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String isoCode = paysRepository.findById(user.getPaysId())
            .map(Pays::getIsoCode)
            .orElse(null);

        Role role = user.getRole();
        List<String> permissions = role != null ? role.getPermissions() : List.of();

        return MeResponse.builder()
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .azureUpn(user.getAzureUpn())
            .roleId(role != null ? role.getId() : null)
            .roleName(role != null ? role.getFrenchName() : null)
            .permissions(permissions)
            .paysId(user.getPaysId())
            .isoCode(isoCode)
            .employeeId(user.getEmployeeId())
            .build();
        // Sensitive fields intentionally omitted:
        // password, refreshToken, azureOid, ms365AccessToken, ms365RefreshToken
    }
}
```

- [ ] **Step 2: Add @CacheEvict to UserSyncService.syncUser**

Open `src/main/java/com/daf360/portal/service/UserSyncService.java`. Find the `syncUser` method signature. Add `@CacheEvict(value = "userInfo", key = "#result.id")` before the existing `@Transactional` annotation. The result is:

```java
import org.springframework.cache.annotation.CacheEvict;

// ...

@CacheEvict(value = "userInfo", key = "#result.id")
@Transactional
public User syncUser(OidcIdToken idToken, String ms365AccessToken, String ms365RefreshToken) {
    // ... existing implementation unchanged ...
}
```

Only add the import and the annotation. Do NOT change any existing logic.

- [ ] **Step 3: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/daf360/portal/service/UserService.java \
        src/main/java/com/daf360/portal/service/UserSyncService.java
git commit -m "feat: add UserService with 5-min Caffeine cache; evict on re-login"
```

---

### Task 5: MeController and UserApiController cleanup

**Files:**
- Create: `src/main/java/com/daf360/portal/controller/MeController.java`
- Modify: `src/main/java/com/daf360/portal/controller/UserApiController.java`

Background: `JwtAuthFilter` stores `claims.getSubject()` (the userId String) as the `Authentication` principal. `MeController` reads it directly from `Authentication`. The old `UserApiController.me()` used `@AuthenticationPrincipal PortalUser` which is null for JWT-authenticated requests — it must be removed.

- [ ] **Step 1: Create MeController**

Create `src/main/java/com/daf360/portal/controller/MeController.java`:

```java
package com.daf360.portal.controller;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        MeResponse response = userService.getUserInfo(userId);
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: Remove the old /api/me handler from UserApiController**

Open `src/main/java/com/daf360/portal/controller/UserApiController.java`. Replace the entire file content with the following (keep only `publicConfig`, drop `me()` and associated imports):

```java
package com.daf360.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserApiController {

    @GetMapping("/public/config")
    public ResponseEntity<Object> publicConfig() {
        return ResponseEntity.ok(Map.of(
            "loginUrl",   "/oauth2/authorization/azure",
            "logoutUrl",  "/logout",
            "appVersion", "1.0.0"
        ));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/daf360/portal/controller/MeController.java \
        src/main/java/com/daf360/portal/controller/UserApiController.java
git commit -m "feat: add MeController for GET /api/me; remove stale handler from UserApiController"
```

---

### Task 6: PermissionEvaluatorImpl and SecurityConfig wiring

**Files:**
- Create: `src/main/java/com/daf360/portal/security/PermissionEvaluatorImpl.java`
- Modify: `src/main/java/com/daf360/portal/config/SecurityConfig.java`

Background: `JwtAuthFilter` already sets authorities as `PERM_GET_USERS`, `PERM_RH_READ`, etc. The `PermissionEvaluatorImpl` checks those authorities so `@PreAuthorize("hasPermission(null,'GET_USERS')")` on any controller method will work. The evaluator must be wired into Spring Security's method expression handler via a `@Bean` in `SecurityConfig`.

- [ ] **Step 1: Create PermissionEvaluatorImpl**

Create `src/main/java/com/daf360/portal/security/PermissionEvaluatorImpl.java`:

```java
package com.daf360.portal.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) return false;
        String required = "PERM_" + permission.toString();
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(required));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        return hasPermission(authentication, (Object) null, permission);
    }
}
```

- [ ] **Step 2: Wire PermissionEvaluatorImpl into SecurityConfig**

Open `src/main/java/com/daf360/portal/config/SecurityConfig.java`. Add the following imports and bean at the END of the class, just before the closing `}`:

Add imports (after the existing import block):
```java
import com.daf360.portal.security.PermissionEvaluatorImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
```

Add this bean method inside the class body (before the final `}`):
```java
@Bean
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
        PermissionEvaluator permissionEvaluator) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(permissionEvaluator);
    return handler;
}
```

Note: The method is `static` — this is required by Spring Security 6 so that the bean is created early enough to participate in the method security configuration.

- [ ] **Step 3: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/daf360/portal/security/PermissionEvaluatorImpl.java \
        src/main/java/com/daf360/portal/config/SecurityConfig.java
git commit -m "feat: add PermissionEvaluator for hasPermission(null,'X') @PreAuthorize pattern"
```

---

### Task 7: GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/daf360/portal/exception/GlobalExceptionHandler.java`

Background: `@RestControllerAdvice` intercepts exceptions thrown from controllers and `@Service` methods during request processing. Spring Security filter-level 401 for `/api/**` is already handled by `HttpStatusEntryPoint(UNAUTHORIZED)` in `SecurityConfig` and does NOT reach here. This handler covers: `AccessDeniedException` from `@PreAuthorize` (thrown inside DispatcherServlet), `EntityNotFoundException` from `UserService`, and all unchecked exceptions.

- [ ] **Step 1: Create the exception package and GlobalExceptionHandler**

Create `src/main/java/com/daf360/portal/exception/GlobalExceptionHandler.java`:

```java
package com.daf360.portal.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "FORBIDDEN"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "UNAUTHORIZED"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        String resource = ex.getMessage() != null ? ex.getMessage() : "unknown";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NOT_FOUND", "resource", resource));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleInternal(Exception ex,
                                                              HttpServletRequest request) {
        String ref = UUID.randomUUID().toString();
        log.error("Unhandled exception [ref={}] {} {}: {}",
            ref, request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "INTERNAL_ERROR", "ref", ref));
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/daf360/portal/exception/GlobalExceptionHandler.java
git commit -m "feat: add GlobalExceptionHandler (401/403/404/500 — no stack trace to client)"
```

---

### Task 8: UserService unit tests

**Files:**
- Create: `src/test/java/com/daf360/portal/service/UserServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/daf360/portal/service/UserServiceTest.java`:

```java
package com.daf360.portal.service;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PaysRepository paysRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, paysRepository);
    }

    private User buildUser(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setFullName("Alice Martin");
        u.setEmail("alice@corp.com");
        u.setAzureUpn("alice@corp.onmicrosoft.com");
        u.setAzureOid("oid-secret");
        u.setPassword("hashed-secret");
        u.setRefreshToken("refresh-uuid-secret");
        u.setMs365AccessToken("ms-access-secret");
        u.setMs365RefreshToken("ms-refresh-secret");
        u.setEmployeeId("EMP001");
        u.setPaysId(1L);
        u.setRole(role);
        return u;
    }

    @Test
    void getUserInfo_returnsCorrectPublicFields() {
        Role role = new Role();
        role.setId(2L);
        role.setFrenchName("RH Manager");
        role.setPermissions(List.of("RH_READ", "GET_USERS"));

        Pays pays = new Pays();
        pays.setId(1L);
        pays.setIsoCode("TN");

        when(userRepository.findById(7L)).thenReturn(Optional.of(buildUser(7L, role)));
        when(paysRepository.findById(1L)).thenReturn(Optional.of(pays));

        MeResponse response = userService.getUserInfo(7L);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getFullName()).isEqualTo("Alice Martin");
        assertThat(response.getEmail()).isEqualTo("alice@corp.com");
        assertThat(response.getAzureUpn()).isEqualTo("alice@corp.onmicrosoft.com");
        assertThat(response.getRoleId()).isEqualTo(2L);
        assertThat(response.getRoleName()).isEqualTo("RH Manager");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("RH_READ", "GET_USERS");
        assertThat(response.getPaysId()).isEqualTo(1L);
        assertThat(response.getIsoCode()).isEqualTo("TN");
        assertThat(response.getEmployeeId()).isEqualTo("EMP001");
    }

    @Test
    void getUserInfo_neverExposesSensitiveFields() throws Exception {
        Role role = new Role();
        role.setId(1L);
        role.setFrenchName("Collaborateur");
        role.setPermissions(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, role)));
        when(paysRepository.findById(1L)).thenReturn(Optional.empty());

        MeResponse response = userService.getUserInfo(1L);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);

        assertThat(json).doesNotContain("hashed-secret");
        assertThat(json).doesNotContain("refresh-uuid-secret");
        assertThat(json).doesNotContain("ms-access-secret");
        assertThat(json).doesNotContain("ms-refresh-secret");
        assertThat(json).doesNotContain("oid-secret");
    }

    @Test
    void getUserInfo_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserInfo(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void getUserInfo_noRole_returnsNullRoleFieldsAndEmptyPermissions() {
        User user = buildUser(3L, null);
        user.setRole(null);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(paysRepository.findById(1L)).thenReturn(Optional.empty());

        MeResponse response = userService.getUserInfo(3L);

        assertThat(response.getRoleId()).isNull();
        assertThat(response.getRoleName()).isNull();
        assertThat(response.getPermissions()).isEmpty();
        assertThat(response.getIsoCode()).isNull();
    }
}
```

- [ ] **Step 2: Run tests — verify they FAIL (UserService doesn't exist yet in test classpath)**

Wait — UserService was already created in Task 4. The tests should compile and pass now. Run:

```
mvn test -pl . -Dtest=UserServiceTest -q
```
Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 3: Run the full test suite**

Run:
```
mvn test -q
```
Expected output:
```
Tests run: 1, ...  -- com.daf360.portal.PortalApplicationTests
Tests run: 4, ...  -- com.daf360.portal.service.JwtTokenServiceTest
Tests run: 4, ...  -- com.daf360.portal.service.UserServiceTest
Tests run: 4, ...  -- com.daf360.portal.service.UserSyncServiceTest
Tests run: 13, Failures: 0, Errors: 0
BUILD SUCCESS
```

If `PortalApplicationTests` fails due to a new H2 DDL issue (e.g., missing `Pays` table column), the error will say something like `Schema-validation: missing table`. Since `ddl-auto: create-drop` is set in the test `application.yml`, H2 creates all tables from entity definitions automatically — no DDL fix needed.

If any test fails, read the error message carefully and fix only the failing assertion before proceeding.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/daf360/portal/service/UserServiceTest.java
git commit -m "test: add UserServiceTest — verifies correct fields and no sensitive field leaks"
```

---

## Self-Review

**Spec coverage check:**
- ✅ `GET /api/me` with required fields: `MeController` + `UserService.getUserInfo`
- ✅ Load roleId, roleName, permissions from DB (Role entity with `@ElementCollection`)
- ✅ Load pays.iso_code via `PaysRepository`
- ✅ 5-minute Caffeine cache per userId: `@Cacheable("userInfo")` in `UserService`, `CacheConfig`
- ✅ NEVER include sensitive fields: `MeResponse` has no password/refresh_token/azure_oid/ms365_* fields
- ✅ PermissionEvaluator: `PermissionEvaluatorImpl` + wired in `SecurityConfig`
- ✅ `@PreAuthorize("hasPermission(null,'GET_USERS')")` pattern works via `PERM_GET_USERS` authority check
- ✅ 401 → `{"error":"UNAUTHORIZED"}`: `GlobalExceptionHandler`
- ✅ 403 → `{"error":"FORBIDDEN"}`: `GlobalExceptionHandler`
- ✅ 404 → `{"error":"NOT_FOUND","resource":"..."}`: `GlobalExceptionHandler`
- ✅ 500 → `{"error":"INTERNAL_ERROR","ref":"<UUID>"}` + server-side log: `GlobalExceptionHandler`
- ✅ Unit tests for UserService: 4 tests including sensitive-field leak verification
- ✅ Cache eviction on re-login: `@CacheEvict` in `UserSyncService.syncUser`
