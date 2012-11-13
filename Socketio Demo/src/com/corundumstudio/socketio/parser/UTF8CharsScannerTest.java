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
package com.corundumstudio.socketio.parser;

import junit.framework.Assert;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

public class UTF8CharsScannerTest {

	@Test
	public void testfindTailIndex() {
    	String str = "132 4  \ufffd\ufffd  \\Привет";
    	UTF8CharsScanner p = new UTF8CharsScanner();
    	ChannelBuffer b = ChannelBuffers.wrappedBuffer(str.getBytes());
    	int len = p.findTailIndex(b, b.readerIndex(), b.capacity(), str.length());
    	Assert.assertEquals(b.capacity(), len);
	}

}
