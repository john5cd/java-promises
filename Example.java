public class Example {

    public static void main(String[] args) {
        System.out.println("start");

        Promise<Object> promise1 = new Promise<>((res, rej) -> {
            Thread promiseThread = new Thread(()->{
                try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
                res.accept("i am asynchronous");
                rej.accept(new Throwable("no"));
            });
            promiseThread.start();
        }).then(data -> {
            System.out.println(((String) data).toUpperCase());
            return data;
        }).then(data -> {
            System.out.println("sentence contains: "+ ((String) data).length()+ " letters");
            return null;
        });


        Promise<Object> promise2 = new Promise<>((res, rej) -> {
            Thread promiseThread2 = new Thread(()->{
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
                res.accept("async 1 second");
                rej.accept(new Throwable("no"));
            });
            promiseThread2.start();
        }).then(data -> {
            System.out.println((String) data);
            return null;
        });


        System.out.println("end");
        
    }
}

