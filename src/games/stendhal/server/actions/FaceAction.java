/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.actions;

import games.stendhal.common.Direction;
import games.stendhal.server.entity.player.Player;
import marauroa.common.game.RPAction;

public class FaceAction implements ActionListener {

	private static final String _DIR = "dir";
	private static final String _FACE = "face";

	public static void register() {
		CommandCenter.register(_FACE, new FaceAction());
	}

	public void onAction(Player player, RPAction action) {


		if (action.has(_DIR)) {
			player.stop();
			player.setDirection(Direction.build(action.getInt(_DIR)));
			player.notifyWorldAboutChanges();
		}


	}
}
