package org.leeroy.authenticator.service;

import io.smallrye.mutiny.Uni;
import org.leeroy.authenticator.exception.InvalidLoginAttemptException;
import org.leeroy.authenticator.exception.WaitBeforeTryingLoginAgainException;
import org.leeroy.authenticator.resource.request.AuthenticateRequest;

public interface AccountService {

    Uni<String> authenticate(AuthenticateRequest authenticateRequest) throws InvalidLoginAttemptException,
            WaitBeforeTryingLoginAgainException;

    Uni<Void> forgotPassword(String ipAddress, String device, String username);

    void changePassword(String username, String oldPassword, String newPassword);

    Uni<String> createAccount(String username, String password);

    Uni<String> createAccount(String username);

    void deleteAccount(String username, String password);
}
