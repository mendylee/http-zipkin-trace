package common.http.zipkin.trace;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.util.StringUtils;

import com.ning.http.client.Request;

public class NingHttpRequestTextMap implements SpanTextMap {

	private final Request delegate;

	NingHttpRequestTextMap(Request delegate) {
		this.delegate = delegate;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		final Iterator<Map.Entry<String, List<String>>> iterator = this.delegate.getHeaders().entrySet().iterator();
		return new Iterator<Map.Entry<String, String>>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Map.Entry<String, String> next() {
				Map.Entry<String, List<String>> next = iterator.next();
				List<String> value = next.getValue();
				return new AbstractMap.SimpleEntry<>(next.getKey(), value.isEmpty() ? "" : value.get(0));
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
