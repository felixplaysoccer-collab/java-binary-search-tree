package edu.uwm.cs351.util;
import java.util.Map;

/**
 * An entry in a Map.  A default implementation.
 * @see {@link java.util.Map.Entry}
 */
public class DefaultEntry<K,V> implements Map.Entry<K,V> {

	protected K key;
	protected V value;
	
	public DefaultEntry(K k, V v) { key = k; value = v; }
	
	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V v) {
		V old = value;
		value = v;
		return old;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry<?,?> e = (Map.Entry<?,?>)o;
		return eq(key, e.getKey()) && eq(value, e.getValue());
	}

	public int hashCode() {
		return ((key   == null)   ? 0 :   key.hashCode()) ^
			   ((value == null)   ? 0 : value.hashCode());
	}

	public String toString() {
		return key + "=" + value;
	}

	private static boolean eq(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}
}