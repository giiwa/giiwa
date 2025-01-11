/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.json;

import java.io.IOException;

import org.giiwa.dao.Bean;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BeanAdapter extends TypeAdapter<Bean> {

	public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {

		@Override
		@SuppressWarnings("unchecked")
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			return (Bean.class.isAssignableFrom(type.getRawType()) ? (TypeAdapter<T>) new BeanAdapter(gson) : null);
		}
		
	};

	private final Gson context;

	private BeanAdapter(Gson context) {
		this.context = context;
	}

	@Override
	public Bean read(JsonReader in) throws IOException {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void write(JsonWriter out, Bean value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}

		TypeAdapter<JSON> delegate = context.getAdapter(TypeToken.get(JSON.class));

		delegate.write(out, value.json());
	}

}
