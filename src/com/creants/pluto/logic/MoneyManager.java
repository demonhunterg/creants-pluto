package com.creants.pluto.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.Player;
import com.creants.pluto.om.Result;
import com.creants.pluto.util.MauBinhConfig;

/**
 * @author LamHa
 *
 */
public class MoneyManager {
	private static final int MAX_MONEY = 2000000000;
	private long gameMoney;

	public MoneyManager(long gameMoney) {
		this.gameMoney = gameMoney;
	}

	public long getGameMoney() {
		return gameMoney;
	}

	/**
	 * Kiểm tra user có đủ tiền không
	 * 
	 * @param user
	 * @return
	 */
	public boolean enoughMoneyToStart(User user) {
		if (user == null) {
			return false;
		}

		return user.getMoney() >= gameMoney * MauBinhConfig.getInstance().getStartMoneyRate();
	}

	public long getGuaranteeMoney(User user) {
		return Math.max(0L, user.getMoney() - MauBinhConfig.getInstance().getMaxLoseChi() * gameMoney);
	}

	public double[][] getWinChi(Result[][] result) {
		if (result == null || result[0] == null) {
			return null;
		}

		double[][] ret = new double[result.length][result[0].length];
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result[0].length; j++) {
				ret[i][j] = (result[i][j] == null ? 0.0D : result[i][j].getWinChi());
			}
		}

		return ret;
	}

	public long[] calculateMoney(int[] winChi) {
		return new long[] { winChi[0] * gameMoney, winChi[1] * gameMoney, winChi[2] * gameMoney,
				winChi[3] * gameMoney };
	}

	public long[] calculateMoney(Player[] players, double[][] result, long[] addMoney) {
		if (players == null || result == null) {
			return null;
		}

		if (addMoney == null) {
			addMoney = new long[4];
		}

		long[] winMoney = new long[4];

		for (int i = 0; i < 4; i++) {
			if (players[i].getUser() != null) {
				double winChi = 0.0D;
				for (int j = 0; j < players.length; j++) {
					if (players[j].getUser() != null && j != i) {
						winChi += result[i][j];
					}
				}

				winMoney[i] = (long) ((winChi * gameMoney));
			}
		}

		int index = -1;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {

				if (players[i].getUser().getMoney() + winMoney[i] + addMoney[i] < 0L) {
					index = i;
					break;
				}
			}
		}

		if (index == -1) {
			return winMoney;
		}

		long[] temp = calculateMoneyForPlayer(index, players, result, addMoney[index]);
		for (int i = 0; i < players.length; i++) {
			addMoney[i] += temp[i];
		}

		User storedUser = players[index].getUser();
		players[index].setUser(null);
		winMoney = calculateMoney(players, result, addMoney);
		for (int i = 0; i < players.length; i++) {
			winMoney[i] += temp[i];
		}

		players[index].setUser(storedUser);
		return winMoney;
	}

	public long[] addBonusChi(Player[] players, long[] winMoney, int[] winChi) {
		if (players == null || winMoney == null || winChi == null) {
			return null;
		}

		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				winChi[i] += players[i].getBonusChi();
				winMoney[i] += players[i].getBonusMoney();
			}
		}
		return winMoney;
	}

	public long[] updateMoney(MauBinhGame controller, Player[] players, long[] winMoney, int[] winChi,
			Result[][] result) {
		if (controller == null || players == null || winMoney == null || winChi == null || result == null) {
			return null;
		}

		Iterator<Integer> localIterator;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				List<Integer> listmoney = new ArrayList<Integer>();
				if (winMoney[i] > Integer.MAX_VALUE || winMoney[i] < Integer.MIN_VALUE) {
					listmoney = splitBigMoney(winMoney[i]);
				} else {
					listmoney.add((int) winMoney[i]);
				}

				for (localIterator = listmoney.iterator(); localIterator.hasNext();) {
					int money = localIterator.next();

					if (winMoney[i] < 0L && money > 0) {
						money = -money;
					}

					if (money < 0) {
						money = -(int) Math.min(-money, players[i].getUser().getMoney());
					}

					// TODO lưu lại thông tin tiền cho người chơi, nhớ lúc lưu
					// thì có thông tin lý do, cộng trừ tiền
					//
					// controller.updateMoney(players[i].getUser(), money,
					// getDescriptionForPlayer(i, players, winChi, result));
				}
			}
		}

		return winMoney;
	}

	private List<Integer> splitBigMoney(long money) {
		List<Integer> listInt = new ArrayList<Integer>();
		long tempMoney = money;
		if (tempMoney < 0L) {
			tempMoney = -tempMoney;
		}
		while (tempMoney > Integer.MAX_VALUE) {
			listInt.add(MAX_MONEY);
			tempMoney -= MAX_MONEY;
		}
		listInt.add((int) tempMoney);
		return listInt;
	}

	public void updateMoneyForLeave(MauBinhGame controller, User leaver, int playerNo, Player[] players) {
		try {
			int value = (int) Math.min(leaver.getMoney(), MauBinhConfig.getInstance().getChiLeaveBonus() * gameMoney);

			// TODO update tiền lý do người chơi rời phòng
			// controller.updateMoney(user, -value*playerNo, description);

			for (int i = 0; i < players.length; i++) {
				User user = players[i].getUser();
				if (user != null) {
					// chia tiền
					players[i].addBonusMoney(value);
					// mỗi thằng ăn được 6 chi
					players[i].addBonusChi(MauBinhConfig.getInstance().getChiLeaveBonus());
				}
			}
		} catch (Exception e) {
			Tracer.error(MoneyManager.class, "[ERROR] updateMoneyForLeave fail!", e);
		}
	}

	private long[] calculateMoneyForPlayer(int index, Player[] players, double[][] result, long addMoney) {
		if ((players == null) || (result == null) || (index < 0) || (index >= players.length)
				|| (players[index] == null) || (players[index].getUser() == null)) {
			return null;
		}

		List<Integer> winnerIndex = new ArrayList<Integer>();
		List<Integer> loserIndex = new ArrayList<Integer>();
		double sumLoseChi = 0.0D;
		double sumWinChi = 0.0D;
		for (int i = 0; i < players.length; i++) {
			if ((players[i].getUser() != null) && (i != index)) {

				if (result[index][i] > 0.0D) {
					loserIndex.add(i);
					sumWinChi += result[index][i];
				}

				if (result[index][i] < 0.0D) {
					winnerIndex.add(i);
					sumLoseChi += result[index][i];
				}
			}
		}
		List<Double> loseRate = new ArrayList<Double>();
		for (int j = 0; j < winnerIndex.size(); j++) {
			loseRate.add(Double.valueOf(result[index][winnerIndex.get(j)] / sumLoseChi));
		}

		for (int i = 0; i < loserIndex.size(); i++) {
			for (int j = 0; j < winnerIndex.size(); j++) {
				double temp = result[index][loserIndex.get(i)] * loseRate.get(j);
				temp = Math.min(temp, -result[index][winnerIndex.get(j)]);

				result[index][winnerIndex.get(j)] += temp;
				result[winnerIndex.get(j)][index] -= temp;

				result[index][loserIndex.get(i)] -= temp;
				result[loserIndex.get(i)][index] += temp;

				result[winnerIndex.get(j)][loserIndex.get(i)] += temp;
				result[loserIndex.get(i)][winnerIndex.get(j)] -= temp;
			}
		}

		long[] ret = new long[players.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = 0L;
		}

		if (!winnerIndex.isEmpty() && result[index][winnerIndex.get(0)] < 0.0D) {
			double maxPayMoney = Math.min(-(sumLoseChi + sumWinChi) * gameMoney,
					players[index].getUser().getMoney() + addMoney);
			for (int j = 0; j < winnerIndex.size(); j++) {
				ret[winnerIndex.get(j)] = (long) (maxPayMoney * loseRate.get(j));
			}

			ret[index] = (long) (-maxPayMoney);
		}

		return ret;
	}
}
