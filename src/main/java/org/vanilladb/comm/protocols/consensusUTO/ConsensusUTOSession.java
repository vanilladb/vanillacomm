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

package org.vanilladb.comm.protocols.consensusUTO;

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.vanilladb.comm.protocols.events.ConsensusDecide;
import org.vanilladb.comm.protocols.events.ConsensusPropose;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.utils.Debug;

/**
 * Consensus-based Uniform Total Order broadcast algorithm.
 * 
 * July 2003
 * 
 * @author MJo�oMonteiro
 */
public class ConsensusUTOSession extends Session {

    private SocketAddress iwp;

    private Channel channel;

    /* global sequence number of the message ssent by this process */
    private int seqNumber;

    /* Sequence number of the set of messages to deliver in the same round! */
    private int sn;

    /* Sets the beginning and the end of the rounds */
    private boolean wait;

    /*
     * Set of delivered messages.
     * 
     * unit: sendable event+seq number (ListElement)
     */
    private LinkedList<ListElement> delivered;

    /*
     * Set of unordered messages.
     * 
     * unit: sendable event+seq number (ListElement)
     */
    private LinkedList<ListElement> unordered;

    /**
     * Standard constructor
     * 
     * @param l
     *            the CONoWaintingLayer
     */
    public ConsensusUTOSession(Layer l) {
        super(l);
    }

