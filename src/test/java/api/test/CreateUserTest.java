package api.test;

import api.model.login.LoginInput;
import api.model.login.LoginResponse;
import api.model.user.*;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsBlankString.blankString;

public class CreateUserTest {
    private static String token;
    private static List<String> createdUserIds = new ArrayList<>();
    private static final String CREATE_USER_PATH = "api/user";
    private static final String LOGIN_USER_PATH = "api/login";
    private static final String DELETE_USER_PATH = "api/user/{id}";
    private static final String GET_USER_PATH = "api/user/{id}";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
        // get token
        LoginInput loginInput = new LoginInput("staff", "1234567890");
        Response actualResponse = RestAssured.given().log().all()
                        .header("Content-Type", "application/json")
                        .body(loginInput)
                        .post(LOGIN_USER_PATH);
        assertThat(actualResponse.statusCode(), equalTo(200));
        LoginResponse loginResponse = actualResponse.as(LoginResponse.class);
        assertThat(loginResponse.getToken(), not(blankString()));
        token = "Bearer ".concat(loginResponse.getToken());
    }

    //Happy Case
    @Test
    void verifyStaffCreateUserSuccessfully() {
        String randomEmail = String.format("auto_api_%s@abc.com", System.currentTimeMillis());
        System.out.println(randomEmail);

        Address address = Address.getDefault();

        User user = User.getDefault();
        user.setAddresses(List.of(address));
//        user.setEmail(randomEmail); // issue 1

        //Store the moment before execution
        Instant beforeExecution = Instant.now();

        Response createUserResponse = RestAssured.given().log().all()
                .header("Content-Type", "application/json")
                .header(AUTHORIZATION_HEADER, token)
                .body(user)
                .post(CREATE_USER_PATH);
        //verify user created successfully
        System.out.printf("Create user response:%s%n", createUserResponse.asString());
        assertThat(createUserResponse.statusCode(), equalTo(200));
        CreateUserResponse actual = createUserResponse.as(CreateUserResponse.class);

        createdUserIds.add(actual.getId()); // why ?

        assertThat(actual.getId(), not(blankString())); //after create user,id will be displayed,verify it is not null
        assertThat(actual.getMessage(), equalTo("Customer created"));

        //get api/user/{id}
        Response getCreatedUserResponse = RestAssured.given().log().all()
                .header(AUTHORIZATION_HEADER, token)
                .pathParam("id", actual.getId())
                .get(GET_USER_PATH);
        System.out.printf("Create user response: %s%n", getCreatedUserResponse.asString());

        assertThat(getCreatedUserResponse.statusCode(), equalTo(200));

        AddressResponse expectedAddress = new AddressResponse();
//        expectedAddress.setCustomerId(actual.getId()); // issue 2
        expectedAddress.setStreetNumber("123");
        expectedAddress.setStreet("Main St");
        expectedAddress.setWard("Ward 1");
        expectedAddress.setDistrict("District 1");
        expectedAddress.setCity("Thu Duc");
        expectedAddress.setState("Ho Chi Minh");
        expectedAddress.setZip("70000");
        expectedAddress.setCountry("VN");

        GetUserResponse<AddressResponse> expectedUser = new GetUserResponse<AddressResponse>();
        expectedUser.setId(actual.getId());
        expectedUser.setFirstName("John");
        expectedUser.setLastName("Doe");
        expectedUser.setMiddleName("Smith");
        expectedUser.setBirthday("01-23-2000");
        expectedUser.setEmail(randomEmail);
        expectedUser.setPhone("0987654322");
        expectedUser.setAddresses(List.of(expectedAddress));

        String actualGetCreated = getCreatedUserResponse.asString();
        assertThat(actualGetCreated, jsonEquals(expectedUser).
                whenIgnoringPaths("createdAt", "updatedAt", "addresses[*].id", "addresses[*].createdAt", "addresses[*].updatedAt"));

        GetUserResponse<AddressResponse> actualGetCreatedModel = getCreatedUserResponse.as(new TypeRef<GetUserResponse<AddressResponse>>() {
        });
        Instant userCreatedAt = Instant.parse(actualGetCreatedModel.getCreatedAt());
        datetimeVerifier(beforeExecution, userCreatedAt);
        actualGetCreatedModel.getAddresses().forEach(actualAddress -> {
            assertThat(actualAddress.getId(), not(blankString()));
            Instant addressCreatedAt = Instant.parse(actualAddress.getCreatedAt());
            datetimeVerifier(beforeExecution, addressCreatedAt);
            Instant addressUpdatedAt = Instant.parse(actualAddress.getUpdatedAt());
            datetimeVerifier(beforeExecution, addressUpdatedAt);
        });
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
                    .header(AUTHORIZATION_HEADER, token).
                    pathParam("id", id).
                    delete(DELETE_USER_PATH);
        });
    }
}
