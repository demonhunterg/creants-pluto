package com.creants.pluto.handler;

import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.Player;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.MessageFactory;

/**
 * @author LamHa
 *
 */
public class ReadyRequestHandler extends AbstractRequestHandler {

	@Override
	public void handleRequest(User user, Message message) {
		Player player = gameLogic.getPlayerByUser(user);
		if (player != null && player.getUser() != null) {
			Tracer.debug(ReadyRequestHandler.class, "[FATAL] user ready: " + user.getUserName());
			player.setReady(true);
			gameApi.sendToUser(MessageFactory.createMauBinhMessage(GameCommand.ACTION_READY), user);
		} else {
			// TODO log
		}
	}

}
