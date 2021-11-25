package org.leeroy.authenticator.service.impl;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.leeroy.authenticator.exception.InvalidLoginAttemptException;
import org.leeroy.authenticator.exception.WaitBeforeTryingLoginAgainException;
import org.leeroy.authenticator.model.BlockedAccess;
import org.leeroy.authenticator.repository.AccountRepository;
import org.leeroy.authenticator.resource.request.AuthenticateRequest;
import org.leeroy.authenticator.service.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class AccountServiceImpl implements AccountService {

    @Inject
    BlockedAccessService blockedIPService;

    @Inject
    LoginAttemptService loginAttemptService;

    @Inject
    PasswordService passwordService;

    @Inject
    EmailService emailService;

    @Inject
    BlockedAccessService blockedAccessService;

    @Inject
    AccountRepository accountRepository;


    private final String FORGOT_PASSWORD_ATTEMPT_MESSAGE = "We sent you a link by e-mail so you can set the password";
    private final String BLOCKED_EXCEPTION_MESSAGE = "You have to wait a while before you try again";

    @Override
    public Uni<Long> authenticate(AuthenticateRequest authenticateRequest) throws InvalidLoginAttemptException,
            WaitBeforeTryingLoginAgainException {

        AtomicReference<Uni<Long>> accountId = null;

        blockedIPService.isBlocked(authenticateRequest.getIpAddress(), authenticateRequest.getDevice())
                .onItem()
                .invoke(Unchecked.consumer(isBlocked -> {
                    if (isBlocked) {
                        Log.error("Invalid attempt login");
                        throw new WaitBeforeTryingLoginAgainException();
                    } else {
                        if (isUsernameAndPasswordValid(authenticateRequest.getUsername(), authenticateRequest.getPassword())) {
                            loginAttemptService.createLoginAttempt(authenticateRequest.getIpAddress(), authenticateRequest.getDevice(),
                                    authenticateRequest.getChannel(),
                                    authenticateRequest.getClient(),
                                    authenticateRequest.getUsername()
                            );

                            accountId.set(accountRepository.find("username", authenticateRequest.getUsername())
                                    .firstResult()
                                    .onItem()
                                    .ifNotNull()
                                    .transform(account -> account.getId()));
                        } else {
                            loginAttemptService.getLoginAttempts(authenticateRequest.getIpAddress(),
                                            authenticateRequest.getDevice())
                                    .onItem()
                                    .transform(count -> count > 15)
                                    .subscribe()
                                    .with(isBlockedLoginAttempt -> {
                                        if (isBlockedLoginAttempt) {
                                            blockedAccessService.blockIP(BlockedAccess.builder()
                                                    .ipAddress(authenticateRequest.getIpAddress())
                                                    .device(authenticateRequest.getDevice())
                                                    .build());
                                        }
                                    });

                            loginAttemptService.createLoginAttempt(authenticateRequest.getIpAddress(), authenticateRequest.getDevice(),
                                    authenticateRequest.getChannel(),
                                    authenticateRequest.getClient(),
                                    authenticateRequest.getUsername()
                            );

                            throw new InvalidLoginAttemptException();
                        }
                    }
                }));

        return accountId.get();
    }

    @Override
    public Uni<Void> forgotPassword(String ipAddress, String device, String username) {
        return blockedIPService.isBlocked(ipAddress, device).onItem().invoke(isBlocked -> {
                    if (isBlocked) {
                        Log.error(BLOCKED_EXCEPTION_MESSAGE);
                        throw new BadRequestException(BLOCKED_EXCEPTION_MESSAGE);
                    }
                })
                .onItem().call(() ->
                        accountRepository.hasUser(username).onItem().invoke(userExists -> {
                                    if (!userExists) {
                                        Log.error("Invalid attempt forgot password");
                                        throw new BadRequestException();
                                    }
                                })
                                .onItem().invoke(() -> existSetPasswordLink(username)).onItem().invoke(existSetPasswordLink -> {
                                    if (!existSetPasswordLink) {
                                        Log.error("Invalid attempt forgot password");
                                        throw new BadRequestException();
                                    }
                                })
                                .chain(() -> Uni.createFrom().voidItem()).call(() -> {
                                    Log.info("Login attempt by " + ipAddress + " :" + device);
                                    return loginAttemptService.createLoginAttempt(ipAddress, device, "", "", username)
                                    .chain(() -> passwordService.createSetPasswordToken(username))
                                    .chain(token -> getSetPasswordEmailContent(token))
                                            .call(emailContent -> emailService.sendEmail(username, emailContent));
                                })
                                .onFailure().call(() -> {
                                    Log.error("Invalid attempt forgot password");
                                    return loginAttemptService.createLoginAttempt(ipAddress, device, "", "", username)
                                            .chain(() -> loginAttemptService.getLoginAttempts(ipAddress, device))
                                            .onItem().invoke(attempts -> {
                                                if (attempts > 10) {
                                                    blockedAccessService.blockIP(null);
                                                }
                                            });
                                }).onFailure().recoverWithUni(() -> Uni.createFrom().voidItem())
                )
                .onItemOrFailure().transformToUni((item, failure) -> Uni.createFrom().voidItem());
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {

    }

    @Override
    public void createAccount(String username, String password) {

    }

    @Override
    public void createAccount(String username) {

    }

    @Override
    public void deleteAccount(String username, String password) {

    }

    private Uni<String> getSetPasswordEmailContent(String token) {
        return Uni.createFrom().item("Email:" + token);
    }

    private boolean isUsernameAndPasswordValid(String username, String password) {
        return false;
    }

    private boolean isUsernameValid(String username) {
        return false;
    }

    private Uni<Boolean> existSetPasswordLink(String username) {
        return Uni.createFrom().item(true);
    }
}
