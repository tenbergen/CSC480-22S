package edu.oswego.cs.rest.controllers;

import javax.annotation.security.RolesAllowed;
import javax.json.JsonException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.oswego.cs.rest.services.AuthServices;


@Path("/auth")
public class Controller {
    @POST
    @Path("token/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateToken(@Context HttpHeaders request) throws JsonException{
        if (request.getRequestHeader(HttpHeaders.AUTHORIZATION) == null) 
            return Response.status(Response.Status.FORBIDDEN).entity("No token found.").build();
        
        String authToken = request.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0).split(" ")[1];
        return Response.status(Response.Status.OK).entity(new AuthServices().generateNewToken(authToken)).build();
    }
    
    @POST
    @RolesAllowed("lakers")
    @Path("token/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshToken(@Context SecurityContext securityContext) {
        return Response.status(Response.Status.OK).entity(new AuthServices().refreshToken(securityContext)).build();
    }
    
}
