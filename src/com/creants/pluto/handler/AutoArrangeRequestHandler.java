package com.creants.pluto.handler;

import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.Player;

/**
 * @author LamHa
 *
 */
public class AutoArrangeRequestHandler extends AbstractRequestHandler {

	@Override
	public void handleRequest(User user, Message message) {
		Player player = gameLogic.getPlayerByUser(user);
		gameLogic.processAutoArrangeCommand(player);
	}

}
