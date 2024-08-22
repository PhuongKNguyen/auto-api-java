package org.example;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class GetCountriesApiTests {
    @BeforeAll
    static void setUp(){
        RestAssured.baseURI ="http://localhost";
        RestAssured.port=3000;
    }

    @Test
    void verifyGetCountriesApiResponseSchema()
    {

        RestAssured.get("api/v1/countries")
                .then().assertThat().body(matchesJsonSchemaInClasspath("get-countries-json-schema.json"));

    }

    @Test
    void verifyGetCountriesApiReturnCorrectData()
    {
       String expected = """
            [
                {
                    "name": "Viet Nam",
                    "code": "VN"
                },
                {
                    "name": "USA",
                    "code": "US"
                },
                {
                    "name": "Canada",
                    "code": "CA"
                },
                {
                    "name": "UK",
                    "code": "GB"
                },
                {
                    "name": "France",
                    "code": "FR"
                },
                {
                    "name": "Japan",
                    "code": "JP"
                },
                {
                    "name": "India",
                    "code": "IN"
                },
                {
                    "name": "China",
                    "code": "CN"
                },
                {
                    "name": "Brazil",
                    "code": "BR"
                }
            ]
            """;
        Response actualResponse = RestAssured.get("api/v1/countries");
        String actualResponseBody = actualResponse.asString();
        assertThat(actualResponseBody,jsonEquals(expected).when(IGNORING_ARRAY_ORDER));


    }
}
