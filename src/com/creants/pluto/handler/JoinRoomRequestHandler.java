package com.creants.pluto.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.om.RoomSize;
import com.avengers.netty.gamelib.key.NetworkConstant;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.logic.MauBinhGame.STATE;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.MessageFactory;

/**
 * @author LamHa
 *
 */
public class JoinRoomRequestHandler extends AbstractRequestHandler {
	private static final Logger LOG = LoggerFactory.getLogger(JoinRoomRequestHandler.class);

	@Override
	public void handleRequest(User user, Message message) {
		IRoom lastJoinedRoom = user.getLastJoinedRoom();
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("[DEBUG] Join Room [username: %s, roomId:%d, roomName:%s]", user.getUserName(),
					lastJoinedRoom.getId(), lastJoinedRoom.getName()));
		}
		

		RoomSize roomSize = lastJoinedRoom.getSize();
		int totalUsers = roomSize.getTotalUsers();
		if (totalUsers >= 2 && gameLogic.gameState == STATE.NOT_START) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("[DEBUG] Starting game [username: %s, roomId:%d, roomName:%s]",
						user.getUserName(), lastJoinedRoom.getId(), lastJoinedRoom.getName()));
			}
			gameLogic.startCountDown(10);
			message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_START_AFTER_COUNTDOWN);
			message.putInt(NetworkConstant.KEYI_TIMEOUT, gameLogic.getStartAfterSeconds());
			gameApi.sendAllInRoom(message);
		}

	}

}
