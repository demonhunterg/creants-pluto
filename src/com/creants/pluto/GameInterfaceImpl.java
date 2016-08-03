package com.creants.pluto;

import java.util.List;

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.gamelib.GameAPI;
import com.avengers.netty.gamelib.GameInterface;
import com.avengers.netty.gamelib.result.IPlayMoveResult;
import com.avengers.netty.gamelib.result.StartGameResult;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.handler.AutoArrangeRequestHandler;
import com.creants.pluto.handler.FinishRequestHandler;
import com.creants.pluto.handler.JoinRoomRequestHandler;
import com.creants.pluto.handler.UserDisconnectHandler;
import com.creants.pluto.logic.MauBinhGame;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.MessageFactory;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class GameInterfaceImpl extends AbstractGameLogic implements GameInterface {
	private MauBinhGame gameLogic;
	private GameAPI gameAPI;

	public GameInterfaceImpl(IRoom room) {
		super(room);
		Tracer.info(GameInterfaceImpl.class, "- Innit PLUTO");
	}

	@Override
	public MauBinhGame createGameLogic(IRoom room) {
		gameLogic = new MauBinhGame(room);
		return gameLogic;
	}

	@Override
	public MauBinhGame getGameLogic() {
		return gameLogic;
	}

	@Override
	public void initRequestHandler() {
		addRequestHandler(SystemNetworkConstant.COMMAND_USER_JOIN_ROOM, new JoinRoomRequestHandler());
		addRequestHandler(GameCommand.ACTION_AUTO_ARRANGE, new AutoArrangeRequestHandler());
		addRequestHandler(GameCommand.ACTION_FINISH, new FinishRequestHandler());
		addRequestHandler(SystemNetworkConstant.COMMAND_USER_DISCONNECT, new UserDisconnectHandler());
	}

	@Override
	public void dispatchEvent(short commandId, User user, Message message) {
		processRequest(commandId, user, message);
	}

	@Override
	public JsonObject getGameData() {
		return gameLogic.getGameData();
	}

	@Override
	public Object getGameDataForViewer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void leaveRoom(User user, IRoom room) {
		int totalUsers = room.countPlayer();
		Tracer.debugMauBinh(GameInterfaceImpl.class, "[DEBUG] user leave room.", "username:" + user.getUserName(),
				"room:" + room.getName(), "count player: " + totalUsers);
		// trường hợp thoát ra còn một người chơi duy nhất thì không đếm
		if (totalUsers < 2) {
			gameLogic.stopCountDown();
		}

		gameLogic.leave(user);
		Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_QUIT_GAME);
		message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getUserId());
		gameAPI.sendAllInRoomExceptUser(message, user);

		if (room.isOwner(user)) {

		}
	}

	@Override
	public IPlayMoveResult onPlayMoveHandle(User sender, Message message) {
		IPlayMoveResult playMoveResult = processRequest(message.getByte(SystemNetworkConstant.KEYR_ACTION_IN_GAME),
				sender, message);
		// TODO kiểm tra trạng thái của game
		return playMoveResult;
	}

	@Override
	public StartGameResult onStartGame(User owner, List<User> listPlayer, Message extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setApi(GameAPI gameApi) {
		this.gameAPI = gameApi;
		// TODO refactor
		initGameApi(gameApi);
		gameLogic.setGameApi(gameApi);

	}

	public GameAPI getGameAPI() {
		return gameAPI;
	}

	@Override
	public void setDealer(long userId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startCountdownStartGame(long timeout) {
		// TODO Auto-generated method stub

	}

	@Override
	public IPlayMoveResult timeout() {
		// TODO Auto-generated method stub
		return null;
	}

}