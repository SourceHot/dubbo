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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.MetaCacheManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.isValidInstance;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;

/**
 * Each service discovery is bond to one application.
 */
public abstract class AbstractServiceDiscovery implements ServiceDiscovery {
    private final Logger logger = LoggerFactory.getLogger(AbstractServiceDiscovery.class);
    /**
     * 是否摧毁
     */
    private volatile boolean isDestroy;

    /**
     * 服务实例名称
     */
    protected final String serviceName;
    /**
     * 服务实例
     */
    protected volatile ServiceInstance serviceInstance;
    /**
     * 元信息
     */
    protected volatile MetadataInfo metadataInfo;
    /**
     * 元信息报告
     */
    protected MetadataReport metadataReport;
    /**
     * 元信息类型
     */
    protected String metadataType;
    /**
     * 元信息缓存管理器
     */
    protected final MetaCacheManager metaCacheManager;
    /**
     * 需要注册的URL
     */
    protected URL registryURL;

    /**
     * 服务实例变化监听器
     */
    protected Set<ServiceInstancesChangedListener> instanceListeners = new ConcurrentHashSet<>();

    /**
     * 应用模型
     */
    protected ApplicationModel applicationModel;

    public AbstractServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        this(applicationModel.getApplicationName(), registryURL);
        this.applicationModel = applicationModel;
        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        metadataType = metadataReportInstance.getMetadataType();
        this.metadataReport = metadataReportInstance.getMetadataReport(registryURL.getParameter(REGISTRY_CLUSTER_KEY));
//        if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataReportInstance.getMetadataType())) {
//            this.metadataReport = metadataReportInstance.getMetadataReport(registryURL.getParameter(REGISTRY_CLUSTER_KEY));
//        } else {
//            this.metadataReport = metadataReportInstance.getNopMetadataReport();
//        }
    }

    public AbstractServiceDiscovery(String serviceName, URL registryURL) {
        this.applicationModel = ApplicationModel.defaultModel();
        this.registryURL = registryURL;
        this.serviceName = serviceName;
        this.metadataInfo = new MetadataInfo(serviceName);
        this.metaCacheManager = new MetaCacheManager(getCacheNameSuffix(),
            applicationModel.getFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getCacheRefreshingScheduledExecutor());
    }

    public synchronized void register() throws RuntimeException {
        // 构造服务实例
        this.serviceInstance = createServiceInstance(this.metadataInfo);
        // 验证服务实例
        if (!isValidInstance(this.serviceInstance)) {
            logger.warn("No valid instance found, stop registering instance address to registry.");
            return;
        }

        // 是否需要修订
        boolean revisionUpdated = calOrUpdateInstanceRevision(this.serviceInstance);
        // 如果需要修订
        if (revisionUpdated) {
            // 汇报元信息
            reportMetadata(this.metadataInfo);
            // 执行注册（子类实现）
            doRegister(this.serviceInstance);
        }
    }

    /**
     * Update assumes that DefaultServiceInstance and its attributes will never get updated once created.
     * Checking hasExportedServices() before registration guarantees that at least one service is ready for creating the
     * instance.
     */
    @Override
    public synchronized void update() throws RuntimeException {
        // 已经摧毁的情况下不做操作
        if (isDestroy) {
            return;
        }

        // 服务实例为空的情况下创建服务实例
        if (this.serviceInstance == null) {
            this.serviceInstance = createServiceInstance(this.metadataInfo);
        }
        // 如果服务实例验证不通过进行定制化操作
        else if (!isValidInstance(this.serviceInstance)) {
            ServiceInstanceMetadataUtils.customizeInstance(this.serviceInstance, this.applicationModel);
        }

        // 如果服务实例验证不通过不做处理
        if (!isValidInstance(this.serviceInstance)) {
            return;
        }

        // 确定是否需要修订服务实例
        boolean revisionUpdated = calOrUpdateInstanceRevision(this.serviceInstance);
        // 需要则更新
        if (revisionUpdated) {
            logger.info(String.format("Metadata of instance changed, updating instance with revision %s.", this.serviceInstance.getServiceMetadata().getRevision()));
            doUpdate(this.serviceInstance);
        }
    }

    @Override
    public synchronized void unregister() throws RuntimeException {
        // fixme, this metadata info might still being shared by other instances
//        unReportMetadata(this.metadataInfo);
        if (!isValidInstance(this.serviceInstance)) {
            return;
        }
        doUnregister(this.serviceInstance);
    }

    @Override
    public final ServiceInstance getLocalInstance() {
        return this.serviceInstance;
    }

    @Override
    public MetadataInfo getLocalMetadata() {
        return this.metadataInfo;
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision, List<ServiceInstance> instances) {
        // 从元信息缓存管理器中获取
        MetadataInfo metadata = metaCacheManager.get(revision);

        // 如果元数据不为空并且元信息不是空的
        if (metadata != null && metadata != MetadataInfo.EMPTY) {
            // 元数据初始化
            metadata.init();
            // metadata loaded from cache
            if (logger.isDebugEnabled()) {
                logger.debug("MetadataInfo for revision=" + revision + ", " + metadata);
            }
            // 返回元数据
            return metadata;
        }

        synchronized (metaCacheManager) {
            // try to load metadata from remote.
            // 重试次数
            int triedTimes = 0;
            // 重试次数小于3
            while (triedTimes < 3) {
                // 获取远端元数据
                metadata = MetadataUtils.getRemoteMetadata(revision, instances, metadataReport);

                // 如果不是空的元数据进行初始化并结束循环
                if (metadata != MetadataInfo.EMPTY) {// succeeded
                    metadata.init();
                    break;
                } else {// failed
                    // 如果重试次数大于0，重试次数累加1并暂停1秒进入下一次处理
                    if (triedTimes > 0) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Retry the " + triedTimes + " times to get metadata for revision=" + revision);
                        }
                    }
                    triedTimes++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }

            // 如果元信息为空记录异常日志，反之则将其放入到元信息缓存管理器中
            if (metadata == MetadataInfo.EMPTY) {
                logger.error("Failed to get metadata for revision after 3 retries, revision=" + revision);
            } else {
                metaCacheManager.put(revision, metadata);
            }
        }
        return metadata;
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision) {
       return metaCacheManager.get(revision);
    }

    @Override
    public final void destroy() throws Exception {
        isDestroy = true;
        metaCacheManager.destroy();
        doDestroy();
    }

    @Override
    public final boolean isDestroy() {
        return isDestroy;
    }

    @Override
    public void register(URL url) {
        metadataInfo.addService(url);
    }

    @Override
    public void unregister(URL url) {
        metadataInfo.removeService(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        metadataInfo.addSubscribedURL(url);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        metadataInfo.removeSubscribedURL(url);
    }

    @Override
    public List<URL> lookup(URL url) {
       throw new UnsupportedOperationException("Service discovery implementation does not support lookup of url list.");
    }

    protected void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {
        // 取消注册
        this.unregister();

        // 如果dubbo.metadata.revision属性不是0
        if (!EMPTY_REVISION.equals(getExportedServicesRevision(serviceInstance))) {
            // 执行汇报元数据操作
            reportMetadata(serviceInstance.getServiceMetadata());
            // 执行注册操作
            this.doRegister(serviceInstance);
        }
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    protected abstract void doRegister(ServiceInstance serviceInstance) throws RuntimeException;

    protected abstract void doUnregister(ServiceInstance serviceInstance);

    protected abstract void doDestroy() throws Exception;

    /**
     * 构造服务实例
     *
     * @param metadataInfo
     * @return
     */
    protected ServiceInstance createServiceInstance(MetadataInfo metadataInfo) {
        // 构造函数创建
        DefaultServiceInstance instance = new DefaultServiceInstance(serviceName, applicationModel);
        // 设置元信息
        instance.setServiceMetadata(metadataInfo);
        // 设置元信息存储类型
        setMetadataStorageType(instance, metadataType);
        // 定制化服务实例
        ServiceInstanceMetadataUtils.customizeInstance(instance, applicationModel);
        return instance;
    }

    /**
     * 确定是否需要修订服务实例
     *
     * @param instance
     * @return
     */
    protected boolean calOrUpdateInstanceRevision(ServiceInstance instance) {
        // 获取dubbo.metadata.revision对应的数据
        String existingInstanceRevision = getExportedServicesRevision(instance);
        // 获取服务元信息
        MetadataInfo metadataInfo = instance.getServiceMetadata();
        // 服务元信息计算新修订号
        String newRevision = metadataInfo.calAndGetRevision();
        // 如果新修订号与扩展属性中的修订号不相同返回true
        if (!newRevision.equals(existingInstanceRevision)) {
            instance.getMetadata()
                .put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, metadataInfo.getRevision());
            return true;
        }
        return false;
    }

    protected void reportMetadata(MetadataInfo metadataInfo) {
        if (metadataReport != null) {
            // 创建元数据对象
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.getRevision());
            // 1. 元数据类型是local
            // 2. 通过metadataReport确认是否需要汇报
            // 3. 元数据类型是remote
            if ((DEFAULT_METADATA_STORAGE_TYPE.equals(metadataType) && metadataReport.shouldReportMetadata()) || REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                metadataReport.publishAppMetadata(identifier, metadataInfo);
            }
        }
    }

    protected void unReportMetadata(MetadataInfo metadataInfo) {
        if (metadataReport != null) {
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.getRevision());
            if ((DEFAULT_METADATA_STORAGE_TYPE.equals(metadataType) && metadataReport.shouldReportMetadata()) || REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                metadataReport.unPublishAppMetadata(identifier, metadataInfo);
            }
        }
    }

    private String getCacheNameSuffix() {
        String name = this.getClass().getSimpleName();
        int i = name.indexOf(ServiceDiscovery.class.getSimpleName());
        if (i != -1) {
            name = name.substring(0, i);
        }
        URL url = this.getUrl();
        if (url != null) {
           return name.toLowerCase() + url.getBackupAddress();
        }
        return name.toLowerCase();
    }
}
