/*
 *
 * Hands-On code of the book Introduction to Reliable Distributed Programming
 * by Christian Cachin, Rachid Guerraoui and Luis Rodrigues
 * Copyright (C) 2005-2011 Luis Rodrigues
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * Contact
 * 	Address:
 *		Rua Alves Redol 9, Office 605
 *		1000-029 Lisboa
 *		PORTUGAL
 * 	Email:
 * 		ler@ist.utl.pt
 * 	Web:
 *		http://homepages.gsd.inesc-id.pt/~ler/
 * 
 */

package org.vanilladb.comm.protocols.events;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * Used to notify protocols that some process crashed.
 * 
 * @author nuno
 */
public class Crash extends Event {

    private int processRank;

    /**
     * Default constructor.
     */
    public Crash() {
        super();
    }

    /**
     * Constructor of this event.
     * 
     * @param pr
     *            the process rank that crashed.
     */
    public Crash(int pr) {
        super();
        processRank = pr;
    }

    /**
     * Constructor of this event.
     * 
     * @param channel
     *            Appia Channel
     * @param direction
     *            Direction of the event.
     * @param src
     *            session that created the event.
     * @param pr
     *            process rank that crashed.
     * @throws AppiaEventException
     */
    public Crash(Channel channel, int direction, Session src, int pr)
            throws AppiaEventException {
        super(channel, direction, src);
        processRank = pr;
    }

    /**
     * gets the crashed process rank.
     * 
     * @return crashed process rank
     */
    public int getCrashedProcess() {
        return processRank;
    }

}
