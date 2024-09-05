package api.model.country;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CountryVersionThree {
    private String name;
    private String code;
    private float gdp;
    @JsonProperty("private")
    private int fieldPrivate;
}
