package com.creants.pluto.om.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.creants.pluto.logic.MauBinhCardSet;
import com.creants.pluto.om.MauBinhType;
import com.creants.pluto.om.Result;
import com.creants.pluto.util.MauBinhConfig;

/**
 * Danh sách bài mà người chơi đang nắm
 * 
 * @author LamHa
 *
 */
public class Cards {
	private List<Card> cards;
	private SmallSet set01;
	private MiddleSet set02;
	private LastSet set03;
	private byte maubinhType;

	public Cards() {
		cards = new ArrayList<Card>();

		set01 = new SmallSet();
		set02 = new MiddleSet();
		set03 = new LastSet();

		maubinhType = MauBinhType.NOT_MAU_BINH;
	}

	public List<Card> list() {
		return cards;
	}

	public void setCards(List<Card> listcards) {
		if (listcards.size() != 13) {
			return;
		}

		cards = listcards;
	}

	/**
	 * Lấy danh sách card đã sắp xếp
	 * 
	 * @return
	 */
	public List<Card> getArrangeCards() {
		if (!isFinishArrangement()) {
			return null;
		}

		List<Card> ret = new ArrayList<Card>();
		ret.addAll(get1stSet().getCards());
		ret.addAll(get2ndSet().getCards());
		ret.addAll(get3rdSet().getCards());
		return ret;
	}

	public void clearArrangement() {
		set01.clear();
		set02.clear();
		set03.clear();

		if (maubinhType == MauBinhType.THREE_STRAIGHT || maubinhType == MauBinhType.THREE_FLUSH) {
			maubinhType = MauBinhType.NOT_MAU_BINH;
		}
	}

	/**
	 * Set kiểu mau binh sau khi được sắp xếp
	 */
	public void setMauBinhTypeAfterArrangement() {
		if (maubinhType != MauBinhType.NOT_MAU_BINH) {
			return;
		}

		if (set01.isFlush() && set02.isFlush() && set03.isFlush()) {
			maubinhType = MauBinhType.THREE_FLUSH;
			return;
		}

		if (set01.isStraight() && set02.isStraight() && set03.isStraight()) {
			maubinhType = MauBinhType.THREE_STRAIGHT;
			return;
		}
	}

	public boolean receivedCard(Card card) {
		if (cards.size() > 13) {
			return false;
		}

		for (Card card2 : cards) {
			if (card.getId() == card2.getId()) {
				return false;
			}
		}

		cards.add(card);
		Collections.sort(cards);

		if (cards.size() == 13) {
			setMauBinhType();
		}

		return true;
	}

	public boolean receivedCardTo1stSet(Card card) {
		if (!isContainCard(card)) {
			return false;
		}

		return get1stSet().receivedCard(card);
	}

	public boolean receivedCardTo2ndSet(Card card) {
		if (!isContainCard(card)) {
			return false;
		}

		return get2ndSet().receivedCard(card);
	}

	public boolean receivedCardTo3rdSet(Card card) {
		if (!isContainCard(card)) {
			return false;
		}

		return get3rdSet().receivedCard(card);
	}

	public byte getMauBinhType() {
		return maubinhType;
	}

	public boolean isEnoughCard() {
		return list().size() == 13;
	}

	public boolean isFinishArrangement() {
		return get1stSet().getType() != -1 && get2ndSet().getType() != -1 && get3rdSet().getType() != -1;
	}

	/**
	 * Đếm số xì
	 * 
	 * @return
	 */
	public int getNumberOfAce() {
		if (cards == null || cards.isEmpty()) {
			return 0;
		}

		int ret = 0;
		int lastIndex = cards.size() - 1;
		int temp = Math.min(lastIndex + 1, 4);
		for (int i = 0; i < temp; i++) {
			if (!MauBinhCardSet.isAce(cards.get(lastIndex - i)))
				break;
			ret++;
		}

		return ret;
	}

	/**
	 * Bin lủng
	 * 
	 * @return
	 */
	public boolean isFailedArrangement() {
		if (isMauBinh()) {
			return false;
		}

		// 3 chi có đủ bài hay không
		if (!get1stSet().isEnough() || !get2ndSet().isEnough() || !get3rdSet().isEnough()) {
			return true;
		}

		// so sánh 3 chi xem có bị lủng không
		int result01 = get3rdSet().compareWith(get2ndSet());
		int result02 = get2ndSet().compareWith(get1stSet());
		return result01 == Integer.MIN_VALUE || result02 == Integer.MIN_VALUE || result01 == -1 || result02 == -1;
	}

