package com.senspark.game.utils;

public class SimpleKeyValue<K, V> {

	protected K key;
	protected V value;

	public SimpleKeyValue(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}
}
