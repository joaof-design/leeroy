package org.leeroy.authenticator.resource;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.leeroy.authenticator.exception.InvalidLoginAttemptException;
import org.leeroy.authenticator.exception.WaitBeforeTryingLoginAgainException;
import org.leeroy.authenticator.resource.request.AuthenticateRequest;
import org.leeroy.authenticator.service.AccountServiceBase;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/v1")
public class AccountResource {

    @Inject
    AccountServiceBase accountService;

    /**
     * @param authenticateRequest
     * @return
     * @throws InvalidLoginAttemptException
     * @throws WaitBeforeTryingLoginAgainException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/authenticate")
    public Uni<Object> authenticate(@Context HttpServerRequest request,
                                    AuthenticateRequest authenticateRequest) throws InvalidLoginAttemptException,
            WaitBeforeTryingLoginAgainException {
        String ipAddress = request.remoteAddress().hostAddress();
        return accountService.authenticate(authenticateRequest);
    }


    @PUT
    @Path("/forgot-password")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> forgotPassword(@Context HttpServerRequest request, JsonObject body) {
        String ipAddress = request.remoteAddress().hostAddress();
        String device = "";
        String username = body.getString("username");
        return accountService.forgotPassword(ipAddress, device, username).onItem().transform(item -> "We sent you a email which you can use to set your password");
    }

    @POST
    @Path("/create-account")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> createAccount(@Context HttpServerRequest request, JsonObject body) {
        String ipAddress = request.remoteAddress().hostAddress();
        String device = "";
        String username = body.getString("username");
        String password = body.getString("password");
        if (password != null) {
            return accountService.createAccount(ipAddress, device, username, password);
        } else {
            return accountService.createAccount(ipAddress, device, username);
        }
    }

    @PUT
    @Path("/set-password")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> setPassword(@Context HttpServerRequest request, JsonObject body) {
        String token = body.getString("token");
        String password = body.getString("password");
        return accountService.setPassword(token, password).onItem().transform(item -> "Password changed");
    }

    @PUT
    @Path("/change-password")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> changePassword(@Context HttpServerRequest request, JsonObject body) {
        String username = body.getString("username");
        String oldPassword = body.getString("oldPassword");
        String newPassword = body.getString("newPassword");
        return accountService.changePassword(username, oldPassword, newPassword).onItem().transform(item -> "Password changed");
    }

    @POST
    @Path("/delete-account")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> deleteAccount(@Context HttpServerRequest request, JsonObject body) {
        String username = body.getString("username");
        String password = body.getString("password");
        return accountService.deleteAccount(username, password).onItem().transform(item -> "Account deleted");
    }
}
