package io.github.middolsundear.stork.servicediscovery.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.spi.StorkInfrastructure;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/y-bowen">bowen yang</a>
 * @version 1.0
 * @date 2022年02月16日 11:13 上午
 */
public class NacosServiceDiscovery extends CachingServiceDiscovery {
    private final String serviceName;
    private final boolean secure;
    private final String serveAddr;
    private final String userName;
    private final String password;
    private final String groupName;
    private final String namespaceId;
    private final boolean healthyOnly;
    public NacosServiceDiscovery(NacosServiceDiscoveryProviderConfiguration config, String serviceName, boolean secure) {
        super(config.getRefreshPeriod());
        this.serveAddr = config.getNacosServeAddr();
        this.serviceName = serviceName;
        this.secure = secure;
        this.userName = config.getNacosUserName();
        this.password = config.getNacosPassword();
        this.groupName = config.getNacosGroupName();
        this.namespaceId = config.getNacosNamespaceId();
        this.healthyOnly = Boolean.valueOf(config.getHealthyOnly());
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> list) {
        Uni<List<Instance>> emitter = Uni.createFrom().emitter(uniEmitter -> {
            try {
                Properties properties = new Properties();
                properties.setProperty(PropertyKeyConst.SERVER_ADDR,serveAddr);
                properties.setProperty(PropertyKeyConst.USERNAME,userName);
                properties.setProperty(PropertyKeyConst.PASSWORD,password);
                properties.setProperty(PropertyKeyConst.NAMESPACE,namespaceId);
                NamingService naming = NamingFactory.createNamingService(properties);
                List<Instance> instances = naming.selectInstances(serviceName, groupName, healthyOnly);
                uniEmitter.complete(instances);
            } catch (NacosException e) {
                uniEmitter.fail(e);
            }
        });
        return emitter.map(instances -> instances.stream().map(instance -> {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(instance.getInstanceId().getBytes(StandardCharsets.UTF_8));
                BigInteger instanceId = new BigInteger(1, md5.digest());
                return new DefaultServiceInstance(instanceId.longValue(),instance.getIp(),instance.getPort(),secure);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(instance -> null!=instance).collect(Collectors.toList()));
    }
}
