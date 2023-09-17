package de.dplatz;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.stream.Collectors;

@Path("/hello")
public class TestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@QueryParam("var") String var) {
        System.out.print(".");
        if (var == null) return System.getenv().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n"));

        return System.getenv(var);
    }

    @GET
    @Path("session")
    @Produces(MediaType.TEXT_PLAIN)
    public Response session(@CookieParam("mycookie") Cookie cookie) {
        String podName = System.getenv("HOSTNAME");
        if (cookie == null) {
            NewCookie newCookie = new NewCookie("mycookie", podName);
            return Response.ok("Created cookie for " + podName).cookie(newCookie).build();
        }
        else {
            if (cookie.getValue().equals(podName)) {
                return Response.ok("Received cookie for myself " + podName).build();
            }
            else {
                return Response.ok("!!!! RECEIVED WRONG COOKIE !!!! I am " + podName + " but cookie for " + cookie.getValue()).build();
            }
        }

    }
}
