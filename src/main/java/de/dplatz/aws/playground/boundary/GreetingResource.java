/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dplatz.aws.playground.boundary;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 *
 * @author daniel.platz
 */
@Path("greetings")
public class GreetingResource {
    
    @GET
    @Path("{name}")
    public String greet(@PathParam("name") String name) {
        return "Hello " + name + ". Have a great day!";
    }
}
