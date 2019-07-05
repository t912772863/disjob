package com.tian.disjob.aop;

import com.tian.disjob.anno.ExcJob;
import com.tian.disjob.common.DbOperator;
import com.tian.disjob.entity.ExcLock;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tianxiong on 2019/6/10.
 */
@Aspect
@Component
public class ExcJobAop {
    private final Logger LOGGER = Logger.getLogger(ExcJobAop.class);

    private static final ConcurrentHashMap<String, ExcLock> excLockMap = new ConcurrentHashMap();

    @Before(value = "@annotation(com.tian.disjob.anno.ExcJob)")
    public void before(JoinPoint jp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature)jp.getSignature();
        Method targetMethod = methodSignature.getMethod();
        ExcJob excJob = targetMethod.getDeclaredAnnotation(ExcJob.class);
        String groupName = excJob.groupName();
        String nodeName = excJob.nodeName();
        ExcLock excLock = DbOperator.getExcLock(groupName);
        if(excLock == null){
            LOGGER.info(groupName+"."+nodeName+" get exc_lock failed.");
            return;
        }
        boolean idx = DbOperator.updateLockStatus(excLock.getGroupName(),excLock.getVersion(),1);
        if(!idx){
            LOGGER.info(groupName+"."+nodeName+" get exc_lock failed.");
            return;
        }
        excLockMap.put(groupName+"_"+nodeName, excLock);

    }

    @AfterReturning(value = "@annotation(com.tian.disjob.anno.ExcJob)")
    public void after(JoinPoint jp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature)jp.getSignature();
        Method targetMethod = methodSignature.getMethod();
        ExcJob excJob = targetMethod.getDeclaredAnnotation(ExcJob.class);
        String groupName = excJob.groupName();
        String nodeName = excJob.nodeName();
        ExcLock excLock = excLockMap.get(groupName+"_"+nodeName);
        boolean idx = DbOperator.updateLockStatus(excLock.getGroupName(),excLock.getVersion()+1,1);
        if(!idx){
            LOGGER.info(groupName+"."+nodeName+" get exc_lock failed.");
            return;
        }
        excLockMap.put(groupName+"_"+nodeName, excLock);

    }
}
