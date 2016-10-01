package com.creants.pluto.util;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.gamelib.key.NetworkConstant;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.MauBinhType;
import com.creants.pluto.om.Player;
import com.creants.pluto.om.Result;
import com.creants.pluto.om.card.Card;
import com.creants.pluto.om.card.Cards;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class MessageFactory {
	private static final byte PROTOCOL_VERSION = 1;

	public static Message createMessage(short commandId) {
		Message message = new Message();
		message.setCommandId(commandId);
		message.setProtocolVersion(PROTOCOL_VERSION);
		return message;
	}

	public static Message makeStartMessage(int roomId, int limitTime, List<Card> cards, byte maubinhType) {
		Message message = createMauBinhMessage(GameCommand.ACTION_START_GAME);
		try {
			message.putInt(SystemNetworkConstant.KEYI_ROOM_ID, roomId);
			message.putInt(NetworkConstant.KEYI_TIMEOUT, limitTime);
			message.putLong(GameCommand.KEYL_UTC_TIME, DateTime.now().toDateTime(DateTimeZone.UTC).getMillis());

			byte[] cardIds = new byte[cards.size()];
			for (int i = 0; i < cards.size(); i++) {
				cardIds[i] = cards.get(i).getId();
			}

			message.putBytes(NetworkConstant.KEYBLOB_CARD_LIST, cardIds);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeStartMessage fail!", e);
		}

		return message;
	}

	public static Message makeStopMessage() {
		return createMauBinhMessage(GameCommand.STOP);
	}

	public static Message makeSetLimitTimeMessage(int limitTime, byte type) {
		Message m = createMauBinhMessage(GameCommand.SET_LIMIT_TIME);
		try {
			// m.dos.writeByte(limitTime);
			// m.dos.writeByte(type);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeSetLimitTimeMessage fail!", e);
		}

		return m;
	}

	public static Message makeTableInfoMessage(byte limitTime, byte restTime) {
		Message m = createMauBinhMessage(GameCommand.TABLE_INFO);
		try {
			// m.dos.writeByte(limitTime);
			// m.dos.writeByte(restTime);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeTableInfoMessage fail!", e);
		}

		return m;
	}

	public static Message makeFinishMessage(int userID) {
		Message message = createMauBinhMessage(GameCommand.ACTION_FINISH);
		try {
			message.putInt(SystemNetworkConstant.KEYI_USER_ID, userID);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeFinishMessage fail!", e);
		}

		return message;
	}

	public static Message makeInterfaceErrorMessage(byte command, short errorCode, String errorMessage) {
		Message message = createMauBinhMessage(GameCommand.INTERFACE_ERROR);
		message.putShort(SystemNetworkConstant.KEYR_ACTION_IN_GAME, command);
		message.putShort(SystemNetworkConstant.KEYR_ERROR, errorCode);
		message.putString(SystemNetworkConstant.KEYS_MESSAGE, errorMessage);
		return message;
	}

	public static Message makeResultMessage(int playerIndex, Player[] players, long[] winMoney, int[] winChi,
			Result[][] result) {
		Player player = players[playerIndex];
		if (players == null || winChi == null || result == null || playerIndex < 0 || playerIndex >= players.length
				|| player.getUser() == null) {
			return null;
		}

		Message message = createMauBinhMessage(GameCommand.ACTION_END_GAME);
		JsonObject gameResult = new JsonObject();
		gameResult.addProperty("player_id", player.getUser().getPlayerId());
		gameResult.addProperty("username", player.getUser().getUserName());
		gameResult.addProperty("money", winMoney[playerIndex]);
		gameResult.addProperty("winchi_no", winChi[playerIndex]);
		Cards cards = players[playerIndex].getCards();
		byte type = cards.isFailedArrangement() ? MauBinhType.BINH_LUNG : cards.getMauBinhType();
		gameResult.addProperty("mau_binh_type", type);
		int winChiMauBinh = 0, winChi1 = 0, winChi2 = 0, winChi3 = 0, winchiAce = 0;

		JsonArray ja = new JsonArray();
		try {
			User user = null;
			JsonObject jo = null;
			for (int i = 0; i < players.length; i++) {
				user = players[i].getUser();
				if (i != playerIndex && user != null) {
					jo = new JsonObject();
					jo.addProperty("player_id", user.getPlayerId());
					jo.addProperty("username", user.getUserName());
					jo.addProperty("money", winMoney[i]);
					jo.addProperty("winchi_no", winChi[i]);
					cards = players[i].getCards();
					type = cards.isFailedArrangement() ? MauBinhType.BINH_LUNG : cards.getMauBinhType();
					jo.addProperty("mau_binh_type", type);

					List<Card> cardList = cards.getArrangeCards();
					if (cardList == null) {
						cardList = cards.list();
					}

					byte[] cardIds = new byte[cardList.size()];
					for (int j = 0; j < cardList.size(); j++) {
						cardIds[j] = cardList.get(j).getId();
					}

					int winChiMauBinh2 = result[playerIndex][i].getWinChiMauBinh();
					winChiMauBinh += winChiMauBinh2;
					int winChi01 = result[playerIndex][i].getWinChi01();
					winChi1 += winChi01;
					int winChi02 = result[playerIndex][i].getWinChi02();
					winChi2 += winChi02;
					int winChi03 = result[playerIndex][i].getWinChi03();
					winChi3 += winChi03;
					// int winChiAce2 = result[playerIndex][i].getWinChiAce();
					// winchiAce += winChiAce2;

					jo.addProperty("card_list", GsonUtils.toGsonString(cardIds));
					jo.addProperty("winchi_maubinh", -winChiMauBinh2);
					jo.addProperty("winchi_1", -winChi01);
					jo.addProperty("winchi_2", -winChi02);
					jo.addProperty("winchi_3", -winChi03);
					// jo.addProperty("winchi_ace", -winChiAce2);
					ja.add(jo);
				}
			}
		} catch (Exception e) {
			Tracer.errorRoom(MessageFactory.class, "[ERROR] makeResultMessage fail!", e);
		}

		gameResult.addProperty("winchi_maubinh", winChiMauBinh);
		gameResult.addProperty("winchi_1", winChi1);
		gameResult.addProperty("winchi_2", winChi2);
		gameResult.addProperty("winchi_3", winChi3);
		gameResult.addProperty("winchi_ace", winchiAce);
		gameResult.addProperty("start_after", 15);

		gameResult.add("player_list", ja);
		message.putString(SystemNetworkConstant.KEYS_JSON_DATA, gameResult.toString());

		return message;
	}
	// public static Message makeResultMessage(int playerIndex, Player[]
	// players, long[] winMoney, int[] winChi,
	// Result[][] result) {
	// Player player = players[playerIndex];
	// if (players == null || winChi == null || result == null || playerIndex <
	// 0 || playerIndex >= players.length
	// || player.getUser() == null) {
	// return null;
	// }
	//
	// Message message = createMauBinhMessage(GameCommand.ACTION_END_GAME);
	// try {
	// message.putInt(SystemNetworkConstant.KEYI_USER_ID,
	// player.getUser().getUserId());
	// message.putLong(SystemNetworkConstant.KEYL_MONEY, winMoney[playerIndex]);
	// byte type = (byte) ((player.getCards().IsFailedArrangement() ||
	// player.isTimeOut()) ? -2
	// : player.getCards().getMauBinhType());
	//
	// message.putByte(GameCommand.KEYB_MAUBINH_TYPE, type);
	// // tổng thắng thua bao nhiêu chi
	// message.putInt(GameCommand.KEYI_WINCHI, winChi[playerIndex]);
	//
	// for (int i = 0; i < players.length; i++)
	// if (i != playerIndex && players[i].getUser() != null) {
	// message.putInt(SystemNetworkConstant.KEYI_USER_ID,
	// players[i].getUser().getUserId());
	// message.putLong(SystemNetworkConstant.KEYL_MONEY, winMoney[i]);
	// message.putInt(GameCommand.KEYI_WINCHI, winChi[i]);
	// type = (byte) (players[i].getCards().IsFailedArrangement() ? -2
	// : players[i].getCards().getMauBinhType());
	// // -2 là thủng hoặc hết giờ
	// message.putByte(GameCommand.KEYI_WINCHI, type);
	//
	// List<Card> cardList = players[i].getCards().getArrangeCards();
	// if (cardList == null) {
	// cardList = players[i].getCards().getCards();
	// }
	//
	// byte[] cardIds = new byte[cardList.size()];
	// for (int j = 0; j < cardList.size(); j++) {
	// cardIds[j] = cardList.get(j).getId();
	// }
	//
	// message.putBytes(NetworkConstant.KEYBLOB_CARD_LIST, cardIds);
	//
	// // TODO dinh nghia key winchi theo từng loại, ko dùng key
	// // giống nhau
	// message.putInt((byte) 0x06, result[playerIndex][i].getWinChiMauBinh());
	// message.putInt((byte) 0x06, result[playerIndex][i].getWinChi01());
	// message.putInt((byte) 0x06, result[playerIndex][i].getWinChi02());
	// message.putInt((byte) 0x06, result[playerIndex][i].getWinChi03());
	// message.putInt((byte) 0x06, result[playerIndex][i].getWinChiAce());
	// }
	// } catch (Exception ex) {
	// Reporter.getErrorLogger().error("Mau binh
	// MessageFactory.makeResultMessage error: ", ex);
	// }
	//
	// return message;
	// }

	public static Message makeAutoArrangeResultMessage(List<Card> cards) {
		Message message = createMauBinhMessage(GameCommand.ACTION_AUTO_ARRANGE);
		try {
			byte[] cardIds = new byte[cards.size()];
			for (int i = 0; i < cards.size(); i++) {
				cardIds[i] = cards.get(i).getId();
			}

			message.putBytes(NetworkConstant.KEYBLOB_CARD_LIST, cardIds);
			message.putLong(GameCommand.KEYL_UTC_TIME, DateTime.now().toDateTime(DateTimeZone.UTC).getMillis());
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeAutoArrangeResultMessage fail!", e);
		}

		return message;
	}

	public static Message makeSortByOrderMessage(List<Card> cards) {
		Message message = createMauBinhMessage(GameCommand.SORT_BY_ORDER);
		try {
			byte[] cardIds = new byte[cards.size()];
			for (int i = 0; i < cards.size(); i++) {
				cardIds[i] = cards.get(i).getId();
			}

			message.putBytes(NetworkConstant.KEYBLOB_CARD_LIST, cardIds);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeSortByOrderMessage fail!", e);
		}

		return message;
	}

	public static Message makeSortByTypeMessage(List<Card> cards) {
		Message message = createMauBinhMessage(GameCommand.SORT_BY_TYPE);
		try {
			byte[] cardIds = new byte[cards.size()];
			for (int i = 0; i < cards.size(); i++) {
				cardIds[i] = cards.get(i).getId();
			}
			message.putBytes(NetworkConstant.KEYBLOB_CARD_LIST, cardIds);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeSortByTypeMessage fail!", e);
		}

		return message;
	}

	public static Message makeErrorMessage(String errorMessage) {
		Message message = new Message();
		return message;
	}

	public static Message makeInGameInforMessage(boolean isFinish, List<Card> cards, int limitTime, int restTime,
			byte maubinhType) {
		Message m = new Message();
		try {
			if ((cards == null) || (cards.isEmpty())) {
				return null;
			}

			// m.dos.writeByte(isFinish ? 1 : 0);
			// for (int i = 0; i < cards.size(); i++) {
			// m.dos.writeByte(cards.get(i).getId());
			// }
			//
			// m.dos.writeByte(limitTime);
			// m.dos.writeByte(restTime);
			// m.dos.writeByte(maubinhType);
		} catch (Exception e) {
			Tracer.error(MessageFactory.class, "[ERROR] makeInGameInforMessage fail!", e);
		}

		return m;
	}

	public static Message makeInGameInforMessageForViewer(int limitTime, int restTime) {
		Message message = new Message();
		return message;
	}

	public static Message createMauBinhMessage(short inGamecommandId) {
		Message message = new Message();
		message.setCommandId(NetworkConstant.COMMAND_REQUEST_IN_GAME);
		message.setProtocolVersion(PROTOCOL_VERSION);
		message.putShort(SystemNetworkConstant.KEYR_ACTION_IN_GAME, inGamecommandId);
		return message;
	}
}
