package com.marshal.halcyon.workflow.component;

import com.marshal.halcyon.core.listener.ContextRefreshedListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @auth: Marshal
 * @date: 2019/4/1
 * @desc: 扫描实现了ActivitiBean接口的类，供工作流调用,事实上，所有spring的bean都可以直接由activiti调用
 */
@Component
public class ActivitiBeanProvider extends HashMap<Object, Object> implements ContextRefreshedListener {

    @Override
    public void contextInitialized(ApplicationContext applicationContext) {
        Map<String, ActivitiBean> beans = applicationContext.getBeansOfType(ActivitiBean.class);
        beans.forEach((k, v) -> {
            this.put(k, v);
        });
    }
}
