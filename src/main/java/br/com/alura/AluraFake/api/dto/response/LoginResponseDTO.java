package br.com.alura.AluraFake.api.dto.response;

public class LoginResponseDTO {
    private String jwtToken;
    public LoginResponseDTO(String jwtToken) { this.jwtToken = jwtToken; }
    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }
}
