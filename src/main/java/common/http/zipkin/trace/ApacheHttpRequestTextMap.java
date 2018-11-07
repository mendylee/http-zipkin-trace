package common.http.zipkin.trace;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.util.StringUtils;

public class ApacheHttpRequestTextMap implements SpanTextMap {
	private final HttpRequestBase delegate;

	public ApacheHttpRequestTextMap(HttpRequestBase delegate) {
		this.delegate = delegate;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		final HeaderIterator iterator = this.delegate.headerIterator();
		return new Iterator<Map.Entry<String, String>>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Map.Entry<String, String> next() {
				Header next = (Header) iterator.next();
				String value = next.getValue();
				return new AbstractMap.SimpleEntry<>(next.getName(), value.isEmpty() ? "" : value);
			}
		};
	}

	@Override
	public void put(String key, String value) {
		if (!StringUtils.hasText(value)) {
			return;
		}
		this.delegate.addHeader(key, value);
	}
}
