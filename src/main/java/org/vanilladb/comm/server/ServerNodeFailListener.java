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
package org.vanilladb.comm.server;

import org.vanilladb.comm.messages.ChannelType;
/**
 * Defines a class that receives a failed node.
 * 
 * @author mkliao
 * 
 */
public interface ServerNodeFailListener {

	/**
	 * 
	 * @param id
	 *            the failed node id
	 * @param group
	 *            the group of the failed node
	 * @param channel
	 *            the channel the crash event delivered from
	 */
	public void onNodeFail(int id, ChannelType channelType);
}
