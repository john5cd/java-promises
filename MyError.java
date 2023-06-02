class MyError<V> implements ValueOrError<V> {

    private final Throwable throwable;

    public MyError(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public V value() {
        return null;
    }

    @Override
    public Throwable error() {
        return throwable;
    }

    static <T> ValueOrError<T> of(Throwable t) { return new MyError<>(t);}
}