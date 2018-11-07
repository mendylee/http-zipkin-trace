package common.http.zipkin.trace;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.util.StringUtils;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;

public class GoogleHttpRequestTextMap implements SpanTextMap {

	private final HttpRequest delegate;

	GoogleHttpRequestTextMap(HttpRequest delegate) {
		this.delegate = delegate;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		final Iterator<String> keyIter = this.delegate.getHeaders().keySet().iterator();
		final HttpHeaders httpHeaders = this.delegate.getHeaders();
		return new Iterator<Map.Entry<String, String>>() {
			@Override
			public boolean hasNext() {
				return keyIter.hasNext();
			}

			@Override
			public Map.Entry<String, String> next() {
				String key = keyIter.next();
				List<String> value = httpHeaders.getHeaderStringValues(key);
				return new AbstractMap.SimpleEntry<>(key, value.isEmpty() ? "" : value.get(0));
			}
		};
	}

	@Override
	public void put(String key, String value) {
		if (!StringUtils.hasText(value)) {
			return;
		}
		this.delegate.getHeaders().put(key, Collections.singletonList(value));
	}

}