    /**
     * The event handler function. Dispatches the new event to the appropriate
     * function.
     * 
     * @param e
     *            the event
     */
    public void handle(Event e) {
        if (e instanceof ChannelInit)
            handleChannelInit((ChannelInit) e);
        else if (e instanceof ProcessInitEvent)
            handleProcessInitEvent((ProcessInitEvent) e);
        else if (e instanceof SendableEvent) {
            if (e.getDir() == Direction.DOWN)
                handleSendableEventDOWN((SendableEvent) e);
            else
                handleSendableEventUP((SendableEvent) e);
        } else if (e instanceof ConsensusDecide)
            handleConsensusDecide((ConsensusDecide) e);
        else {
            try {
                e.go();
            } catch (AppiaEventException ex) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handle]"
                        + ex.getMessage());
            }
        }
    }

    /**
     * Handles channelInit event. Initializes the two lists.
     * 
     * @param e
     *            the channelinit event just arrived.
     */
    public void handleChannelInit(ChannelInit e) {
        Debug.print("TO: handle: " + e.getClass().getName());

        try {
            e.go();
        } catch (AppiaEventException ex) {
            Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handleCI]:1:"
                    + ex.getMessage());
        }

        this.channel = e.getChannel();

        delivered = new LinkedList<ListElement>();
        unordered = new LinkedList<ListElement>();

        sn = 1;
        wait = false;

    }

    /**
     * Handles process init event. Now, it's the right time to initialize the
     * consensus protocol.
     * 
     * @param e
     *            the sendable event.
     */
    public void handleProcessInitEvent(ProcessInitEvent e) {

        iwp = e.getProcessSet().getSelfProcess().getSocketAddress();

        try {
            e.go();
        } catch (AppiaEventException ex) {
            Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handlePI]:1:"
                    + ex.getMessage());
        }

        // initializing consensus protocol!
        // try {
        // ConsensusInit init= new ConsensusInit(channel,Direction.DOWN,this);
        // init.go();
        // } catch (AppiaEventException ex) {
        // Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handlePI]:2:" +
        // ex.getMessage());
        // }

    }

    /**
     * Handles sendable event to be sent to the network. Adds the sequence
     * number and forwards the message into the channel.
     * 
     * @param e
     *            the sendable event.
     */
    public void handleSendableEventDOWN(SendableEvent e) {
        Debug.print("TO: handle: " + e.getClass().getName() + " DOWN");

        Message om = e.getMessage();
        // inserting the global seq number of this msg
        om.pushInt(seqNumber);

        try {
            e.go();
        } catch (AppiaEventException ex) {
            Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handleDOWN]"
                    + ex.getMessage());
        }

        // increments the global seq number
        seqNumber++;
    }

    /**
     * Handles sendable event just arrived from the network.
     * 
     * @param e
     *            the sendable event.
     */
    public void handleSendableEventUP(SendableEvent e) {
        Debug.print("TO: handle: " + e.getClass().getName() + " UP");

        Message om = e.getMessage();
        int seq = om.popInt();

        // checks if the msg has already been delivered.
        ListElement le;
        if (!isDelivered((SocketAddress) e.source, seq)) {
            le = new ListElement(e, seq);
            unordered.add(le);
        }

        // let's see if we can start a new round!
        if (unordered.size() != 0 && !wait) {
            wait = true;
            // sends our proposal to consensus protocol!
            ConsensusPropose cp;
            byte[] bytes = null;
            try {
                cp = new ConsensusPropose(channel, Direction.DOWN, this);

                bytes = serialize(unordered);

                OrderProposal op = new OrderProposal(bytes);
                cp.value = op;

                cp.go();
                Debug.print("TO: handleUP: Proposta:");
                for (int g = 0; g < unordered.size(); g++) {
                    Debug.print("source:" + unordered.get(g).se.source
                            + " seq:" + unordered.get(g).seq);

                }
                Debug.print("TO: handleUP: Proposta feita!");

            } catch (AppiaEventException ex) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handleUP]"
                        + ex.getMessage());
            }
        }

    }

    /**
     * Handles consensus decide event just arrived from the consensus protocol.
     * 
     * @param e
     *            the consensus decide event.
     */
    public void handleConsensusDecide(ConsensusDecide e) {
        Debug.print("TO: handle: " + e.getClass().getName());

        LinkedList<ListElement> decided = deserialize(((OrderProposal) e.decision).bytes);

        // try {
        // BufferedWriter out = new BufferedWriter(new FileWriter("logDebug-"
        // + iwp.toString() + ".txt", true));
        // for (int y = 0; y < decided.size(); y++) {
        //
        // String aux = decided.get(y).se.getMessage().popString();
        // out.write(aux);
        // out.newLine();
        // decided.get(y).se.getMessage().pushString(aux);
        //
        // }
        // out.newLine();
        // out.close();
        // } catch (IOException ex) {
        // ex.printStackTrace();
        // System.exit(1);
        // }

        // The delivered list must be complemented with the msg in the decided
        // list!
        for (int i = 0; i < decided.size(); i++) {
            if (!isDelivered((SocketAddress) decided.get(i).se.source,
                    decided.get(i).seq)) {
                // if a msg that is in decided doesn't yet belong to delivered,
                // add it!
                delivered.add(decided.get(i));
            }
        }

        // update unordered list by removing the messages that are in the
        // delivered list
        for (int j = 0; j < unordered.size(); j++) {
            if (isDelivered((SocketAddress) unordered.get(j).se.source,
                    unordered.get(j).seq)) {
                unordered.remove(j);
                j--;
            }
        }

        decided = sort(decided);

        // deliver the messages in the decided list, which is already ordered!
        for (int k = 0; k < decided.size(); k++) {
            try {
                decided.get(k).se.go();
            } catch (AppiaEventException ex) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handleDecide]"
                        + ex.getMessage());
            }
        }
        sn++;
        wait = false;

        // re-initializing consensus protocol!
        // try {
        // ConsensusInit init= new ConsensusInit(channel,Direction.DOWN,this);
        // init.go();
        // } catch (AppiaEventException ex) {
        // Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:handlePI]:2:" +
        // ex.getMessage());
        // }

    }

    /*
     * Checks if the msg with seq number 'seq' and coming from 'source' has been
     * already delivered i.e, is already in the delivered list
     */
    boolean isDelivered(SocketAddress source, int seq) {

        for (int k = 0; k < delivered.size(); k++) {
            if (delivered.get(k).getSE().source.equals(source)
                    && delivered.get(k).getSeq() == seq)
                return true;
        }

        return false;
    }

    /*
     * Used to order deterministically the set of msg decided by consensus.
     * 
     * The order chosen in this implementation is the order that the msgs bring
     * in the array from consensus. That order is mantained, during the
     * deserialization process, in the list that is the argument of this
     * function.
     */
    LinkedList<ListElement> sort(LinkedList<ListElement> list) {
        return list;
    }

    // serialize
    // deserialize

    /**
     * int serialization.
     */
    byte[] intToByteArray(int i) {
        byte[] ret = new byte[4];

        ret[0] = (byte) ((i & 0xff000000) >> 24);
        ret[1] = (byte) ((i & 0x00ff0000) >> 16);
        ret[2] = (byte) ((i & 0x0000ff00) >> 8);
        ret[3] = (byte) (i & 0x000000ff);

        return ret;
    }

    /**
     * int deserialization.
     */
    int byteArrayToInt(byte[] b, int off) {
        int ret = 0;

        ret |= b[off] << 24;
        ret |= (b[off + 1] << 24) >>> 8; // must be done this way because of
        ret |= (b[off + 2] << 24) >>> 16; // java's sign extension of <<
        ret |= (b[off + 3] << 24) >>> 24;

        return ret;
    }

    /*
     * Unordered List serialization:
     * 
     * n_elem+(seq+int+classname+port+int+source+int+message)*n_elem
     */
    private byte[] serialize(LinkedList<ListElement> list) {

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] bytes = null;

        // number of elements of the list:int
        try {
            data.write(intToByteArray(list.size()));

            // now, serialize each element
            for (int i = 0; i < list.size(); i++) {

                // getting the list element
                ListElement le = list.get(i);
                // sequence number:int
                data.write(intToByteArray(le.seq));
                // class name
                bytes = le.se.getClass().getName().getBytes();
                data.write(intToByteArray(bytes.length));
                data.write(bytes, 0, bytes.length);
                // source port:int
                data.write(intToByteArray(((InetSocketAddress) le.se.source)
                        .getPort()));
                // source host:string
                String host = ((InetSocketAddress) le.se.source).getAddress()
                        .getHostAddress();
                bytes = host.getBytes();
                data.write(intToByteArray(bytes.length));
                data.write(bytes, 0, bytes.length);
                // message
                bytes = le.se.getMessage().toByteArray();
                data.write(intToByteArray(bytes.length));
                data.write(bytes, 0, bytes.length);
            }

        } catch (IOException e) {
            Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:serialize]"
                    + e.getMessage());
        }

        // creating the byte[]
        bytes = data.toByteArray();

        return bytes;
    }

    /*
     * Unordered List DEserialization:
     * 
     * The byte[] comes like:
     * n_elem+(seq+int+classname+port+int+source+int+message)*n_elem
     */
    private LinkedList<ListElement> deserialize(byte[] data) {

        LinkedList<ListElement> ret = new LinkedList<ListElement>();
        int curPos = 0;

        // getting the size of the list
        int listSize = byteArrayToInt(data, curPos);
        curPos += 4;

        // getting the elements of the list
        for (int i = 0; i < listSize; i++) {
            try {
                // seq number
                int seq = byteArrayToInt(data, curPos);
                curPos += 4;
                // class name
                int aux_size = byteArrayToInt(data, curPos);
                String className = new String(data, curPos + 4, aux_size);
                curPos += aux_size + 4;
                // creating the event
                SendableEvent se = null;

                se = (SendableEvent) Class.forName(className).newInstance();
                // format known event attributes
                se.setDir(Direction.UP);
                se.setSourceSession(this);
                se.setChannel(channel);

                // source:porto
                int port = byteArrayToInt(data, curPos);
                curPos += 4;
                // source:host
                aux_size = byteArrayToInt(data, curPos);
                String host = new String(data, curPos + 4, aux_size);
                curPos += aux_size + 4;
                se.source = new InetSocketAddress(InetAddress.getByName(host),
                        port);
                // finally, the message
                aux_size = byteArrayToInt(data, curPos);
                curPos += 4;

                se.getMessage().setByteArray(data, curPos, aux_size);
                curPos += aux_size;
                se.init();
                // creating the element that is the unit of the list
                ListElement le = new ListElement(se, seq);
                // adding this element to the list to return
                ret.add(le);

            } catch (InstantiationException e) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:DEserialize]:1: "
                        + e.getMessage());
            } catch (IllegalAccessException e) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:DEserialize]:2: "
                        + e.getMessage());
            } catch (ClassNotFoundException e) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:DEserialize]:3: "
                        + e.getMessage());
            } catch (UnknownHostException e) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:DEserialize]:4: "
                        + e.getMessage());
            } catch (AppiaEventException e) {
                Logger.getLogger(ConsensusUTOSession.class.getName()).fine("[ConsensusUTOSession:DEserialize]:5: "
                        + e.getMessage());
            }
        }// for

        return ret;
    }

}// end of session

class ListElement {
    /* the message */
    SendableEvent se;

    /* sequence number */
    int seq;

    public ListElement(SendableEvent se, int seq) {
        this.se = se;
        this.seq = seq;
    }

    SendableEvent getSE() {
        return se;
    }

    int getSeq() {
        return seq;
    }
}
