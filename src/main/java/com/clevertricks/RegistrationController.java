package com.clevertricks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Controller
public class RegistrationController {

    @Value("${keycloak.admin.url}")
    private String keycloakUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/register")
    public String showForm(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
            @RequestParam String password,
            Model model) {

        String token;
        try {
            token = fetchAdminToken();
        } catch (Exception e) {
            model.addAttribute("error", "Registration service unavailable");
            return "register";
        }

        String userJson = """
                {"username":"%s","enabled":true,"credentials":[{"type":"password","value":"%s","temporary":false}]}
                """.formatted(username, password);

        try {
            restClient.post()
                    .uri(keycloakUrl + "/admin/realms/" + realm + "/users")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            model.addAttribute("error", "Username already taken");
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed");
            return "register";
        }

        return "redirect:/login?registered";
    }

    private String fetchAdminToken() {
        String body = "grant_type=password&client_id=admin-cli"
                + "&username=" + adminUsername
                + "&password=" + adminPassword;

        var response = restClient.post()
                .uri(keycloakUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(java.util.Map.class);

        return (String) response.get("access_token");
    }
}
