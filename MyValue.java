class MyValue<V> implements ValueOrError<V> {

    private final V value;

    public MyValue(V value) {
        this.value = value;
    }

    @Override
    public V value() {
        return value;
    }

    @Override
    public Throwable error() {
        return null;
    }

    static <T> ValueOrError<T> of(T t) { return new MyValue<>(t);}
}