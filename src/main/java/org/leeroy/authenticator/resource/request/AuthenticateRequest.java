package org.leeroy.authenticator.resource.request;

import lombok.Data;

@Data
public class AuthenticateRequest {

    private String ipAddress;
    private String device;
    private String username;
    private String password;
}