	/**
	 * So bài
	 * 
	 * @param cards
	 * @return
	 */
	public Result compareWith(Cards cards) {
		if (cards == null || cards.list() == null || !isEnoughCard() || !cards.isEnoughCard()) {
			return null;
		}

		if (!isMauBinh()) {
			if (!cards.isMauBinh()) {
				return compareNotMauBinhWithNotMauBinh(cards);
			}

			// thằng kia mậu binh
			Result result = new Result();
			result.setWinChiMauBinh(-cards.getMauBinhWinChi());
			return result;
		}

		if (!cards.isMauBinh()) {
			Result result = new Result();
			result.setWinChiMauBinh(getMauBinhWinChi());
			return result;
		}

		return compareMauBinhWithMauBinh(cards);
	}

	/**
	 * Có phải bài mậu binh không
	 * 
	 * @return
	 */
	public boolean isMauBinh() {
		return maubinhType != MauBinhType.NOT_MAU_BINH;
	}

	public String getName() {
		if (isFailedArrangement())
			return MauBinhConfig.getInstance().getNameFailedArrangement();

		if (isMauBinh()) {
			return getMauBinhName();
		}
		return String.format("%s-%s-%s",
				new Object[] { get1stSet().getName(), get2ndSet().getName(), get3rdSet().getName() });
	}

	/**
	 * Lấy tên loại mậu binh theo id mậu binh
	 * 
	 * @return
	 */
	public String getMauBinhName() {
		switch (getMauBinhType()) {
		case 0:
			return MauBinhConfig.getInstance().getNameMauBinhSixPair();
		case 1:
			return MauBinhConfig.getInstance().getNameMauBinhThreeStraight();
		case 2:
			return MauBinhConfig.getInstance().getNameMauBinhThreeFlush();
		case 3:
			return MauBinhConfig.getInstance().getNameMauBinhSameColor12();
		case 4:
			return MauBinhConfig.getInstance().getNameMauBinhSixPairWithThree();
		case 5:
			return MauBinhConfig.getInstance().getNameMauBinhStraight13();
		case 6:
			return MauBinhConfig.getInstance().getNameMauBinhFourOfThree();
		case 7:
			return MauBinhConfig.getInstance().getNameMauBinhSameColor13();
		}

		return MauBinhConfig.getInstance().getNameNotMauBinh();
	}

	private boolean isContainCard(Card card) {
		for (Card card2 : cards) {
			if (card.getId() == card2.getId()) {
				return true;
			}
		}

		return false;
	}

	private SmallSet get1stSet() {
		return set01;
	}

	private BigSet get2ndSet() {
		return set02;
	}

	private BigSet get3rdSet() {
		return set03;
	}

	/**
	 * Mỗi loại mậu binh sẽ tương ứng với thắng bao nhiêu chi
	 * 
	 * @return
	 */
	private int getMauBinhWinChi() {
		switch (getMauBinhType()) {
		case 0:
			return MauBinhConfig.getInstance().getChiMauBinhSixPair();
		case 1:
			return MauBinhConfig.getInstance().getChiMauBinhThreeStraight();
		case 2:
			return MauBinhConfig.getInstance().getChiMauBinhThreeFlush();
		case 3:
			return MauBinhConfig.getInstance().getChiMauBinhSameColor12();
		case 4:
			return MauBinhConfig.getInstance().getChiMauBinhSixPairWithThree();
		case 5:
			return MauBinhConfig.getInstance().getChiMauBinhStraight13();
		case 6:
			return MauBinhConfig.getInstance().getChiMauBinhFourOfThree();
		case 7:
			return MauBinhConfig.getInstance().getChiMauBinhSameColor13();
		}

		return Integer.MIN_VALUE;
	}

	/**
	 * So 2 bài không phải mau binh với nhau
	 * 
	 * @param cards
	 *            danh sách bài cần so
	 * @return
	 */
	private Result compareNotMauBinhWithNotMauBinh(Cards cards) {
		if (isFailedArrangement()) {
			if (cards.isFailedArrangement()) {
				return new Result();
			}
			
			return cards.compareNotMauBinhWithFailed().getNegative();
		}

		if (cards.isFailedArrangement()) {
			return compareNotMauBinhWithFailed();
		}

		Result ret = new Result();
		boolean isWinThreeSet = true;
		boolean isLoseThreeSet = true;

		int result = get1stSet().getWinChiInComparisonWith(cards.get1stSet());
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi01(result);
		isWinThreeSet = isWinThreeSet && result > 0;
		isLoseThreeSet = isLoseThreeSet && result < 0;

		result = get2ndSet().getWinChiInComparisonWith(cards.get2ndSet());
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi02(result);
		isWinThreeSet = isWinThreeSet && result > 0;
		isLoseThreeSet = isLoseThreeSet && result < 0;
		result = get3rdSet().getWinChiInComparisonWith(cards.get3rdSet());
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi03(result);
		isWinThreeSet = isWinThreeSet && result > 0;
		isLoseThreeSet = isLoseThreeSet && result < 0;
		// nếu thằng 3 chi hoặc thua cả 3 chi
		if (isWinThreeSet || isLoseThreeSet) {
			ret.setMultiK(MauBinhConfig.getInstance().getChiWinThreeSetRate());
		}

		// ret.setWinChiAce(getNumberOfAce() - cards.getNumberOfAce());
		return ret;
	}

