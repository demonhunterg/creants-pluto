package com.creants.pluto.handler;

import com.avengers.netty.core.util.CoreTracer;
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
		if (user.getMoney() <= 1000) {
			gameApi.leaveRoom(user.getUserId());
			return;
		}

		Player player = gameLogic.getPlayerByUser(user);
		if (player != null && player.getUser() != null) {
			CoreTracer.debug(ReadyRequestHandler.class, "[DEBUG] [" + user.getUserName() + "] is ready.");
			player.setReady(true);
			gameApi.sendToUser(MessageFactory.createMauBinhMessage(GameCommand.ACTION_READY), user);
		} else {
			CoreTracer.error(ReadyRequestHandler.class, "[ERROR] [" + user.getUserName() + "] is not player. ");
		}
	}

}
