package api.service;

import api.model.login.LoginInput;
import api.model.login.LoginResponse;
import api.model.user.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class UserService {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CREATE_USER_PATH = "api/user";
    private static final String LOGIN_USER_PATH = "api/login";
    private static final String GET_USER_PATH = "api/user/{id}";
    private static final String DELETE_USER_PATH = "api/user/{id}";

    public static String loginAndGetToken(LoginInput loginInput) {
        Response response = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(loginInput)
                .post(LOGIN_USER_PATH);

        return "Bearer " + response.as(LoginResponse.class).getToken();
    }

    public static Response createUser(User user, String token) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .header(AUTHORIZATION_HEADER, token)
                .body(user)
                .post(CREATE_USER_PATH);
    }

    public static Response getUser(String userId, String token) {
        return RestAssured.given()
                .header(AUTHORIZATION_HEADER, token)
                .pathParam("id", userId)
                .get(GET_USER_PATH);
    }

    public static void deleteUser(String userId, String token) {
        RestAssured.given()
                .header(AUTHORIZATION_HEADER, token)
                .pathParam("id", userId)
                .delete(DELETE_USER_PATH);
    }
}
