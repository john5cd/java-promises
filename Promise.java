import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

class Promise<V> {
    public static enum Status {
        PENDING,
        FULFILLED,
        REJECTED
    }

    private Status promiseStatus;
    private ValueOrError<V> promiseValue;
    static volatile int numProm = -1;
    static volatile int numSet = -1;
    static volatile int numAny = 0;
    
    public Promise(PromiseExecutor<V> executor) {
            promiseStatus = Status.PENDING;
            Consumer<V> cons1 = (avalue) -> {
                myResolve(avalue);
            };
            Consumer<Throwable> cons2 = (avalue) -> {
                myReject(avalue);
            };
    
            executor.execute(cons1, cons2);
        }
        
    private synchronized void myResolve(V avalue) {
            
            if (promiseStatus == Status.PENDING){
                promiseStatus = Status.FULFILLED;
                this.promiseValue = new MyValue<V>(avalue);
                this.notify();
            };        
    }

    private synchronized void myReject(Throwable avalue) {
        if (promiseStatus == Status.PENDING){
            promiseStatus = Status.REJECTED;
            this.promiseValue = new MyError<V>(avalue);
            this.notify();
            };
    }

    public <T> Promise<ValueOrError<T>> then(Function<V, T> onResolve, Consumer<Throwable> onReject) {
        Thread then2 = new Thread(() -> {
            synchronized (this) {
                try {this.wait();} catch (InterruptedException e) {e.printStackTrace();}
                if (promiseStatus==Status.FULFILLED) {
                    if (onResolve!=null && onReject==null) {
                        T vv = onResolve.apply(promiseValue.value());
                        this.promiseValue = new MyValue<V>((V) vv);
                    } else {
                        onReject.accept(null);
                    }
                        this.notify();
                }else if (promiseStatus==Status.REJECTED) {
                    if (onReject!=null) {
                        onReject.accept(promiseValue.error());
                    } else {
                        promiseValue = new MyError<>(promiseValue.error());
                        onResolve.apply((V) promiseValue.error());
                    }
                    this.notify();
                }
            }
        });
        then2.start();
        return (Promise<ValueOrError<T>>) this;
    }

    public <T> Promise<T> then(Function<V, T> onResolve) {
        
        then(onResolve, null);
        return (Promise<T>) this;
    }

    public Promise<Throwable> catchError(Consumer<Throwable> onReject) {
        then(null, onReject);
        return (Promise<Throwable>) this;
    }

    public <T> Promise<T> andFinally(Consumer<ValueOrError<T>> onSettle) {
        Thread then2 = new Thread(() -> {
            synchronized (this) {
                try {this.wait();} catch (InterruptedException e) {e.printStackTrace();}
                if (promiseStatus==Status.FULFILLED) {
                    onSettle.accept((ValueOrError<T>) promiseValue);
                }else if (promiseStatus==Status.REJECTED) {
                    onSettle.accept((ValueOrError<T>) promiseValue);
                }
                this.notify();
            }
        });
        then2.start();
        return (Promise<T>) this;
    }

    public static <T> Promise<T> all(List<Promise<?>> promises) {
        
        return new Promise<T>((res, rej) -> {

                ArrayList containerAll = new ArrayList<>();
                ArrayList returnlist = new ArrayList<>();
                ArrayList index = new ArrayList<>();
                for (Promise<?> prom : promises) {
                    prom.then((value) -> {
                        if (value.getClass().getName()=="java.lang.Throwable") {
                            rej.accept((Throwable) value);
                        }
                        containerAll.add(value);
                        index.add(promises.indexOf(prom));
                        if (containerAll.size()==((ArrayList) promises).size()) {
                            while (returnlist.size()<((ArrayList) promises).size()) {
                                numProm++;
                                for (Object iterable : index) {
                                    if ((int) iterable==numProm) {
                                        returnlist.add(containerAll. get(index.indexOf(iterable)));
                                    }
                                }
                            }
                        }
                        if (returnlist.size()==((ArrayList) promises).size()) {
                            numProm = -1;
                            res.accept((T) returnlist);
                        }
                        return value;
                    });
                }
        });
    }

    public static <T> Promise<T> race(List<Promise<?>> promises) {

        return new Promise<T>((res, rej) -> {
                promises.forEach(prom -> {
                        prom.then(value -> {
                            res.accept((T) value);
                            return ((T) value); 
                        });
                    });
                });
    }

    public static <T> Promise<T> resolve(T value) {

        return new Promise<T>((res, rej) -> {
            Thread st_resolve = new Thread(() -> {
                try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                res.accept(value);
            });
            st_resolve.start();
        });
    }

    public static Promise<Throwable> reject(Throwable error) {
        return new Promise<Throwable>((res, rej) -> {
            Thread st_reject = new Thread(() -> {
                try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                rej.accept(error);
            });
            st_reject.start();
        });
    }

    public static <T> Promise<T> allSettled(List<Promise<?>> promises) {

        return new Promise<T>((res, rej) -> {

            ArrayList containerAll = new ArrayList<>();
            ArrayList returnlist = new ArrayList<>();
            ArrayList index = new ArrayList<>();
            for (Promise<?> prom : promises) {
                prom.then((value) -> {
                    containerAll.add(value);
                    index.add(promises.indexOf(prom));
                    if (containerAll.size()==((ArrayList) promises).size()) {
                        while (returnlist.size()<((ArrayList) promises).size()) {
                            numSet++;
                            for (Object iterable : index) {
                                if ((int) iterable==numSet) {
                                    returnlist.add(containerAll. get(index.indexOf(iterable)));
                                }
                            }
                        }
                    }
                    if (returnlist.size()==((ArrayList) promises).size()) {
                        numSet = -1;
                        res.accept((T) returnlist);
                    }
                    return value;
                });
            }
    });
    }

    public static <T> Promise<T> any(Iterable<Promise<?>> promises) {

        return new Promise<T>((res, rej) -> {
            promises.forEach(prom -> {
                    prom.then(value -> {
                        if (value!=null) {
                            res.accept((T) value);
                        }else {
                            numAny++;
                            if (numAny==((ArrayList) promises).size()) {
                                rej.accept(new Throwable("All failed"));
                                numAny=0;
                            }
                        }
                        return ((T) value); 
                    });
                });
            });
    }
}