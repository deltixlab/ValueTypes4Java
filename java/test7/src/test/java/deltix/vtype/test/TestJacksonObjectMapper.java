/*
 * Copyright 2017-2018 Deltix, Inc.
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

package deltix.vtype.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import deltix.dfp.Decimal64;
import deltix.dfp.Decimal64Utils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Workaround for using Value Types with a typical serialization framework.
 */
public class TestJacksonObjectMapper {
    @Test
    public void testObjectMapper() throws Exception {
        Decimal64Container init = new Decimal64Container();
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(init);

        Decimal64Container res = objectMapper.readValue(json, Decimal64Container.class);
        assertEquals(init, res);
    }


    static class Decimal64Container {
        private Decimal64 _DecimalValue;

        public Decimal64Container() {
            _DecimalValue = Decimal64.ZERO;
        }

        /**
         * @return Dto field.
         */
        @JsonSerialize(using = DecimalSerializer.class)
        @JsonProperty("decimal_value")

        // Only works with VT Agent
//        public Decimal64 getDecimalValue() {
//            return _DecimalValue;
//        }

        // Works in any case
        public long getDecimalAsLong() {
            return Decimal64.toUnderlying(_DecimalValue);
        }

        /**
         * @param value Value to be set.
         */
        @JsonDeserialize(using = DecimalDeserializer.class)
        @JsonProperty("decimal_value")

        // Only works with VT agent running
//        public void setDecimalValue(Decimal64 value) {
//            _DecimalValue = value;
//        }
        // Works in any case
        public void setDecimalAsLong(long value) {
            _DecimalValue = Decimal64.fromUnderlying(value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Decimal64Container && Decimal64.equals(_DecimalValue, ((Decimal64Container) obj)._DecimalValue);
        }
    }

    static class DecimalSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(Decimal64.fromUnderlying(value).toString());
        }
    }

    static class DecimalDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final String s = p.getText();
            if (s == null) {
                return Decimal64Utils.NULL;
            }
            return Decimal64.toUnderlying(Decimal64.parse(s));
        }
    }
}

/**
 * This implementation is not going to work due to type mismatch
 */

/*
    static class DecimalSerializer extends JsonSerializer<Decimal64> {
        @Override
        public void serialize(Decimal64 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }


    static class DecimalDeserializer extends JsonDeserializer<Decimal64> {
        @Override
        public Decimal64 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final String s = p.getText();
            if (s == null) {
                return null;
            }
            return Decimal64.parse(s);
        }
    }
*/
