package com.creants.pluto.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avengers.netty.gamelib.key.NetworkConstant;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.card.Card;
import com.creants.pluto.om.card.CardSet;

/**
 * @author LamHa
 *
 */
public class FinishRequestHandler extends AbstractRequestHandler {
	private static final Logger LOG = LoggerFactory.getLogger(FinishRequestHandler.class);

	@Override
	public void handleRequest(User user, Message message) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("[DEBUG] Arrange Finished [username:%s]", user.getUserName()));
		}

		byte[] blob = message.getBlob(NetworkConstant.KEYBLOB_CARD_LIST);
		List<Card> listCards = new ArrayList<Card>(13);
		for (int i = 0; i < 13; i++) {
			listCards.add(CardSet.getCard(blob[i]));
		}
		gameLogic.processBinhFinish(user, listCards);
	}

}
