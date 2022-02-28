package io.github.middolsundear.stork.servicediscovery.nacos;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="https://github.com/y-bowen">bowen yang</a>
 * @version 1.0
 */
public class NacosServiceDiscovery extends CachingServiceDiscovery {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String serviceName;
    private final boolean secure;
    private final String serveAddr;
    private final String userName;
    private final String password;
    private final String groupName;
    private final String namespaceId;
    private final Boolean healthyOnly;
    private final WebClient webClient;
    private final URI server;
    public NacosServiceDiscovery(NacosServiceDiscoveryProviderConfiguration config, String serviceName, boolean secure, Vertx vertx) {
        super(config.getRefreshPeriod());
        this.serveAddr = config.getNacosServeAddr();
        this.serviceName = serviceName;
        this.secure = secure;
        this.userName = config.getNacosUserName();
        this.password = config.getNacosPassword();
        this.groupName = config.getNacosGroupName();
        this.namespaceId = config.getNacosNamespaceId();
        this.healthyOnly = Boolean.valueOf(config.getHealthyOnly());
        this.webClient = WebClient.create(vertx);
        this.server = URI.create(this.serveAddr);

    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> list) {

        AtomicReference<String> accessToken = new AtomicReference<>("");
        webClient.post(server.getPort(), server.getHost(), "/nacos/v1/auth/login")
                .addQueryParam("username", this.userName)
                .addQueryParam("password", this.password).send()
                .onSuccess(bufferHttpResponse ->
                        accessToken.set(bufferHttpResponse.bodyAsJsonObject().getString("accessToken"))
                )
                .onFailure(throwable ->
                        logger.error(throwable.getStackTrace().toString())
                );

        return Uni.createFrom().emitter(uniEmitter -> {
            this.webClient.get(server.getPort(),server.getHost(),"/nacos/v1/ns/instance/list")
                    .addQueryParam("serviceName",this.serviceName)
                    .addQueryParam("groupName",this.groupName)
                    .addQueryParam("namespaceId",this.namespaceId)
                    .addQueryParam("healthyOnly",this.healthyOnly.toString())
                    .addQueryParam("accessToken",accessToken.get())
                    .send().onSuccess(bufferHttpResponse -> {
                        List<ServiceInstance> serviceInstances = new ArrayList<>();
                        try {
                            for (Object host : bufferHttpResponse.bodyAsJsonObject().getJsonArray("hosts")) {
                                JsonObject instance = (JsonObject) host;
                                MessageDigest md5 = null;
                                md5 = MessageDigest.getInstance("MD5");
                                md5.update(instance.getString("instanceId").getBytes(StandardCharsets.UTF_8));
                                BigInteger instanceId = new BigInteger(1, md5.digest());
                                serviceInstances.add(new DefaultServiceInstance(instanceId.longValue(),
                                        instance.getString("ip"),
                                        instance.getInteger("port"),
                                        secure));
                            }
                            uniEmitter.complete(serviceInstances);
                        }catch (NoSuchAlgorithmException e){
                            uniEmitter.fail(e);
                        }
                    }).onFailure(throwable -> {
                        uniEmitter.fail(throwable);
                    });
        });
    }
}
