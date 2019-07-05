package com.tian.disjob.anno;

import java.lang.annotation.*;

/**
 * 排它任务
 * Created by tianxiong on 2019/6/9.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExcJob {
    /**
     * 分组名, 也就是定时任务的类名,方法名. 没有指定处理的时候用全路径.
     * @return
     */
    String groupName() default "";

    /**
     * 节点名, 同一个分组下的节点名, 不能重复.建议用自增序列.
     * @return
     */
    String nodeName();

    /**
     * 排它任务, 某个节点可持有锁最大分钟数. 超过这个时间, 锁可能被其它节点获取到.
     * @return
     */
    int lockMax() default 60;

}
