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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.node.ObjectNode;

import com.corundumstudio.socketio.Configuration;

public class JacksonJsonSupport implements JsonSupport {

    private class JsonObjectDeserializer extends StdDeserializer<JsonObject> {

        final Set<Class<?>> classes = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

        protected JsonObjectDeserializer() {
            super(JsonObject.class);
        }

        @Override
        public JsonObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            JsonNode rootNode = mapper.readTree(jp);
            if (!rootNode.isObject()) {
                return null;
            }

            Object val = readObject(mapper, rootNode);
            return new JsonObject(val);
        }

        private Object readObject(ObjectMapper mapper, JsonNode rootNode) throws IOException,
                JsonParseException, JsonMappingException {
            Class<?> clazz = Object.class;
            ObjectNode root = (ObjectNode) rootNode;
            JsonNode node = root.remove(configuration.getJsonTypeFieldName());
            if (node != null) {
                try {
                    String typeName = node.asText();
                    Class<?> supportClazz = Class.forName(typeName);
                    if (classes.contains(supportClazz)) {
                        clazz = supportClazz;
                    }
                } catch (ClassNotFoundException e) {
                    // skip it
                }
            }
            Object val = mapper.readValue(root, clazz);
            return val;
        }

    }

    private class AckArgsDeserializer extends StdDeserializer<AckArgs> {

        protected AckArgsDeserializer() {
            super(AckArgs.class);
        }

        @Override
        public AckArgs deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            List args = new ArrayList();
            AckArgs result = new AckArgs(args);

            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            JsonNode root = mapper.readTree(jp);
            Class<?> clazz = currentAckClass.get();
            Iterator<JsonNode> iter = root.iterator();
            while (iter.hasNext()) {
                Object val;
                JsonNode arg = iter.next();
                if (arg.isTextual() || arg.isBoolean()) {
                    clazz = Object.class;
                }

                // TODO refactor it!
                if (arg.isNumber()) {
                    if (clazz.equals(Long.class)) {
                        val = arg.asLong();
                    }
                    if (clazz.equals(BigDecimal.class)) {
                        val = arg.getBigIntegerValue();
                    }
                    if (clazz.equals(Double.class)) {
                        val = arg.asDouble();
                    }
                    if (clazz.equals(Integer.class)) {
                        val = arg.asInt();
                    }
                    if (clazz.equals(Float.class)) {
                        val = (float)arg.getDoubleValue();
                    }
                }
                val = mapper.readValue(arg, clazz);
                args.add(val);
            }
            return result;
        }

    }

    private class EventDeserializer extends StdDeserializer<Event> {

        final Map<String, Class<?>> eventMapping = new ConcurrentHashMap<String, Class<?>>();

        protected EventDeserializer() {
            super(Event.class);
        }

        @Override
        public Event deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            ObjectNode root = (ObjectNode) mapper.readTree(jp);
            String eventName = root.get("name").asText();

            List eventArgs = new ArrayList();
            Event event = new Event(eventName, eventArgs);
            JsonNode args = root.get("args");
            if (args != null) {
                Iterator<JsonNode> iterator = args.getElements();
                if (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    Class<?> eventClass = eventMapping.get(eventName);
                    Object arg = mapper.readValue(node, eventClass);
                    eventArgs.add(arg);
                    while (iterator.hasNext()) {
                        node = iterator.next();
                        arg = mapper.readValue(node, Object.class);
                        eventArgs.add(arg);
                    }
                }
            }
            return event;
        }

    }

    private final ThreadLocal<Class<?>> currentAckClass = new ThreadLocal<Class<?>>();
    private final Configuration configuration;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventDeserializer eventDeserializer = new EventDeserializer();
    private final JsonObjectDeserializer jsonObjectDeserializer = new JsonObjectDeserializer();
    private final AckArgsDeserializer ackArgsDeserializer = new AckArgsDeserializer();

    public JacksonJsonSupport(Configuration configuration) {
        this.configuration = configuration;

        SimpleModule module = new SimpleModule("EventDeserializerModule", new Version(1, 0, 0, null));
        module.addDeserializer(Event.class, eventDeserializer);
        module.addDeserializer(JsonObject.class, jsonObjectDeserializer);
        module.addDeserializer(AckArgs.class, ackArgsDeserializer);
        objectMapper.registerModule(module);

        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

//        TODO If jsonObjectDeserializer will be not enough
//        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL);
//        typer.init(JsonTypeInfo.Id.CLASS, null);
//        typer.inclusion(JsonTypeInfo.As.PROPERTY);
//        typer.typeProperty(configuration.getJsonTypeFieldName());
//        objectMapper.setDefaultTyping(typer);
    }

    @Override
    public void addEventMapping(String eventName, Class<?> eventClass) {
        eventDeserializer.eventMapping.put(eventName, eventClass);
    }

    @Override
    public void addJsonClass(Class<?> clazz) {
        jsonObjectDeserializer.classes.add(clazz);
    }

    @Override
    public void removeEventMapping(String eventName) {
        eventDeserializer.eventMapping.remove(eventName);
    }

    @Override
    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }

    @Override
    public AckArgs readAckArgs(InputStream src, Class<?> argType) throws IOException {
        currentAckClass.set(argType);
        return objectMapper.readValue(src, AckArgs.class);
    }

    @Override
    public void writeValue(OutputStream out, Object value) throws IOException {
        objectMapper.writeValue(out, value);

    }

    @Override
    public String writeValueAsString(Object value) throws IOException {
        return objectMapper.writeValueAsString(value);
    }

}
