package com.tian.disjob.anno;

import java.lang.annotation.*;

/**
 * 多个定时任务, 需要进行数据分片的注解
 * Created by tianxiong on 2019/6/9.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModJob {
}
