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

package org.vanilladb.comm.protocols.floodingConsensus;

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.HashSet;

import org.vanilladb.comm.protocols.consensusUtils.Proposal;
import org.vanilladb.comm.protocols.events.ConsensusDecide;
import org.vanilladb.comm.protocols.events.ConsensusPropose;
import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.floodingConsensus.DecidedEvent;
import org.vanilladb.comm.protocols.floodingConsensus.MySetEvent;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * The Regular Flooding Consensus implementation.
 * 
 * @author alexp
 */
public class FloodingConsensusSession extends Session {

    public FloodingConsensusSession(Layer layer) {
        super(layer);
    }

    private int round = 0;

    private ProcessSet correct = null;

    private Proposal decided = null;

    private HashSet<SampleProcess>[] correct_this_round = null;

    private HashSet<Proposal>[] proposal_set = null;

    /**
     * @see appia.Session#handle(appia.Event)
     */
    public void handle(Event event) {
        if (event instanceof ProcessInitEvent)
            handleProcessInit((ProcessInitEvent) event);
        else if (event instanceof Crash)
            handleCrash((Crash) event);
        else if (event instanceof ConsensusPropose)
            handleConsensusPropose((ConsensusPropose) event);
        else if (event instanceof MySetEvent)
            handleMySet((MySetEvent) event);
        else if (event instanceof DecidedEvent)
            handleDecided((DecidedEvent) event);
        else {
            debug("Unwanted event received, ignoring.");
            try {
                event.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void init() {
        int max_rounds = correct.getSize() + 1;
        correct_this_round = new HashSet[max_rounds];
        proposal_set = new HashSet[max_rounds];
        int i;
        for (i = 0; i < max_rounds; i++) {
            correct_this_round[i] = new HashSet<SampleProcess>();
            proposal_set[i] = new HashSet<Proposal>();
        }
        for (i = 0; i < correct.getSize(); i++) {
            SampleProcess p = correct.getProcess(i);
            if (p.isCorrect())
                correct_this_round[0].add(p);
        }
        round = 1;
        decided = null;

        count_decided = 0;
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

        decide(crash.getChannel());
    }

    private void handleConsensusPropose(ConsensusPropose propose) {
        proposal_set[round].add(propose.value);
        try {

            MySetEvent ev = new MySetEvent(propose.getChannel(),
                    Direction.DOWN, this);
            ev.getMessage().pushObject(proposal_set[round]);
            ev.getMessage().pushInt(round);
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        decide(propose.getChannel());
    }

    @SuppressWarnings("unchecked")
    private void handleMySet(MySetEvent event) {
        SampleProcess p_i = correct.getProcess((SocketAddress) event.source);
        int r = event.getMessage().popInt();
        HashSet<Proposal> set = (HashSet<Proposal>) event.getMessage()
                .popObject();
        correct_this_round[r].add(p_i);
        proposal_set[r].addAll(set);
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
                    && !correct_this_round[round].contains(p))
                return;
        }

        if (correct_this_round[round].equals(correct_this_round[round - 1])) {

            for (Proposal proposal : proposal_set[round])
                if (decided == null)
                    decided = proposal;
                else if (proposal.compareTo(decided) < 0)
                    decided = proposal;

            try {
                ConsensusDecide ev = new ConsensusDecide(channel, Direction.UP,
                        this);
                ev.decision = (Proposal) decided;
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }

            try {
                DecidedEvent ev = new DecidedEvent(channel, Direction.DOWN,
                        this);
                ev.getMessage().pushObject(decided);
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        } else {
            round++;
            proposal_set[round].addAll(proposal_set[round - 1]);
            try {
                MySetEvent ev = new MySetEvent(channel, Direction.DOWN, this);
                ev.getMessage().pushObject(proposal_set[round]);
                ev.getMessage().pushInt(round);
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }

            count_decided = 0;
        }
    }

    private void handleDecided(DecidedEvent event) {
        // Counts the number os Decided messages received and reinitiates the
        // algorithm
        if ((++count_decided >= correctSize()) && (decided != null)) {
            init();
            return;
        }

        if (decided != null)
            return;

        SampleProcess p_i = correct.getProcess((SocketAddress) event.source);
        if (!p_i.isCorrect())
            return;

        decided = (Proposal) event.getMessage().popObject();

        try {
            ConsensusDecide ev = new ConsensusDecide(event.getChannel(),
                    Direction.UP, this);
            ev.decision = decided;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        try {
            DecidedEvent ev = new DecidedEvent(event.getChannel(),
                    Direction.DOWN, this);
            ev.getMessage().pushObject(decided);
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }

        round = 0;
    }

    // Used to count the number of Decided messages received, therefore
    // determining when
    // all processes have decided and therefore allow a new decision process.
    private int count_decided;

    private int correctSize() {
        int size = 0, i;
        SampleProcess[] processes = correct.getAllProcesses();
        for (i = 0; i < processes.length; i++) {
            if ((processes[i] != null) && processes[i].isCorrect())
                ++size;
        }
        return size;
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
        for (i = 0; i < correct_this_round.length; i++) {
            debug.print("\tcorrect_this_round[" + i + "]=");

            for (SampleProcess p : correct_this_round[i])
                debug.print(p.getProcessNumber() + ",");

            debug.println();
        }
        for (i = 0; i < proposal_set.length; i++) {
            debug.print("\tproposal_set[" + i + "]=");
            for (Proposal prop : proposal_set[i])
                debug.print("(" + prop.toString() + "),");
            debug.println();
        }
        debug.println();
    }
}
