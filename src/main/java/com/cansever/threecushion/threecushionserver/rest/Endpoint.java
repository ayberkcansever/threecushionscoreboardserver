package com.cansever.threecushion.threecushionserver.rest;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import com.cansever.threecushion.threecushionserver.db.DBHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by AyberkC on 07.07.2017.
 */
@Component
@Path("/rest")
public class Endpoint {

    @Autowired
    private DBHelper dbHelper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/loadgame/{player_id}")
    public GameBean load(@PathParam("player_id") String playerId) {
        return dbHelper.loadPlayersGame(playerId);
    }

    @POST
    @Path("/start")
    public String startGame(@RequestBody GameBean gameBean) {
        return dbHelper.insertNewGame(gameBean);
    }

}