package org.leeroy.authenticator.resource;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.leeroy.authenticator.exception.InvalidLoginAttemptException;
import org.leeroy.authenticator.exception.WaitBeforeTryingLoginAgainException;
import org.leeroy.authenticator.resource.request.AuthenticateRequest;
import org.leeroy.authenticator.service.AccountService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/v1")
public class AccountResource {

    @Inject
    AccountService accountService;

    /**
     * @param authenticateRequest
     * @return
     * @throws InvalidLoginAttemptException
     * @throws WaitBeforeTryingLoginAgainException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/authenticate")
    public Response authenticate(AuthenticateRequest authenticateRequest) throws InvalidLoginAttemptException,
            WaitBeforeTryingLoginAgainException {
        accountService.authenticate(authenticateRequest);
        return Response.ok().build();
    }


    @PUT
    @Path("/forgot-password/{username}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> forgotPassword(@Context HttpServerRequest request, @PathParam("username") String username){
        String ipAddress = request.remoteAddress().hostAddress();
        String device = "";
        return accountService.forgotPassword(ipAddress, device, username).onItem().transform(item -> "We sent you a email which you can use to set your password");
    }

    @PUT
    @Path("/create-account")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> createAccount(@Context HttpServerRequest request, JsonObject body){
        String ipAddress = request.remoteAddress().hostAddress();
        String device = "";
        String username = body.getString("username");
        String password = body.getString("password");
        if (password != null){
            return accountService.createAccount(ipAddress, device, username, password);
        } else {
            return accountService.createAccount(ipAddress, device, username);
        }
    }
}
