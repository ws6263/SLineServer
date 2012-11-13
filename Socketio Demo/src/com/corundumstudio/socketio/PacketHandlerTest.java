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
package com.corundumstudio.socketio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import mockit.Mocked;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.junit.Before;
import org.junit.Test;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.JacksonJsonSupport;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.transport.BaseClient;

public class PacketHandlerTest {

    private JsonSupport map = new JacksonJsonSupport(new Configuration());
    private Decoder decoder = new Decoder(map, new AckManager(null));
    private Encoder encoder = new Encoder(map);
    private NamespacesHub namespacesHub = new NamespacesHub(map);
    @Mocked
    private Channel channel;
    @Mocked
    private BaseClient client;
    private final AtomicInteger invocations = new AtomicInteger();

    @Before
    public void before() {
        if (namespacesHub.get(Namespace.DEFAULT_NAME) == null) {
            namespacesHub.create(Namespace.DEFAULT_NAME);
        }
        invocations.set(0);
    }

    private PacketListener createTestListener(final List<Packet> packets) {
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, SocketIOClient client, AckRequest ackRequest) {
                int index = invocations.incrementAndGet();
                Packet currentPacket = packets.get(index-1);
                Assert.assertEquals(currentPacket.getType(), packet.getType());
                Assert.assertEquals(currentPacket.getData(), packet.getData());
            }
        };
        return listener;
    }

    @Test
    public void testOnePacket() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    @Test
    public void testUTF8MultiplePackets() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet3 = new Packet(PacketType.CONNECT);
        packets.add(packet3);

        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "Данные"));
        packets.add(packet);

        Packet packet1 = new Packet(PacketType.JSON);
        packet1.setData(Collections.singletonMap("При\ufffdвет", "wq\ufffdeq"));
        packets.add(packet1);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    @Test
    public void testMultiplePackets() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet3 = new Packet(PacketType.CONNECT);
        packets.add(packet3);

        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        Packet packet1 = new Packet(PacketType.JSON);
        packet1.setData(Collections.singletonMap("fsdfdf", "wqeq"));
        packets.add(packet1);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    private void testHandler(PacketHandler handler, Queue<Packet> packets) throws Exception {
        int size = packets.size();
        ChannelBuffer buffer = encoder.encodePackets(packets);
        handler.messageReceived(null, new UpstreamMessageEvent(channel, new PacketsMessage(client, buffer), null));
        Assert.assertEquals(size, invocations.get());
    }

    //@Test
    public void testDecodePerf() throws Exception {
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, SocketIOClient client, AckRequest ackRequest) {
            }
        };
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        long start = System.currentTimeMillis();
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer("\ufffd10\ufffd3:::Привет\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::".getBytes());
        for (int i = 0; i < 50000; i++) {
            handler.messageReceived(null, new UpstreamMessageEvent(channel, new PacketsMessage(client, buffer), null));
            buffer.readerIndex(0);
        }
        long end = System.currentTimeMillis() - start;
        System.out.println(end + "ms");
        // 670ms
    }


}
