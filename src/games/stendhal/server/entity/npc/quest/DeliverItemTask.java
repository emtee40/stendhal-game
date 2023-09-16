/***************************************************************************
 *                   (C) Copyright 2003-2023 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.entity.npc.quest;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import games.stendhal.common.Rand;
import games.stendhal.common.grammar.Grammar;
import games.stendhal.common.parser.Sentence;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.entity.Outfit;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.npc.ChatAction;
import games.stendhal.server.entity.npc.ChatCondition;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.EventRaiser;
import games.stendhal.server.entity.npc.NPCList;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.action.SetQuestToTimeStampAction;
import games.stendhal.server.entity.npc.condition.AlwaysFalseCondition;
import games.stendhal.server.entity.npc.condition.OutfitCompatibleWithClothesCondition;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.util.StringUtils;


public class DeliverItemTask extends QuestTaskBuilder {

	private Outfit outfit;
	private String itemDescription;
	private String itemName;
	/*
					npc.say(StringUtils.substitute("You must bring this [flavor] to [customerName] within [time]. Say \"pizza\" so that [customerName] knows that I sent you. Oh, and please wear this uniform on your way.", params));
					npc.say("Come back when you have space to carry the pizza!");

					npc.say(StringUtils.substitute("I see you failed to deliver the pizza to [customerName] in time. Are you sure you will be more reliable this time?", params));
					npc.say(StringUtils.substitute("You still have to deliver a pizza to [customerName], and hurry!", params));
	}
	*/

	private Map<String, DeliverItemOrder> orders = new HashMap<>();

	// hide constructor
	DeliverItemTask() {
		super();
	}

	public DeliverItemTask itemName(String itemName) {
		this.itemName = itemName;
		return this;
	}

	/**
	 * The description of an item: You see a [flavor] for [customerName].
	 *
	 * @param itemDescription description of item
	 * @return DeliverItemTask
	 */
	public DeliverItemTask itemDescription(String itemDescription) {
		this.itemDescription = itemDescription;
		return this;
	}

	public DeliverItemTask outfitUniform(Outfit outfit) {
		this.outfit = outfit;
		return this;
	}

	public DeliverItemOrder order() {
		return new DeliverItemOrder(this);
	}

	/**
	 * Get a list of customers appropriate for a player
	 *
	 * @param player the player doing the quest
	 * @return list of customer data
	 */
	private List<String> getAllowedCustomers(Player player) {
		List<String> allowed = new LinkedList<String>();
		int level = player.getLevel();
		for (Map.Entry<String, DeliverItemOrder> entry : orders.entrySet()) {
			if (level >= entry.getValue().getLevel()) {
				allowed.add(entry.getKey());
			}
		}
		return allowed;
	}

	/**
	 * Checks whether the player has failed to fulfil his current delivery job
	 * in time.
	 *
	 * @param player
	 *            The player.
	 * @param questSlot
	 *            Name of the quest slot
	 * @return true if the player is too late. false if the player still has
	 *         time, or if he doesn't have a delivery to do currently.
	 */
	boolean isDeliveryTooLate(final Player player, String questSlot) {
		if (player.hasQuest(questSlot) && !player.isQuestCompleted(questSlot)) {
			final String[] questData = player.getQuest(questSlot).split(";");
			final String customerName = questData[0];
			final DeliverItemOrder customerData = orders.get(customerName);
			final long bakeTime = Long.parseLong(questData[1]);
			final long expectedTimeOfDelivery = bakeTime
				+ (long) 60 * 1000 * customerData.getExpectedMinutes();
			if (System.currentTimeMillis() > expectedTimeOfDelivery) {
				return true;
			}
		}
		return false;

	}

	/** Takes away the player's uniform, if the he is wearing it.
	 * @param player to remove uniform from*/
	void putOffUniform(final Player player) {
		if ((outfit != null) && outfit.isPartOf(player.getOutfit())) {
			player.returnToOriginalOutfit();
		}
	}

	private void prepareBaker() {
		SpeakerNPC npc = NPCList.get().get("Leander");
		for (final String name : orders.keySet()) {
			final DeliverItemOrder data = orders.get(name);
			npc.addReply(name, data.getNpcDescription());
		}
	}

	@Override
	void simulate(QuestSimulator simulator) {
		// TODO Auto-generated method stub
		
	}

	@Override
	ChatAction buildStartQuestAction(String questSlot) {
		prepareBaker(); // TODO
		return new ChatAction() {
			@Override
			public void fire(final Player player, final Sentence sentence, final EventRaiser npc) {
				final String name = Rand.rand(getAllowedCustomers(player));
				final DeliverItemOrder data = orders.get(name);

				Map<String, String> params = new HashMap<>();
				params.put("flavor", data.getFlavor());
				params.put("customerName", Grammar.quoteHash("#" + name));
				params.put("time", Grammar.quantityplnoun(data.getExpectedMinutes(), "minute", "one"));

				final Item pizza = SingletonRepository.getEntityManager().getItem("pizza");
				pizza.setInfoString(data.getFlavor());
				pizza.setDescription(StringUtils.substitute(itemDescription, params));
				pizza.setBoundTo(player.getName());

				if (player.equipToInventoryOnly(pizza)) {
					npc.say(StringUtils.substitute("You must bring this [flavor] to [customerName] within [time]. Say \"pizza\" so that [customerName] knows that I sent you. Oh, and please wear this uniform on your way.", params));
					if (outfit != null) {
						player.setOutfit(outfit, true);
					}
					player.setQuest(questSlot, 0, name);
					new SetQuestToTimeStampAction(questSlot, 1).fire(player, null, npc);
				} else {
					npc.say("Come back when you have space to carry the pizza!");
				}
			}
		};
	}

	@Override
	ChatCondition buildQuestPreCondition(String questSlot) {
		return new OutfitCompatibleWithClothesCondition();
	}


	@Override
	ChatAction buildRejectQuestAction(String questSlot) {
		return new ChatAction() {
			@Override
			public void fire(final Player player, final Sentence sentence, final EventRaiser npc) {
				putOffUniform(player);
			}
		};
	}

	@Override
	ChatAction buildRemindQuestAction(String questSlot) {
		return new ChatAction() {
			@Override
			public void fire(final Player player, final Sentence sentence, final EventRaiser npc) {
					final String customerName = player.getQuest(questSlot, 0);
					if (customerName.equals("rejected")) {
						buildStartQuestAction(questSlot).fire(player, sentence, npc);
						return;
					}
					Map<String, String> params = new HashMap<>();
					params.put("customerName", Grammar.quoteHash("#" + customerName));

					if (isDeliveryTooLate(player, questSlot)) {
						// If the player still carries any pizza due for an NPC,
						// take it away because the baker is angry,
						// and because the player probably won't
						// deliver it anymore anyway.
						for (final Item pizza : player.getAllEquipped("pizza")) {
							if (pizza.getInfoString()!=null) {
								player.drop(pizza);
							}
						}
						npc.say(StringUtils.substitute("I see you failed to deliver the pizza to [customerName] in time. Are you sure you will be more reliable this time?", params));
						npc.setCurrentState(ConversationStates.QUEST_OFFERED);
					} else {
						npc.say(StringUtils.substitute("You still have to deliver a pizza to [customerName], and hurry!", params));
						npc.setCurrentState(ConversationStates.ATTENDING);
					}
			}
		};
	}

	@Override
	ChatCondition buildQuestCompletedCondition(String questSlot) {
		return new AlwaysFalseCondition();
	}

	@Override
	ChatAction buildQuestCompleteAction(String questSlot) {
		return null;
	}

	Map<String, DeliverItemOrder> getOrders() {
		return this.orders;
	}

	String getItemName() {
		return itemName;
	}

	@Override
	List<String> calculateHistoryProgress(QuestHistoryBuilder historyBuilder, Player player, String questSlot) {
		DeliverItemQuestHistoryBuilder history = (DeliverItemQuestHistoryBuilder) historyBuilder;
		List<String> res = new LinkedList<>();
		final String questState = player.getQuest(questSlot, 0);
		if (!"done".equals(questState)) {
			final String[] questData = questState.split(";");
			final String customerName = questData[0];
			final DeliverItemOrder customerData = orders.get(customerName);
			Map<String, String> params = new HashMap<>();
			params.put("flavor", customerData.getFlavor());
			params.put("customerName", customerName);
			params.put("customerDescription", customerData.getNpcDescription());
			res.add(StringUtils.substitute(history.getWhenItemWasGiven(), params));
			res.add(StringUtils.substitute(history.getWhenToldAboutCustomer(), params));
			if (!isDeliveryTooLate(player, questSlot)) {
				res.add(StringUtils.substitute(history.getWhenInTime(), params));
			} else {
				res.add(StringUtils.substitute(history.getWhenOutOfTime(), params));
			}
		}
		return res;
	}

}
