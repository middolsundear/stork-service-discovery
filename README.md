# stork-service-discovery
SmallRye Stork's discovery extension.

![](https://img.shields.io/badge/maven--central-1.0.0-green)

## Quick start
- 1、Introduce maven coordinates
```
   <dependency>
     <groupId>io.github.middolsundear.stork</groupId>
     <artifactId>stork-service-discovery-nacos</artifactId>
     <version>1.0.0</version>
   </dependency>
```
- 2、Add the following configuration in the application.properties file
```
#nacos
stork.my-service.service-discovery=nacos
stork.my-service.service-discovery.nacos-serve-addr=http://127.0.0.1:8848
stork.my-service.service-discovery.nacos-user-name=nacos # default value "nacos"
stork.my-service.service-discovery.nacos-password=nacos # default value "nacos"
stork.my-service.service-discovery.nacos-group-name=DEFAULT_GROUP # default value "DEFAULT_GROUP"
stork.my-service.service-discovery.nacos-namespace-id=public # default value "public"
stork.my-service.load-balancer=round-robin
```

## Quarkus Stork Use
- 1、Create the src/main/java/org/acme/services/BlueService.java with the following content:
```
package org.acme.services;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class BlueService {

    @ConfigProperty(name = "blue-service-port", defaultValue = "9000") int port;

    /**
     * Start an HTTP server for the blue service.
     *
     * Note: this method is called on a worker thread, and so it is allowed to block.
     */
    public void init(@Observes StartupEvent ev, Vertx vertx) {
        vertx.createHttpServer()
                .requestHandler(req -> req.response().endAndForget("Hello from Blue!"))
                .listenAndAwait(port);
    }
}
```
- 2、Create the src/main/java/org/acme/services/RedService.java with the following content:
```
package org.acme.services;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class RedService {
    @ConfigProperty(name = "red-service-port", defaultValue = "9001") int port;

    /**
     * Start an HTTP server for the red service.
     *
     * Note: this method is called on a worker thread, and so it is allowed to block.
     */
    public void init(@Observes StartupEvent ev, Vertx vertx) {
        vertx.createHttpServer()
                .requestHandler(req -> req.response().endAndForget("Hello from Red!"))
                .listenAndAwait(port);
    }

}

```
-3、Create the src/main/java/org/acme/services/Registration.java file with the following content:
```
package org.acme.services;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;


@ApplicationScoped
public class Registration {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @ConfigProperty(name = "nacos.serve.addr") String serveAddr;

    @ConfigProperty(name = "blue-service-port", defaultValue = "9000") Integer red;
    @ConfigProperty(name = "red-service-port", defaultValue = "9001") Integer blue;

    /**
     * Register our two services in Consul.
     *
     * Note: this method is called on a worker thread, and so it is allowed to block.
     */
    public void init(@Observes StartupEvent ev, Vertx vertx) throws ClassNotFoundException {
        final URI uri = URI.create(serveAddr);
        final WebClient client = WebClient.create(vertx.getDelegate());
        AtomicReference<String> accessToken = new AtomicReference<>("");
        client.post(uri.getPort(), uri.getHost(), "/nacos/v1/auth/login")
                .addQueryParam("username", "nacos")
                .addQueryParam("password", "nacos").send()
                .onSuccess(bufferHttpResponse ->{
                    accessToken.set(bufferHttpResponse.bodyAsJsonObject().getString("accessToken"));
                })
                .onFailure(throwable ->
                        logger.error(throwable.getStackTrace().toString())
                );
        client.post(uri.getPort(), uri.getHost(), "/nacos/v1/ns/instance")
                .addQueryParam("ip","127.0.0.1")
                .addQueryParam("port", red.toString())
                .addQueryParam("serviceName","my-service")
                .addQueryParam("accessToken",accessToken.get())
                .send()
                .onSuccess(bufferHttpResponse -> {

                }).onFailure(throwable -> {

        });
        client.post(uri.getPort(), uri.getHost(), "/nacos/v1/ns/instance")
                .addQueryParam("ip","127.0.0.1")
                .addQueryParam("port", blue.toString())
                .addQueryParam("serviceName","my-service")
                .addQueryParam("accessToken",accessToken.get())
                .send()
                .onSuccess(bufferHttpResponse -> {

                }).onFailure(throwable -> {

        });
    }
}
```
-4、Create the src/main/java/org/acme/MyService.java file with the following content:
```
package org.acme;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The REST Client interface.
 *
 * Notice the `baseUri`. It uses `stork://` as URL scheme indicating that the called service uses Stork to locate and
 * select the service instance. The `my-service` part is the service name. This is used to configure Stork discovery
 * and selection in the `application.properties` file.
 */
@RegisterRestClient(baseUri = "stork://my-service")
public interface MyService {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String get();
}
```
-5、Create the src/main/java/org/acme/FrontendApi.java file with the following content:
```
package org.acme;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A frontend API using our REST Client (which uses Stork to locate and select the service instance on each call).
 */
@Path("/api")
public class FrontendApi {

    @RestClient MyService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String invoke() {
        return service.get();
    }

}
```
## Other resources
- nacos: https://nacos.io
- quarkus: https://github.com/quarkusio/quarkus
- SmallRye Stork: https://github.com/smallrye/smallrye-stork
