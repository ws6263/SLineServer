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
package com.corundumstudio.socketio.messages;

import java.io.IOException;
import java.util.UUID;

import org.jboss.netty.channel.Channel;

import com.corundumstudio.socketio.MessageHandler;

public class AuthorizeMessage extends BaseMessage {

    private final UUID sessionId;
    private final String origin;
    private final String msg;
    private final String jsonpParam;

    public AuthorizeMessage(String msg, String jsonpParam, String origin, UUID sessionId) {
        this.origin = origin;
        this.msg = msg;
        this.jsonpParam = jsonpParam;
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getMsg() {
        return msg;
    }

    public String getJsonpParam() {
        return jsonpParam;
    }

    @Override
    public void handleMessage(MessageHandler handler, Channel channel) throws IOException {
        handler.handle(this, channel);
    }

}
