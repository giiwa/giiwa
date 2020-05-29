package org.giiwa.dao;

import java.io.IOException;

import org.giiwa.json.JSON;

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
