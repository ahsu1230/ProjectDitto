/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 *  Copyright 2004 Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xmlpull.v1.xni2xmlpull1;

import org.xmlpull.v1.XmlPullParser;


public class X2StateQueue {
    private X2Iterator x2;
    private X2State[] queue;
    private int queueTail;
    private int queueHead;

    X2StateQueue(X2Iterator iter) {
        x2 = iter;
    }

    void clear() {
        queueHead = queueTail = 0;
        queue = new X2State[0];
    }

    boolean empty() {
        if(queueHead > queueTail) {
            throw new IllegalStateException();
        }

        return (queueHead == queueTail );
    }

    void reset() {
        if(queueHead > queueTail) {
            throw new IllegalStateException();
        }
        queueHead = queueTail = 0;
    }

    X2State append(X2StateType eventType) {
        if(queueTail >= queue.length) {
            X2State[] newQueue = new X2State[queueTail + 8];
            System.arraycopy(queue, 0, newQueue, 0, queue.length);
            for (int i = queue.length; i < newQueue.length; i++)
            {
                newQueue[ i ] = new X2State();
            }
            queue = newQueue;
        }
        X2State state = queue[ queueTail++ ];
        state.reset(eventType, x2);
        return state;
    }

    X2State peekBottom() {
        if(empty()) {
            throw new IllegalStateException("no events in queue");
        }
        return queue[queueHead];
    }

    X2State remove() {
        if(empty()) {
            throw new IllegalStateException("no events in queue");
        }
        return queue[ queueHead ++ ];
    }

    X2State top() {
        if(empty()) {
            throw new IllegalStateException("no events in queue");
        }
        return queue[queueTail - 1];
    }
}

