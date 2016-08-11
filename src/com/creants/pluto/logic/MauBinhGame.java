package com.creants.pluto.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.gamelib.GameAPI;
import com.avengers.netty.gamelib.key.NetworkConstant;
import com.avengers.netty.gamelib.om.RoomInfo;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.Player;
import com.creants.pluto.om.Result;
import com.creants.pluto.om.card.Card;
import com.creants.pluto.om.card.Cards;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.MauBinhConfig;
import com.creants.pluto.util.MessageFactory;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class MauBinhGame {
	public STATE gameState;
	private GameAPI gameApi;
	private static final MauBinhConfig gameConfig = MauBinhConfig.getInstance();
	private User winner = null;
	private MoneyManager moneyManager = null;
	private transient MauBinhCardSet cardSet;
	private Player[] players;
	private int limitTime = 60;
	// thời gian khi start game
	private long startTime;
	private IRoom room;
	private Map<String, User> disconnectedUsers;

	// game start sau bao 10s
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> countdownSchedule;
	private int startAfterSeconds = 10;
	private Integer countDownSeconds = 0;

	public MauBinhGame(IRoom room) {
		this.room = room;
		gameState = STATE.NOT_START;
		cardSet = new MauBinhCardSet();
		players = new Player[4];
		for (int i = 0; i < players.length; i++) {
			players[i] = new Player();
		}

		disconnectedUsers = new ConcurrentHashMap<String, User>();
	}

	public void setGameApi(GameAPI gameApi) {
		this.gameApi = gameApi;
	}

	public long getGuaranteeMoney(User user) {
		return moneyManager.getGuaranteeMoney(user);
	}

	protected User getWinner() {
		return winner;
	}

	public JsonObject getGameData() {
		if (isPlaying()) {
			JsonObject gameData = new JsonObject();
			// lấy thời gian đang đếm còn lại
			int remainTime = limitTime - (int) ((System.currentTimeMillis() - startTime) / 1000);
			gameData.addProperty("remain_time", remainTime);
			gameData.addProperty("game_state", gameState.getValue());
			return gameData;
		}

		return null;
	}

	public void disconnect(User user) {
		// cho phép reconnect
		disconnectedUsers.put(user.getUserName(), user);
		Player playerByUser = getPlayerByUser(user);
		processFinishCommand(user, processAutoArrangeCommand(playerByUser));
	}

	/**
	 * User reconnect
	 * 
	 * @param user
	 */
	public void onReturnGame(User user) {
		if (user == null) {
			return;
		}

		// lấy lại thông tin user cũ
		user = disconnectedUsers.get(user.getUserName());
		Player player = getPlayerByUser(user);
		if (player == null) {
			return;
		}

		disconnectedUsers.remove(user.getUserName());
		int restTime = 0;
		if (isPlaying()) {
			restTime = (int) ((limitTime - (System.currentTimeMillis() - startTime)) / 1000L);
		}

		if (!isPlaying() || player.getUser() == null) {
			gameApi.sendToUser(MessageFactory.makeInGameInforMessageForViewer(limitTime / 1000, restTime), user);
			return;
		}

		List<Card> cardList = player.isFinish() ? player.getCards().getArrangeCards() : player.getCards().getCards();
		gameApi.sendToUser(MessageFactory.makeInGameInforMessage(player.isFinish(), cardList, limitTime / 1000,
				restTime, (byte) player.getCards().getMauBinhType()), user);
	}

	private User getOwner() {
		return room.getOwner();
	}

	private boolean isTimeout() {
		return true;
	}

	public void update() {
		try {
			if (isPlaying() && isTimeout()) {
				boolean havePlayerAutoFinish = false;
				for (Player player : players) {
					if ((player.getUser() != null) && isPlaying() && !player.isFinish()) {
						player.setIsTimeOut(true);
						List<Card> listAutoArrangeCards = processAutoArrangeCommand(player);
						processFinishCommand(player.getUser(), listAutoArrangeCards);
						havePlayerAutoFinish = true;
					}
				}
				if (!havePlayerAutoFinish) {
					processFinish();
				}
			}
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] update fail!", e);
		}
	}

	public long getMoney() {
		// TODO optimize
		RoomInfo roomInfo = (RoomInfo) room.getProperty(NetworkConstant.ROOM_INFO);
		return roomInfo.getBetCoin();
	}

	private int playerSize() {
		return room.getPlayersList().size();
	}

	private void setCurrentMoveTime() {

	}

	public void startCountDown(int startAfterSeconds) {
		countDownSeconds = startAfterSeconds;

		if (countdownSchedule == null || countdownSchedule.isCancelled()) {
			debug("[DEBUG] game will be start after" + countDownSeconds + "seconds");
			countDown();
		}
	}

	private void debug(Object... msgs) {
		Tracer.debugMauBinh(MauBinhGame.class, msgs);
	}

	public int getStartAfterSeconds() {
		return startAfterSeconds;
	}

	public void startGame() {
		try {
			// TODO kiểm tra trong danh sách disconnect để đá user này ra
			if (playerSize() < 2) {
				debug("[DEBUG] Game is begin with a player");
				return;
			}

			moneyManager = new MoneyManager(getMoney());
			// kiểm tra có chủ phòng và chủ phòng còn tiền không để start game
			// không
			if (getOwner() != null && !moneyManager.enoughMoneyToStart(getOwner())) {
				gameApi.sendToUser(MessageFactory.makeErrorMessage("Khong du tien"), getOwner());
				for (int i = 0; i < playerSize(); i++) {
					User user = players[i].getUser();
					if (user != null && !user.equals(getOwner())) {
						playerReady(user, false);
					}
				}

				return;
			}

			debug(String.format("[DEBUG] Game is begin [roomId: %d, roomName: %s]", room.getId(), room.getName()));

			gameState = STATE.PLAYING;
			startTime = System.currentTimeMillis();

			// xóc bài
			cardSet.xaoBai();

			// chia bài
			deliveryCard();

			debug(String.format("[DEBUG] Delivery card finish [roomId: %d, roomName: %s]", room.getId(),
					room.getName()));

			setCurrentMoveTime();
			startCountDown(31);
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] startGame fail!", e);
		}
	}

	public void stopGame() {
		try {
			for (int i = 0; i < players.length; i++) {
				players[i].reset();
			}

			// gameApi.sendAllInRoom(MessageFactory.makeStopMessage());
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] stopGame fail!", e);
		} finally {
		}
	}

	private int getSeatNumber(User user) {
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player != null && player.getUserId() == user.getUserId())
				return i;
		}

		return -1;
	}

	/**
	 * Player sẵn sàn
	 * 
	 * @param user
	 * @param bln
	 */
	public void playerReady(User user, boolean isReady) {
		try {
			int nSeat = getSeatNumber(user);
			if (nSeat < 0 || nSeat >= 4) {
				return;
			}

			if (moneyManager == null || moneyManager.getGameMoney() != getMoney()) {
				moneyManager = new MoneyManager(getMoney());
			}

			if (moneyManager.enoughMoneyToStart(user)) {
				// super.playerReady(user, bln);
			} else {
				gameApi.sendToUser(MessageFactory.makeErrorMessage("Bạn không đủ tiền"), user);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			// Reporter.getErrorLogger()
			// .error("MauBinh playerReady error: r" + getRoom().getRoomId() +
			// "b" + getControllerId(), ex);
		}
	}

	private boolean isInTurn(User user) {
		return true;
	}

	private int countPlayerInTurn() {
		int count = 0;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				if (isInTurn(players[i].getUser()))
					count++;
			}
		}

		return count;
	}

	public void leave(User player) {
		try {
			if (!isPlaying() || getSeatNumber(player) < 0) {
				return;
			}

			// đá ra khỏi bàn chơi
			kickOut(player);

			if (playerSize() <= 0) {
				return;
			}

			long money = moneyManager.updateMoneyForLeave(this, player, countPlayerInTurn(), players);
			if (money != 0) {
				gameApi.updateUserMoney(player, money, 2, "Phạt rời game");
			}
			if (canBeFinish()) {
				for (int i = 0; i < players.length; i++) {
					if (players[i].getUser() != null) {
						players[i].setFinishFlag(true);
					}
				}

				if (GameChecker.isFinishAll(players)) {
					processFinish();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			// Reporter.getErrorLogger().error("MauBinh leave error: r" +
			// getRoom().getRoomId() + "b" + getControllerId(),e);
		}
	}

	private void kickOut(User user) {
		int seatNumber = getSeatNumber(user);
		players[seatNumber].setUser(null);
		players[seatNumber].reset();
	}

	public void setMoney(int value) {
		if (getOwner() == null) {
			return;
		}

		if (getOwner().getMoney() >= gameConfig.getStartMoneyRate() * value) {
			moneyManager = new MoneyManager(value);
		} else {
			gameApi.sendToUser(MessageFactory.makeErrorMessage("Khong du tien"), getOwner());
		}
	}

	protected void join(User user, String pwd) {
		System.out.println("[FATAL] join room");
		if (user != null
				&& (user.getMoney() >= gameConfig.getStartMoneyRate() * Math.max(getMinRoomMoney(), getMoney()))) {
			// super.join(user, pwd);
			if (moneyManager == null || moneyManager.getGameMoney() != getMoney()) {
				moneyManager = new MoneyManager(getMoney());
			}

			if (!moneyManager.enoughMoneyToStart(user)) {
				setMoneyToMin();
			}

			if (getOwner().equals(user)) {
				int limitTimeType;
				if (limitTime == gameConfig.getLimitTimeSlow()) {
					limitTimeType = 0;
				} else {

					if (limitTime == gameConfig.getLimitTimeFast()) {
						limitTimeType = 2;
					} else {
						limitTimeType = 1;
					}
				}

				gameApi.sendToUser(
						MessageFactory.makeSetLimitTimeMessage((byte) (limitTime / 1000), (byte) limitTimeType), user);
			}

			byte restTime = 0;
			if (isPlaying()) {
				restTime = (byte) ((limitTime - (System.currentTimeMillis() - startTime)) / 1000L);
			}

			gameApi.sendToUser(MessageFactory.makeTableInfoMessage((byte) (limitTime / 1000), restTime), user);
		} else {
			gameApi.sendToUser(MessageFactory.makeErrorMessage("NotEnoughMoneyToSet"), user);
		}
	}

	/**
	 * Kiểm tra game có thể hoàn thành không. Tất cả player đã finish chưa.
	 * 
	 * @return
	 */
	private boolean canBeFinish() {
		if (players == null || players.length == 0) {
			return false;
		}

		int playingNo = 0;
		int finishNo = 0;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				if (players[i].isFinish()) {
					finishNo++;
				}
				playingNo++;
			}
		}

		return playingNo == 1 || finishNo == playingNo;
	}

	private User getUser(int index) {
		// TODO optimize
		return room.getListUser().get(index);
	}

	private IRoom getRoom() {
		return room;
	}

	/**
	 * Chia bài
	 */
	private void deliveryCard() {
		for (int i = 0; i < players.length; i++) {
			players[i].reset();
		}

		for (int i = 0; i < cardSet.length(); i++) {
			players[(i % 4)].getCards().receivedCard(cardSet.dealCard());
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < playerSize(); i++) {
			User receiver = getUser(i);
			players[i].setUser(receiver);
			if (receiver != null) {
				if (getMoney() > 0) {
				}

				sb.append(receiver.getUserName() + ";");
				Message message = MessageFactory.makeStartMessage(getRoom().getId(), limitTime / 1000,
						players[i].getCards().getCards(), (byte) players[i].getCards().getMauBinhType());
				gameApi.sendToUser(message, receiver);
			}
		}

		Tracer.debugMauBinh(MauBinhGame.class, "Deliver card to: " + sb.toString());
		// trường hợp đang chia bài mà người chơi bấm tự động bin hết
		if (GameChecker.isFinishAll(players)) {
			processFinish();
		}
	}

	private boolean isPlaying() {
		return gameState == STATE.PLAYING;
	}

	/**
	 * User gửi lệnh bin xong
	 * 
	 * @param user
	 * @param listCards
	 *            danh sách card user gửi lên
	 */
	public void processFinishCommand(User user, List<Card> listCards) {
		Player player = getPlayerByUser(user);
		if (player == null) {
			gameState = STATE.NOT_START;
			return;
		}

		try {
			Cards cards = player.getCards();
			cards.clearArrangement();

			for (int i = 0; i < 3; i++) {
				cards.receivedCardTo1stSet(listCards.get(i));
			}

			int beginset2 = 3;
			for (int i = beginset2; i < 5 + beginset2; i++) {
				cards.receivedCardTo2ndSet(listCards.get(i));
			}

			int beginset3 = 8;
			for (int i = beginset3; i < 5 + beginset3; i++) {
				cards.receivedCardTo3rdSet(listCards.get(i));
			}

			if (!cards.isFinishArrangement()) {
				String errorMessage = "Binh Thủng";
				Message m = MessageFactory.makeInterfaceErrorMessage(GameCommand.ACTION_FINISH,
						GameCommand.ERROR_IN_GAME_BIN_THUNG, errorMessage);
				gameApi.sendToUser(m, user);

				int remainTime = limitTime - (int) ((System.currentTimeMillis() - startTime) / 1000);
				debug("[DEBUG] Bin lủng - user:" + user.getUserName() + ", remaintime: " + remainTime);

				if (remainTime <= 0) {
					player.setFinishFlag(true);
					if (GameChecker.isFinishAll(players)) {
						processFinish();
					}
				}
			} else {
				cards.setMauBinhTypeAfterArrangement();
				if (!player.isTimeOut()) {
					// Báo cho các player khác người chơi này đã bin xong
					gameApi.sendAllInRoom(MessageFactory.makeFinishMessage(user.getUserId()));
				}
				player.setFinishFlag(true);
				if (GameChecker.isFinishAll(players)) {
					processFinish();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Tracer.error(MauBinhGame.class, "[ERROR] processFinishCommand fail!", e);
			// String errorMessage = getMessage(MauBinhGame.class.getName(), new
			// Locale(user.getLocale()), "MissCard");
			String errorMessage = "Lỗi kết thúc game";
			Message mess = MessageFactory.makeInterfaceErrorMessage(GameCommand.ACTION_FINISH,
					GameCommand.ERROR_IN_GAME_MISS_CARD, errorMessage);
			gameApi.sendToUser(mess, user);
		}

	}

	/**
	 * Tự động xếp bài
	 * 
	 * @param player
	 * @return
	 */
	public List<Card> processAutoArrangeCommand(Player player) {
		List<Card> result = new ArrayList<Card>();
		try {
			result = AutoArrangement.getSolution(player.getCards().getCards());
			debug(String.format("[DEBUG] Auto Arrange [username:%s] cards:\n %s", player.getUser().getUserName(),
					logCard(result)));

			if (!player.isTimeOut()) {
				Message m = MessageFactory.makeAutoArrangeResultMessage(result);
				gameApi.sendToUser(m, player.getUser());
			}

			player.setAutoArrangementFlag(true);
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] processAutoArrangeCommand fail!", e);
		}

		return result;
	}

	private void processFinish() {
		if (players == null) {
			return;
		}

		Result[][] result = GameChecker.comparePlayers(players);
		int[] winChi = GameChecker.getWinChi(players, result);
		long[] winMoney = moneyManager.calculateMoney(winChi);
		winMoney = moneyManager.addBonusChi(players, winMoney, winChi);
		// winMoney = moneyManager.updateMoney(this, players, winMoney, winChi,
		// result);
		// winMoney = moneyManager.updateMoneyForAutoArrangementUsing(this,
		// players, winMoney);

		for (int i = 0; i < players.length; i++) {
			User user = players[i].getUser();
			if (user != null) {
				if (winMoney[i] != 0) {
					gameApi.updateUserMoney(user, winMoney[i], 1, "Cập nhật tiền kết thúc game");
				}

				Message message = MessageFactory.makeResultMessage(i, players, winMoney, winChi, result);
				if (message != null) {
					gameApi.sendToUser(message, user);
				}
			}
		}

		winner = getWinner(winChi);
		if (!moneyManager.enoughMoneyToStart(getOwner())) {
			setMoneyToMin();
		}

		setCurrentMoveTime();
		stopGame();

		gameState = STATE.FINISH;
		startCountDown(15);
	}

	private User getWinner(int[] winChi) {
		if (winChi == null || winChi.length == 0 || players == null || winChi.length != players.length) {
			return null;
		}

		int seatWinner = -1;
		User ret = null;
		int maxWinChi = Integer.MIN_VALUE;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {

				if (winChi[i] == maxWinChi) {
					seatWinner = -1;

				} else if (maxWinChi < winChi[i]) {
					maxWinChi = winChi[i];
					seatWinner = i;
				}
			}
		}

		if (seatWinner != -1) {
			ret = players[seatWinner].getUser();
		}

		return ret;
	}

	private void setMoneyToMin() {
		setMoney(getMinRoomMoney());
	}

	private int getMinRoomMoney() {
		return 0;
	}

	public Player getPlayerByUser(User user) {
		int sNum = getSeatNumber(user);
		if (sNum == -1 || sNum >= players.length) {
			return null;
		}

		return players[sNum];
	}

	public static String getMessage(String className, Locale locale, String property) {
		try {
			return ResourceBundle.getBundle(className, locale).getString(property);
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] getMessage fail!",
					"className:" + className + ", property:" + property + ", locale:" + locale + "}", e);
		}
		return null;
	}

	public void countDown() {
		debug("[DEBUG] Start countdown in " + countDownSeconds + " seconds");
		countdownSchedule = scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				synchronized (countDownSeconds) {
					debug("[DEBUG] tick:" + countDownSeconds);
					if (countDownSeconds > 0) {
						countDownSeconds--;
					} else {
						if (gameState == STATE.NOT_START) {
							stopCountDown();
							processUserDisconnect();
							startGame();
						} else if (gameState == STATE.PLAYING) {
							for (int i = 0; i < players.length; i++) {
								if (players[i].getUser() != null) {
									players[i].setFinishFlag(true);
								}
							}

							if (GameChecker.isFinishAll(players)) {
								processFinish();
							}

						} else if (gameState == STATE.FINISH) {
							gameState = STATE.NOT_START;
							processUserDisconnect();
							startGame();
						}
						// stopCountDownStartGame();
						// TODO startGame
					}
				}
			}
		}, 0, 1, TimeUnit.SECONDS);

	}

	private void processUserDisconnect() {
		// có thằng nào disconnect trước đó ko, có thì đá ra
		if (disconnectedUsers.size() > 0) {
			Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_QUIT_GAME);
			User user = null;
			for (String username : disconnectedUsers.keySet()) {
				user = disconnectedUsers.get(username);
				message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getUserId());
				gameApi.sendAllInRoomExceptUser(message, user);
				disconnectedUsers.remove(username);

				kickOut(user);
			}
		}
	}

	public void stopCountDown() {
		countDownSeconds = startAfterSeconds;

		if (countdownSchedule != null) {
			countdownSchedule.cancel(true);
		}
	}

	private String logCard(List<Card> cards) {
		int count = 0;
		String chi1 = "";
		String chi2 = "";
		String chi3 = "";
		for (Card card : cards) {
			if (count < 3) {
				chi1 += card.getName() + "   ";
			} else if (count < 8) {
				chi2 += card.getName() + "   ";
			} else {
				chi3 += card.getName() + "   ";
			}
			count++;
		}

		return chi1 + "\n" + chi2 + "\n" + chi3;
	}

	public static enum STATE {
		NOT_START(0), PLAYING(1), FINISH(2), CALCULATE(3);
		private int value;

		STATE(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
