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

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

/**
 * The model class of an instance of a service, which is used for service registration and discovery.
 * <p>
 * <p>
 * 服务实例
 *
 * @since 2.7.5
 */
public interface ServiceInstance extends Serializable {

    /**
     * The name of service that current instance belongs to.
     *
     * 服务实例名称
     * @return non-null
     */
    String getServiceName();

    /**
     * The hostname of the registered service instance.
     *
     * 获取HOST
     * @return non-null
     */
    String getHost();

    /**
     * The port of the registered service instance.
     * 获取端口
     *
     * @return the positive integer if present
     */
    int getPort();

    /**
     * 获取地址
     *
     * @return
     */
    String getAddress();

    /**
     * The enabled status of the registered service instance.
     * <p>
     * 是否启用
     *
     * @return if <code>true</code>, indicates current instance is enabled, or disable, the client should remove this one.
     * The default value is <code>true</code>
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * The registered service instance is health or not.
     * 是否健康
     *
     * @return if <code>true</code>, indicates current instance is healthy, or unhealthy, the client may ignore this one.
     * The default value is <code>true</code>
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * The key / value pair metadata associated with the service instance.
     * <p>
     * 获取元信息
     *
     * @return non-null, mutable and unsorted {@link Map}
     */
    Map<String, String> getMetadata();

    /**
     * 获取排序后的元信息
     *
     * @return
     */
    SortedMap<String, String> getSortedMetadata();

    /**
     * 获取注册集群
     *
     * @return
     */
    String getRegistryCluster();

    /**
     * 设置注册集群
     *
     * @param registryCluster
     */
    void setRegistryCluster(String registryCluster);

    /**
     * 获取所有拓展参数
     *
     * @return
     */
    Map<String, String> getExtendParams();

    /**
     * 获取拓展参数
     *
     * @param key
     * @return
     */
    String getExtendParam(String key);

    /**
     * 设置拓展参数
     *
     * @param key
     * @param value
     * @return
     */
    String putExtendParam(String key, String value);

    /**
     * 设置拓展参数
     *
     * @param key
     * @param value
     * @return
     */
    String putExtendParamIfAbsent(String key, String value);

    /**
     * 移除拓展参数
     *
     * @param key
     * @return
     */
    String removeExtendParam(String key);

    /**
     * 获取所有参数
     *
     * @return
     */
    Map<String, String> getAllParams();

    /**
     * 获取应用模型
     *
     * @return
     */
    ApplicationModel getApplicationModel();

    /**
     * 设置应用模型
     *
     * @param applicationModel
     */
    void setApplicationModel(ApplicationModel applicationModel);

    /**
     * 获取应用模型
     *
     * @return
     */
    @Transient
    default ApplicationModel getOrDefaultApplicationModel() {
        return ScopeModelUtil.getApplicationModel(getApplicationModel());
    }

    /**
     * Get the value of metadata by the specified name
     *
     * 获取元信息
     * @param name the specified name
     * @return the value of metadata if found, or <code>null</code>
     * @since 2.7.8
     */
    default String getMetadata(String name) {
        return getMetadata(name, null);
    }

    /**
     * Get the value of metadata by the specified name
     * 获取元信息
     *
     * @param name the specified name
     * @return the value of metadata if found, or <code>defaultValue</code>
     * @since 2.7.8
     */
    default String getMetadata(String name, String defaultValue) {
        return getMetadata().getOrDefault(name, defaultValue);
    }

    /**
     * 获取服务元信息
     *
     * @return
     */
    MetadataInfo getServiceMetadata();

    /**
     * 设置服务元信息
     *
     * @param serviceMetadata
     */
    void setServiceMetadata(MetadataInfo serviceMetadata);

    /**
     * 将协议转换为地址信息
     * @param protocol
     * @return
     */
    InstanceAddressURL toURL(String protocol);

}
