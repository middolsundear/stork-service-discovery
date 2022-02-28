package io.github.middolsundear.stork.servicediscovery.nacos;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;

/**
 * @author <a href="https://github.com/y-bowen">bowen yang</a>
 * @version 1.0
 */
@ServiceDiscoveryType("nacos")
@ServiceDiscoveryAttribute(name = "nacos-serve-addr", description = "The Nacos server host.", required = true, defaultValue = "http://127.0.0.1:8848")
@ServiceDiscoveryAttribute(name = "nacos-user-name", description = "The Nacos user name.", required = true, defaultValue = "nacos")
@ServiceDiscoveryAttribute(name = "nacos-password", description = "The Nacos password.", required = true, defaultValue = "nacos")
@ServiceDiscoveryAttribute(name = "nacos-group-name", description = "The Nacos group name.", defaultValue = "DEFAULT_GROUP")
@ServiceDiscoveryAttribute(name = "nacos-namespace-id", description = "The Nacos namespaceId.", defaultValue = "public")
@ServiceDiscoveryAttribute(name = "healthy-only", description = "The Nacos service healthy status;", defaultValue = "true")
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", defaultValue = "10s")
public class NacosServiceDiscoveryProvider implements ServiceDiscoveryProvider<NacosServiceDiscoveryProviderConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(NacosServiceDiscoveryProviderConfiguration config, String serviceName,
                                                   ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new NacosServiceDiscovery(config, serviceName, serviceConfig.secure(),storkInfrastructure.get(Vertx.class, Vertx::vertx));

    }
}
