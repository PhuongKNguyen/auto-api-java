package api.test;

import api.model.login.LoginInput;
import api.model.login.LoginResponse;
import api.model.user.*;
import api.model.user.dto.DbUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.out;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsBlankString.blankString;

public class CreateUserTest {
    private static String TOKEN;
    private static List<String> createdUserIds = new ArrayList<>();
    private static final String CREATE_USER_PATH = "api/user";
    private static final String LOGIN_USER_PATH = "api/login";
    private static final String DELETE_USER_PATH = "api/user/{id}";
    private static final String GET_USER_PATH = "api/user/{id}";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static long TIMEOUT = -1;
    private static long TIME_BEFORE_GET_TOKEN = -1;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
        final StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .build();
        try {
            SessionFactory sessionFactory =
                    new MetadataSources(registry)
                            .addAnnotatedClass(DbUser.class)
                            .buildMetadata()
                            .buildSessionFactory();
        } catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we
            // had trouble building the SessionFactory so destroy it manually.
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @BeforeEach
        //get token
    void beforeEach() {
        if (TIMEOUT == -1 || (System.currentTimeMillis() - TIME_BEFORE_GET_TOKEN) > TIMEOUT * 0.8) {
            LoginInput loginInput = new LoginInput("staff", "1234567890");
            TIME_BEFORE_GET_TOKEN = System.currentTimeMillis();
            Response actualResponse = RestAssured.given().log().all()
                    .header("Content-Type", "application/json")
                    .body(loginInput)
                    .post(LOGIN_USER_PATH);
            assertThat(actualResponse.statusCode(), equalTo(200));
            LoginResponse loginResponse = actualResponse.as(LoginResponse.class);
            assertThat(loginResponse.getToken(), not(blankString()));
            TOKEN = "Bearer ".concat(loginResponse.getToken());
            TIMEOUT = loginResponse.getTimeout();
        }
    }

    @Test
    void verifyStaffCreateUserSuccessfully() {
        Address address = Address.getDefault();
        User<Address> user = User.getDefault();

        String randomEmail = String.format("auto_api_%s@abc.com", System.currentTimeMillis());
        user.setEmail(randomEmail);
        user.setAddresses(List.of(address));

        Instant beforeExecution = Instant.now();

        Response createUserResponse = RestAssured.given().log().all()
                .header("Content-Type", "application/json")
                .header(AUTHORIZATION_HEADER, TOKEN)
                .body(user)
                .post(CREATE_USER_PATH);
        out.printf("Create user response: %s%n", createUserResponse.asString());
        assertThat(createUserResponse.statusCode(), equalTo(200));

        CreateUserResponse actual = createUserResponse.as(CreateUserResponse.class);
        createdUserIds.add(actual.getId());
        assertThat(actual.getId(), not(blankString()));
        assertThat(actual.getMessage(), equalTo("Customer created"));

        Response getCreatedUserResponse = RestAssured.given().log().all()
                .header(AUTHORIZATION_HEADER, TOKEN)
                .pathParam("id", actual.getId())
                .get(GET_USER_PATH);
        out.printf("Create user response: %s%n", getCreatedUserResponse.asString());
        assertThat(getCreatedUserResponse.statusCode(), equalTo(200));

        ObjectMapper mapper = new ObjectMapper();
        GetUserResponse<AddressResponse> expectedUser = mapper.convertValue(user, new TypeReference<GetUserResponse<AddressResponse>>() {
        });
        expectedUser.setId(actual.getId());
        expectedUser.getAddresses().get(0).setCustomerId(actual.getId());

        String actualGetCreated = getCreatedUserResponse.asString();
        assertThat(actualGetCreated, jsonEquals(expectedUser).whenIgnoringPaths("createdAt", "updatedAt",
                "addresses[*].id", "addresses[*].createdAt", "addresses[*].updatedAt"));
        GetUserResponse<AddressResponse> actualGetCreatedModel = getCreatedUserResponse.as(new TypeRef<GetUserResponse<AddressResponse>>() {
        });

        Instant userCreatedAt = Instant.parse(actualGetCreatedModel.getCreatedAt());
        datetimeVerifier(beforeExecution, userCreatedAt);
        Instant userUpdatedAt = Instant.parse(actualGetCreatedModel.getUpdatedAt());
        datetimeVerifier(beforeExecution, userUpdatedAt);

        actualGetCreatedModel.getAddresses().forEach(actualAddress -> {
            assertThat(actualAddress.getId(), not(blankString()));
            Instant addressCreatedAt = Instant.parse(actualAddress.getCreatedAt());
            datetimeVerifier(beforeExecution, addressCreatedAt);
            Instant addressUpdatedAt = Instant.parse(actualAddress.getUpdatedAt());
            datetimeVerifier(beforeExecution, addressUpdatedAt);
        });
    }

