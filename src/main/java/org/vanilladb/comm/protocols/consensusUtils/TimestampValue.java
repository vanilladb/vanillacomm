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
package org.vanilladb.comm.protocols.consensusUtils;

public class TimestampValue implements Comparable<TimestampValue> {
    private long tstamp;
    private PaxosProposal value;
    public TimestampValue(long ts, PaxosProposal v){
        this.tstamp = ts;
        this.value = v;
    }
    
    public long getTstamp(){
        return tstamp;
    }
    
    public PaxosProposal getPaxosProposal(){
        return value;
    }

    public int compareTo(TimestampValue o) {
        if(this.tstamp < o.getTstamp()){
            return -1;
        }
        else if(this.tstamp > o.getTstamp()){
            return 1;
        }
        else{
            return 0;
        }
    }
}
