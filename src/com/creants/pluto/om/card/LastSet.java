package com.creants.pluto.om.card;

import com.creants.pluto.logic.MauBinhCardSet;
import com.creants.pluto.util.MauBinhConfig;

/**
 * Chi cuối
 * 
 * @author LamHa
 *
 */
public class LastSet extends BigSet {
	public int getWinChi() {
		switch (getType()) {
		case SetType.NOT_ENOUGH_CARD:
			return Integer.MIN_VALUE;
		case SetType.FOUR_OF_KIND:
			return getWinChiOfFourOfKind();
		case SetType.STRAIGHT_FLUSH:
			return getWinChiOfStraightFlush();
		}

		return 1;
	}

	private int getWinChiOfFourOfKind() {
		Card four = null;

		for (int i = 1; i < getCards().size(); i++) {
			if (getCards().get(i - 1).getCardNumber() == getCards().get(i).getCardNumber()) {
				four = getCards().get(i);
				break;
			}
		}

		if (four != null && MauBinhCardSet.isAce(four)) {
			return MauBinhConfig.getInstance().getChiLastFourOfKindAce();
		}

		return MauBinhConfig.getInstance().getChiLastFourOfKind();
	}

	/**
	 * Số chi thắng của Thùng Phá Sảnh
	 * 
	 * @return
	 */
	private int getWinChiOfStraightFlush() {
		int size = getCards().size();
		if (MauBinhCardSet.isAce(getCards().get(size - 1))) {
			if (MauBinhCardSet.is5(getCards().get(size - 2))) {
				return MauBinhConfig.getInstance().getChiLastStraightFlushA2345();
			}

			return MauBinhConfig.getInstance().getChiLastStraightFlush10JQKA();
		}

		return MauBinhConfig.getInstance().getChiLastStraightFlush();
	}
}