    @Test
    void verifyStaffCreateUserWithMultipleAddressesSuccessfully() {
        Address address1 = Address.getDefault();
        Address address2 = Address.getDefault();
        address2.setStreetNumber("456");
        User<Address> user = User.getDefault();

        String randomEmail = String.format("auto_api_%s@abc.com", System.currentTimeMillis());
        user.setEmail(randomEmail);
        user.setAddresses(List.of(address1, address2));

        Instant beforeExecution = Instant.now();

        Response createUserResponse = RestAssured.given().log().all()
                .header("Content-Type", "application/json")
                .header(AUTHORIZATION_HEADER, TOKEN)
                .body(user)
                .post(CREATE_USER_PATH);
        out.printf("Create user response: %s%n", createUserResponse.asString());
        assertThat(createUserResponse.statusCode(), equalTo(200));

        CreateUserResponse actual = createUserResponse.as(CreateUserResponse.class);
        createdUserIds.add(actual.getId());
        assertThat(actual.getId(), not(blankString()));
        assertThat(actual.getMessage(), equalTo("Customer created"));

        Response getCreatedUserResponse = RestAssured.given().log().all()
                .header(AUTHORIZATION_HEADER, TOKEN)
                .pathParam("id", actual.getId())
                .get(GET_USER_PATH);
        out.printf("Create user response: %s%n", getCreatedUserResponse.asString());
        assertThat(getCreatedUserResponse.statusCode(), equalTo(200));

        ObjectMapper mapper = new ObjectMapper();
        GetUserResponse<AddressResponse> expectedUser = mapper.convertValue(user, new TypeReference<GetUserResponse<AddressResponse>>() {
        });
        expectedUser.setId(actual.getId());
        expectedUser.getAddresses().get(0).setCustomerId(actual.getId());
        expectedUser.getAddresses().get(1).setCustomerId(actual.getId());

        String actualGetCreated = getCreatedUserResponse.asString();
        assertThat(actualGetCreated, jsonEquals(expectedUser).whenIgnoringPaths("createdAt", "updatedAt",
                "addresses[*].id", "addresses[*].createdAt", "addresses[*].updatedAt"));
        GetUserResponse<AddressResponse> actualGetCreatedModel = getCreatedUserResponse.as(new TypeRef<GetUserResponse<AddressResponse>>() {
        });

        Instant userCreatedAt = Instant.parse(actualGetCreatedModel.getCreatedAt());
        datetimeVerifier(beforeExecution, userCreatedAt);
        Instant userUpdatedAt = Instant.parse(actualGetCreatedModel.getUpdatedAt());
        datetimeVerifier(beforeExecution, userUpdatedAt);

        actualGetCreatedModel.getAddresses().forEach(actualAddress -> {
            assertThat(actualAddress.getId(), not(blankString()));
            Instant addressCreatedAt = Instant.parse(actualAddress.getCreatedAt());
            datetimeVerifier(beforeExecution, addressCreatedAt);
            Instant addressUpdatedAt = Instant.parse(actualAddress.getUpdatedAt());
            datetimeVerifier(beforeExecution, addressUpdatedAt);
        });
    }

    static Stream<Arguments> validationUserProvider() throws JsonProcessingException {
        List<Arguments> argumentsList = new ArrayList<>();
        User<Address> user = User.getDefault();
        user.setFirstName(null);
        argumentsList.add(Arguments.arguments("Verify API return 400 when firstname is null", user));
        user =User.getDefault();
        user.setFirstName("");
        argumentsList.add(Arguments.arguments("Verify API return 400 when firstname is empty", user));
        return argumentsList.stream();
    }

    @ParameterizedTest
    @MethodSource("validationUserProvider")
    void verifyRequiredFieldsWhenCreatingUser(String testcase, User<Address> user) {
        System.out.println();

    }


    private void datetimeVerifier(Instant timeBeforeExecution, Instant actualTime) {
        assertThat(actualTime.isAfter(timeBeforeExecution), equalTo(true));
        assertThat(actualTime.isBefore(Instant.now()), equalTo(true));
    }


    @AfterAll
    static void tearDown() {
        //clean data
        createdUserIds.forEach(id -> {
            RestAssured.given().log().all()
                    .header(AUTHORIZATION_HEADER, TOKEN).
                    pathParam("id", id).
                    delete(DELETE_USER_PATH);
        });
    }
}
