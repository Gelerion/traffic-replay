package com.gelerion.traffic.replay.core.beta;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.SessionCreatedResponse;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
/*
  Use consul distributed locking for dynamic configuration
 */
public class DynamicConfigurationSimulator {
    static AtomicInteger counter = new AtomicInteger();
    static int nServices = 10;
    static CountDownLatch latch = new CountDownLatch(nServices);

    static String groupId = "group-id-3"; //test-id
    static String keyPath = "traffic-replay/config/dynamic/" + groupId;


    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(nServices);
        for (int i = 0; i < nServices; i++) {
            executor.execute(new HttpLoaderInstance());
        }
    }

    static class HttpLoaderInstance implements Runnable {

        @Override
        public void run() {
            latch.countDown();

            Consul consul = Consul.builder()
                    .withUrl("http://consul.com")
                    .withBasicAuth("user", "password")
                    .build();

            String myName = "instance#_" + counter.getAndIncrement();
            Random rnd = new Random();

            //1. simulate delay
            int setupDelay = 500 + rnd.nextInt(3000);
            System.out.println("Starting the service " + myName + " with delay " + setupDelay);
//            sleepQuietly(setupDelay);

            //2. get cutoff time

            // 2.1 check if already present
            Optional<Value> maybeValue = consul.keyValueClient().getValue(keyPath);
            if (maybeValue.isPresent()) {
                System.out.println("2.1 Configuration is present. Value " + maybeValue.get().getValueAsString() + " service: " + myName);
                System.out.println("Done " + maybeValue.get().getValueAsString() + "[" + myName + "]");
                return;
            }

            // 2.2 create a session
            System.out.println("Value isn't present creating a session " + "[" + myName + "]");
            SessionCreatedResponse session = consul.sessionClient()
                    .createSession(ImmutableSession
                            .builder().name(groupId).ttl("1m").addChecks("serfHealth").behavior("delete").build());

            System.out.println("Session " + session.getId() + " created, acquiring a lock" + "[" + myName + "]");
            boolean isLockAcquired = consul.keyValueClient().acquireLock(groupId, session.getId());
            System.out.println("Is lock acquired? " + isLockAcquired + " [" + myName + "]");


            // 2.3 try lock and create a new path
            if (isLockAcquired) {
                System.out.println("Lock is successfully acquired " + "[" + myName + "]");
                int retry = 0;
                boolean isSuccessfulPut;
                do {
                    String msg = "{ :cutoff-time " + System.currentTimeMillis() + " }";
                    isSuccessfulPut = consul.keyValueClient().putValue(keyPath, msg);
                    System.out.println("Is successfulPut? " + isSuccessfulPut + " try #" + retry + " msg: " + msg + "[" + myName + "]");
                } while (!isSuccessfulPut);
            }


            // 3. read the value or wait until creation
            // watcher?
            System.out.println("Reading value service: "  + " [" + myName + "]");
            maybeValue = consul.keyValueClient().getValue(keyPath);
            int retry = 1;
            while (maybeValue.isEmpty()) {
                System.out.println("Reading value service: " + " try #" + retry  + " [" + myName + "]");
                maybeValue = consul.keyValueClient().getValue(keyPath);
                sleepQuietly(500);
            }

            consul.sessionClient().destroySession(session.getId());
            // 4. Done
            System.out.println("Done " + maybeValue.get().getValueAsString() + "[" + myName + "]");
        }
    }


    static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
