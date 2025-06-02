package br.com.alura.AluraFake.api.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDTO {
    private String jwtToken;
    public LoginResponseDTO() {}
    public LoginResponseDTO(String jwtToken) { this.jwtToken = jwtToken; }
    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }
}
