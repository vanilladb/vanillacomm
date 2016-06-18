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

import org.vanilladb.comm.protocols.consensusUtils.Proposal;
import org.vanilladb.comm.protocols.utils.Debug;

/**
 * Class that implements the proposal made to consensus by ConsensusUTO protocol
 * 
 * 
 * @author MJo�oMonteiro
 */
public class OrderProposal extends Proposal {
    private static final long serialVersionUID = -8495751662017937316L;

    /**
     * Content of this array:
     * 
     * number_of_msg+(seq+intClassSize+className+port+intSourceSize+source+
     * intMsgSize+msg)*number_of_msg
     * 
     */
    byte[] bytes;

    public OrderProposal(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Proposal other) {

        final int LESS = -1; // this is less than other
        final int EQUAL = 0;
        final int GREATER = 1; // this is greater than other

        byte[] bytes_other = ((OrderProposal) other).bytes;

        int curPos_this = 0;
        int curPos_other = 0;

        int nElem_this = byteArrayToInt(bytes, curPos_this);
        curPos_this += 4;

        int nElem_other = byteArrayToInt(bytes_other, curPos_other);
        curPos_other += 4;

        if (nElem_this < nElem_other) {
            Debug.print("COMPARE: " + nElem_this + "<" + nElem_other);
            return GREATER;
        } else if (nElem_other < nElem_this) {
            Debug.print("COMPARE: " + nElem_other + "<" + nElem_this);
            return LESS;
        }

        // both OrderProposals have the same number of msgs!
        for (int g = 0; g < nElem_this; g++) {
            // seq
            int seq_this = byteArrayToInt(bytes, curPos_this);
            curPos_this += 4;
            int seq_other = byteArrayToInt(bytes_other, curPos_other);
            curPos_other += 4;
            // classname+classsize
            curPos_this += byteArrayToInt(bytes, curPos_this) + 4;
            curPos_other += byteArrayToInt(bytes_other, curPos_other) + 4;
            // port
            int port_this = byteArrayToInt(bytes, curPos_this);
            curPos_this += 4;
            int port_other = byteArrayToInt(bytes_other, curPos_other);
            curPos_other += 4;
            // source
            String host_this = new String(bytes, curPos_this + 4,
                    byteArrayToInt(bytes, curPos_this));
            curPos_this += byteArrayToInt(bytes, curPos_this) + 4;
            String host_other = new String(bytes_other, curPos_other + 4,
                    byteArrayToInt(bytes_other, curPos_other));
            curPos_other += byteArrayToInt(bytes_other, curPos_other) + 4;
            // msg+msgsize
            curPos_this += byteArrayToInt(bytes, curPos_this) + 4;
            curPos_other += byteArrayToInt(bytes_other, curPos_other) + 4;

            if (host_this.compareTo(host_other) < 0)
                return LESS;
            else if (host_other.compareTo(host_this) < 0)
                return GREATER;

            if (port_this < port_other)
                return LESS;
            else if (port_other < port_this)
                return GREATER;

            if (seq_this < seq_other)
                return LESS;
            else if (seq_other < seq_this)
                return GREATER;

        }
        return EQUAL;

    }

    float averageSeq(OrderProposal op) {
        int seqCounter = 0;
        int curPos = 0;
        int nElem = byteArrayToInt(bytes, curPos);
        // num_elem
        curPos += 4;

        for (int k = 0; k < nElem; k++) {
            seqCounter += byteArrayToInt(bytes, curPos);
            // seq
            curPos += 4;
            // classname+classsize
            curPos += byteArrayToInt(bytes, curPos) + 4;
            // port
            curPos += 4;
            // source+sourcesize
            curPos += byteArrayToInt(bytes, curPos) + 4;
            // msg+msgsize
            curPos += byteArrayToInt(bytes, curPos) + 4;
        }

        return seqCounter / nElem;
    }

    float averagePort(OrderProposal op) {
        int portCounter = 0;
        int curPos = 0;
        int nElem = byteArrayToInt(bytes, curPos);
        // num_elem
        curPos += 4;

        for (int k = 0; k < nElem; k++) {
            // seq
            curPos += 4;
            // classname+classsize
            curPos += byteArrayToInt(bytes, curPos) + 4;
            // counting the port!
            portCounter += byteArrayToInt(bytes, curPos);
            // port
            curPos += 4;
            // source+sourcesize
            curPos += byteArrayToInt(bytes, curPos) + 4;
            // msg+msgsize
            curPos += byteArrayToInt(bytes, curPos) + 4;
        }

        return portCounter / nElem;
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
}
