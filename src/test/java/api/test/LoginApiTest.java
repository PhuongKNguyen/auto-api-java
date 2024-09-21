package api.test;

import api.model.login.LoginInput;
import api.model.login.LoginResponse;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsBlankString.blankString;

public class LoginApiTest {
    private static final String LOGIN_PATH = "api/login";
    private static final String USER_STAFF_NAME = "staff";
    private static final String USER_STAFF_PASSWORD = "1234567890";
    private static final String ERROR_MESSAGE_INVALID_CREDENTIALS = "Invalid credentials";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
    }
    //1.happy case
    //2. Valid username, Invalid password
    //3. InValid username, Valid password
    //4. InValid username, InValid password
    //5. Required field - null
    //6. Required field - empty

    //case 1:happy case
    @Test
    void verifyStaffLoginSuccessful() {
        LoginInput loginInput = new LoginInput(USER_STAFF_NAME, USER_STAFF_PASSWORD);
        Response actualResponse = RestAssured.given().log().all()
                .header("Content-Type", "application/json")
                .body(loginInput).post(LOGIN_PATH);
        assertThat(actualResponse.statusCode(), equalTo(200));
        //Need to verify token is not null and timeout is 120000
        LoginResponse loginResponse = actualResponse.as(LoginResponse.class);
        assertThat(loginResponse.getToken(), not(blankString()));
        assertThat(loginResponse.getTimeout(), equalTo(120000));
    }

    //case unhappy
    static Stream<Arguments> loginProvider() {

        return Stream.of(

                //2. Valid username, Invalid password
                Arguments.of(new LoginInput(USER_STAFF_NAME, "1"), 401, ERROR_MESSAGE_INVALID_CREDENTIALS),
                //3. InValid username, Valid password
                Arguments.of(new LoginInput("staff1", USER_STAFF_PASSWORD), 401, ERROR_MESSAGE_INVALID_CREDENTIALS),
                //4. InValid username, InValid password
                Arguments.of(new LoginInput("1", "1"), 401, ERROR_MESSAGE_INVALID_CREDENTIALS),
                //5. Required field - null
                Arguments.of(new LoginInput(null, USER_STAFF_PASSWORD), 401, ERROR_MESSAGE_INVALID_CREDENTIALS),
                //6. Required field - empty
                Arguments.of(new LoginInput(USER_STAFF_NAME, ""), 401, ERROR_MESSAGE_INVALID_CREDENTIALS)

        );
    }

    @ParameterizedTest
    @MethodSource("loginProvider")
    void verifyLoginUnhappyCases(LoginInput loginInput, int expectedStatusCode, String expectedErrorMessage) {
        Response actualResponse = RestAssured.given().log().all().header("Content-Type", "application/json").body(loginInput).post(LOGIN_PATH);
        assertThat(actualResponse.statusCode(), equalTo(expectedStatusCode));
        LoginResponse loginResponse = actualResponse.as(LoginResponse.class);
        assertThat(loginResponse.getMessage(), equalTo(expectedErrorMessage));
    }
}
