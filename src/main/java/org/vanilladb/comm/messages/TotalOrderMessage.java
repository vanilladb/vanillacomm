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
 * 
 * @author mkliao
 *
 */
public class TotalOrderMessage implements Serializable {

	private static final long serialVersionUID = 8807383803517134106L;
	private long totalOrderSequenceNumber;
	private Object[] messages;
	private long totalOrderIdStart;

	public TotalOrderMessage(Object[] msgs) {
		this.messages = msgs;
		this.totalOrderIdStart = -1;
	}

	public Object[] getMessages() {
		return messages;
	}

	public long getTotalOrderIdStart() {
		return totalOrderIdStart;
	}

	public void setTotalOrderIdStart(long totalOrderId) {
		this.totalOrderIdStart = totalOrderId;
	}

	public long getTotalOrderSequenceNumber(){
		return totalOrderSequenceNumber;
	}
	
	public void setTotalOrderSequenceNumber(long tosn){
		this.totalOrderSequenceNumber = tosn;
	}
}
