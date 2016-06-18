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

package org.vanilladb.comm.protocols.uniformFloodingConsensus;

import net.sf.appia.core.*;

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.vanilladb.comm.protocols.consensusUtils.Proposal;
import org.vanilladb.comm.protocols.events.ConsensusDecide;
import org.vanilladb.comm.protocols.events.ConsensusPropose;
import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;

/**
 * The Uniform Flooding Consensus implementation.
 * 
 * @author alexp
 */
public class UniformFloodingConsensusSession extends Session {

    public UniformFloodingConsensusSession(Layer layer) {
        super(layer);
    }

    private int round = -1;

    private ProcessSet correct = null;

    private Proposal decided = null;

    private List<HashSet<SampleProcess>> delivered = null;

    private HashSet<Proposal> proposal_set = null;

    public void handle(Event event) {
        if (event instanceof ProcessInitEvent)
            handleProcessInit((ProcessInitEvent) event);
        else if (event instanceof Crash)
            handleCrash((Crash) event);
        else if (event instanceof ConsensusPropose)
            handleConsensusPropose((ConsensusPropose) event);
        else if (event instanceof MySetEvent)
            handleMySet((MySetEvent) event);
        else {
            debug("Unwanted event received, ignoring.");
            try {
                event.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void init() {
        int max_rounds = correct.getSize();
        delivered = new ArrayList<HashSet<SampleProcess>>(max_rounds);
        proposal_set = new HashSet<Proposal>();
        for (int i = 0; i < max_rounds; i++) {
            delivered.add(i, new HashSet<SampleProcess>());
        }
        round = 0;
        decided = null;
    }

    private void handleProcessInit(ProcessInitEvent event) {
        correct = event.getProcessSet();
        init();
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleCrash(Crash crash) {
        correct.setCorrect(crash.getCrashedProcess(), false);
        try {
            crash.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        debug("received crash for " + crash.getCrashedProcess());

        decide(crash.getChannel());
    }

    private void handleConsensusPropose(ConsensusPropose propose) {
        proposal_set.add(propose.value);
        try {
            MySetEvent ev = new MySetEvent(propose.getChannel(),
                    Direction.DOWN, this);
            ev.getMessage().pushObject(proposal_set);
            ev.getMessage().pushInt(round);
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMySet(MySetEvent event) {
        SampleProcess p_i = correct.getProcess((SocketAddress) event.source);
        int round = event.getMessage().popInt();
        HashSet<Proposal> newSet = (HashSet<Proposal>) event.getMessage()
                .popObject();
        proposal_set.addAll(newSet);
        delivered.get(round).add(p_i);
        decide(event.getChannel());
    }

    /**
     * Determines if a decision can be made.
     * 
     * @param channel
     */
    private void decide(Channel channel) {
        int i;

        debugAll("decide");

        if (decided != null)
            return;

        for (i = 0; i < correct.getSize(); i++) {
            SampleProcess p = correct.getProcess(i);
            if ((p != null) && p.isCorrect()
                    && !delivered.get(round).contains(p))
                return;
        }

        if (round == delivered.size() - 1) {
            for (Proposal proposal : proposal_set) {
                if (decided == null)
                    decided = proposal;
                else if (proposal.compareTo(decided) < 0)
                    decided = proposal;
            }

            try {
                ConsensusDecide ev = new ConsensusDecide(channel, Direction.UP,
                        this);
                ev.decision = (Proposal) decided;
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }

            init();
        } else {
            round++;
            try {
                MySetEvent ev = new MySetEvent(channel, Direction.DOWN, this);
                ev.getMessage().pushObject(proposal_set);
                ev.getMessage().pushInt(round);
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    // DEBUG
    public static final boolean debugFull = false;

    private PrintStream debug = System.out;

    private void debug(String s) {
        if ((debug != null) && debugFull)
            debug.println(this.getClass().getName() + ": " + s);
    }

    private void debugAll(String s) {
        if ((debug == null) || !debugFull)
            return;

        debug.println();
        debug.println("DEBUG ALL - " + s);
        debug.println("\tround=" + round);
        debug.print("\tcorrect=");
        int i;
        for (i = 0; i < correct.getSize(); i++) {
            SampleProcess p = correct.getProcess(i);
            debug.print("[" + p.getProcessNumber() + ";" + p.getSocketAddress()
                    + ";" + p.isCorrect() + "],");
        }
        debug.println();

        debug.println("\tdecided=" + decided);
        for (HashSet<SampleProcess> list : delivered) {
            debug.print("\tcorrect_this_round[" + i + "]=");
            for (SampleProcess p : list)
                debug.print(p.getProcessNumber() + ",");
            debug.println();
        }

        debug.print("\tproposal_set=");
        for (Proposal o : proposal_set) {
            debug.print("(" + o.toString() + "),");
        }

        debug.println();
    }
}
