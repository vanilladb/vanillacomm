/*******************************************************************************
 * Copyright 2016 vanilladb.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.vanilladb.comm.messages;

import java.io.Serializable;

/**
 * A point to point message, will not be processed by any broadcast protocol.
 * 
 * @author mkliao
 */
public class P2pMessage implements Serializable {

	private static final long serialVersionUID = 6956549804435741217L;
	private Object message;
	private ChannelType group;
	private int receiver;

	/**
	 * 
	 * @param message
	 *            the message object of the point to point message
	 * @param receiver
	 *            the receiver node id
	 * @param group
	 *            the group the receiver belongs to, {@link CLIENT} or
	 *            {@link SERVER}
	 */
	public P2pMessage(Object message, int receiver, ChannelType group) {
		this.message = message;
		this.receiver = receiver;
		this.group = group;
	}

	/**
	 * 
	 * @return the message object of this point to point message
	 */
	public Object getMessage() {
		return message;
	}

	/**
	 * 
	 * @return the receiver node id
	 */
	public int getReceiver() {
		return receiver;
	}

	/**
	 * 
	 * @return the receiver group
	 */
	public ChannelType getGroup() {
		return group;
	}
}