	private Result compareNotMauBinhWithFailed() {
		Result ret = new Result();

		int result = get1stSet().getWinChi();
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi01(result);

		result = get2ndSet().getWinChi();
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi02(result);

		result = get3rdSet().getWinChi();
		if (result == Integer.MIN_VALUE) {
			return null;
		}

		ret.setWinChi03(result);
		// ret.setMultiK(MauBinhConfig.getInstance().getChiWinThreeSetRate());
		// ret.setWinChiAce(getNumberOfAce());

		return ret;
	}

	private Result compareMauBinhWithMauBinh(Cards cards) {
		Result result = new Result();

		if (getMauBinhType() < cards.getMauBinhType()) {
			result.setWinChiMauBinh(-cards.getMauBinhWinChi());
		} else if (getMauBinhType() > cards.getMauBinhType()) {
			result.setWinChiMauBinh(getMauBinhWinChi());
		} else {
			result.setWinChiMauBinh(0);
		}

		return result;
	}

	private void setMauBinhType() {
		if (cards == null || cards.isEmpty() || cards.size() < 13) {
			maubinhType = MauBinhType.NOT_MAU_BINH;
			return;
		}

		int redCardNo = 0;
		if (MauBinhCardSet.isRed(cards.get(0))) {
			redCardNo++;
		}

		// đếm số đôi, số lượng 3 lá, số lượng 4 lá
		int pairNo = 0;
		int threeNo = 0;
		int fourNo = 0;
		int sameCardNo = 0;
		for (int i = 1; i < cards.size(); i++) {
			if (MauBinhCardSet.isRed(cards.get(i))) {
				redCardNo++;
			}

			// bài được xếp tăng dần lấy trước so sau
			if (cards.get(i).getCardNumber() == cards.get(i - 1).getCardNumber()) {
				sameCardNo++;
				continue;
			}

			switch (sameCardNo) {
			case 0:
				break;
			case 1:
				pairNo++;
				break;
			case 2:
				threeNo++;
				break;
			case 3:
				fourNo++;
				break;
			}

			sameCardNo = 0;
		}

		switch (sameCardNo) {
		case 0:
			break;
		case 1:
			pairNo++;
			break;
		case 2:
			threeNo++;
			break;
		case 3:
			fourNo++;
			break;
		}

		// cùng màu đỏ, hoặc cùng màu đen
		if (redCardNo == 13 || redCardNo == 0) {
			maubinhType = MauBinhType.SAME_COLOR_13;
			return;
		}

		// 3 xám và 4 con giống nhau
		if (threeNo + fourNo == 4) {
			maubinhType = MauBinhType.FOUR_OF_THREE;
			return;
		}

		// không có đôi, 3 con, 4 con liên tiếp là sảnh 13 lá
		if (pairNo == 0 && threeNo == 0 && fourNo == 0) {
			maubinhType = MauBinhType.STRAIGHT_13;
			return;
		}

		// 5 cập (fourNo để kiểm tra xem trong đó có 4 lá giống nhau ko) với 3
		// lá giống nhau là lục phé bổn
		if ((pairNo + fourNo * 2) == 5 && threeNo == 1) {
			maubinhType = MauBinhType.SIX_PAIR_WITH_THREE;
			return;
		}

		// 3 cái thùng
		if (set01.isFlush() && set02.isFlush() && set03.isFlush()) {
			maubinhType = MauBinhType.THREE_FLUSH;
			return;
		}

		// 3 cái sảnh
		if (set01.isStraight() && set02.isStraight() && set03.isStraight()) {
			maubinhType = MauBinhType.THREE_STRAIGHT;
			return;
		}

		// 6 đôi
		if (pairNo + fourNo * 2 == 6) {
			maubinhType = MauBinhType.SIX_PAIR;
			return;
		}

		maubinhType = MauBinhType.NOT_MAU_BINH;
	}
}
