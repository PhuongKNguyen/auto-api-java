package api.model.login;

import api.model.country.CountryVersionTwo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginInput {
    private String username;
    private String password;

}
