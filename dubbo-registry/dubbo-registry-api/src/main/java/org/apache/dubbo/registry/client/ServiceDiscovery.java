/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_DELAY_NOTIFICATION_KEY;

/**
 * Defines the common operations of Service Discovery, extended and loaded by ServiceDiscoveryFactory
 */
public interface ServiceDiscovery extends RegistryService, Prioritized {

    /**
     * 注册
     *
     * @throws RuntimeException
     */
    void register() throws RuntimeException;

    /**
     * 更新
     *
     * @throws RuntimeException
     */
    void update() throws RuntimeException;

    /**
     * 解除注册
     *
     * @throws RuntimeException
     */
    void unregister() throws RuntimeException;

    /**
     * Gets all service names
     * <p>
     * 获取服务集合
     *
     * @return non-null read-only {@link Set}
     */
    Set<String> getServices();

    /**
     * 根据服务名获取服务实例集合
     *
     * @param serviceName
     * @return
     * @throws NullPointerException
     */
    List<ServiceInstance> getInstances(String serviceName) throws NullPointerException;

    /**
     * 添加服务实例变化监听器
     *
     * @param listener
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    default void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
    }

    /**
     * unsubscribe to instance change event.
     * 移除服务实例变化监听器
     *
     * @param listener
     * @throws IllegalArgumentException
     */
    default void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws IllegalArgumentException {
    }

    /**
     * 创建服务实例变化监听器
     * @param serviceNames
     * @return
     */
    default ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        return new ServiceInstancesChangedListener(serviceNames, this);
    }

    /**
     * 获取本地服务实例
     * @return
     */
    ServiceInstance getLocalInstance();

    /**
     * 获取本地元数据
     * @return
     */
    MetadataInfo getLocalMetadata();

    /**
     * 获取远端元数据
     * @param revision
     * @return
     */
    MetadataInfo getRemoteMetadata(String revision);

    /**
     * 获取远端元数据
     * @param revision
     * @param instances
     * @return
     */
    MetadataInfo getRemoteMetadata(String revision, List<ServiceInstance> instances);

    /**
     * Destroy the {@link ServiceDiscovery}
     *
     * 摧毁服务实例
     * @throws Exception If met with error
     */
    void destroy() throws Exception;

    /**
     * 是否摧毁
     * @return
     */
    boolean isDestroy();

    /**
     * 获取服务实例连接地址
     * @return
     */
    default URL getUrl() {
        return null;
    }

    /**
     * 获取延迟时间
     */
    default long getDelay() {
        return getUrl().getParameter(REGISTRY_DELAY_NOTIFICATION_KEY, 5000);
    }

    /**
     * A human-readable description of the implementation
     *
     * @return The description.
     */
    String toString();
}
