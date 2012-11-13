/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.transport;

import java.util.UUID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.corundumstudio.socketio.AckManager;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.messages.XHRNewChannelMessage;
import com.corundumstudio.socketio.messages.XHRPacketMessage;
import com.corundumstudio.socketio.parser.Packet;

public class XHRPollingClient extends BaseClient {

    private String origin;

    public XHRPollingClient(AckManager ackManager, DisconnectableHub disconnectable, UUID sessionId) {
        super(sessionId, ackManager, disconnectable);
    }

    public void update(Channel channel, String origin) {
        this.origin = origin;
        this.channel = channel;
        channel.write(new XHRNewChannelMessage(getSessionId(), origin));
    }

    public String getOrigin() {
        return origin;
    }

    public ChannelFuture send(Packet packet) {
        return channel.write(new XHRPacketMessage(getSessionId(), origin, packet));
    }

}
