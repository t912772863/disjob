package com.tian.disjob.container;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.management.AttributeList;
import java.util.List;

/**
 * Created by tianxiong on 2019/6/9.
 */
@Component
public class BeanContainer implements ApplicationContextAware {
    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz){
        if(context == null){
            throw new RuntimeException("before invoke this method, context must be init.");
        }
        return context.getBean(clazz);
    }

    public static <T> T getBean(String beanName, Class<T> clazz){
        if(beanName == null){
            throw new RuntimeException("invalid param, beanName = "+beanName);
        }
        return context.getBean(beanName, clazz);
    }

    /**
     * 获取容器中所有的bean
     * @return
     */
    public static List<Object> getAllBean(){
        String[] beans = context.getBeanDefinitionNames();
        List<Object> beanList = new AttributeList(beans.length);
        for(int i=0;i<beans.length;i++){
            beanList.add(context.getBean(beans[i]));
        }
        return beanList;
    }
}
