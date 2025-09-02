package Graph;

public class Pair<K, V> {
    private final K key;
    private final V value;
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    public K getKey() {
        return key;
    }
    public V getValue() {
        return value;
    }
    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<K, V> pair = (Pair<K, V>) o;
        return this.key.equals(pair.getKey()) && this.value.equals(pair.getValue());
    }

    @Override
    public Pair<K, V> clone() throws CloneNotSupportedException {
        return new Pair<>(this.key, this.value);
    }
}
