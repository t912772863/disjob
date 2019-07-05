package com.tian.disjob.init;

import com.tian.disjob.anno.ExcJob;
import com.tian.disjob.anno.ModJob;
import com.tian.disjob.common.DbOperator;
import com.tian.disjob.container.BeanContainer;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 框架初始化类, 该框架依赖于spring框架的相关模块.
 * 由于业务操作中, 一般情况下数据最终都会入库进行持久化, 因此该框架选择借助数据库进行分布式任务的通讯.
 *
 * 约定数据库名为: dis_job,
 *         表名: exc_job, 多个定时任务只能有一个执行.也就是排它任务
 *         表名: exc_lock, 排它任务的锁表.
 *         表名: mod_job, 多个任务同时执行, 但是对数据进行分布.
 *         表名: heart_log, 所有任务的心跳日志,记录任务名, 节点, 时间等信息.
 *
 * Created by tianxiong on 2019/6/9.
 */
@Component
public class DisJobInit implements ApplicationListener<ContextRefreshedEvent> {
    /**
     * 监听spring的容器加载完成事件,或者刷新事件
     *
     * 初始化完成后, 校验一下数据库表是否存在, 如果不存在则尝试创建.
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        checkDbAndTable();
        saveNodeInfo();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DisJobInit.class);

    private static final String USE_DB_SQL = "use dis_job;";
    private static final String TEST_EXC_JOB_SQL = "select * from dis_job.exc_job limit 1;";
    private static final String TEST_EXC_LOCK_SQL = "select * from dis_job.exc_lock limit 1;";
    private static final String TEST_MOD_JOB_SQL = "select * from dis_job.mod_job limit 1;";
    private static final String TEST_HEART_LOG_SQL = "select * from dis_job.heart_log limit 1;";
    private static final String QUERY_EXC_NODE_SQL = "select * from dis_job.exc_job where group_name = ? and node = ?;";
    private static final String INSERT_EXC_NODE_SQL = "insert into dis_job.exc_job (group_name, node, create_time, modify_time) VALUES (?,?,now(),now());";
    private static final String QUERY_EXC_LOCK_SQL = "select * from dis_job.exc_lock where group_name = ?;";
    private static final String INSERT_EXC_LOCK_SQL = "insert into dis_job.exc_lock (group_name,lock_status,max_lock_time,create_time,modify_time) values (?,0,?,now(),now());";
    private static final String UPDATE_EXC_LOCK_SQL = "update dis_job.exc_lock set max_lock_time = ? where group_name = ?;";


    public List<Method> excJobList = new ArrayList<>();
    public List<Method> modJobList = new ArrayList<>();

    /**
     * 查看数据库和表是否存在.
     */
    private void checkDbAndTable(){
        // 拿到数据库连接信息, 进行切换数据库, 查询几个表等操作, 看是否正常.
        BasicDataSource dataSource = BeanContainer.getBean(BasicDataSource.class);
        if(dataSource == null){
            throw new RuntimeException("found no data source.");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(USE_DB_SQL);
            statement.execute();

        } catch (SQLException e) {
            // 查看这里的异常类型, 如果是库不存在, 或者表不存在, 则尝试创建.

            throw new RuntimeException(e);
        }

        try {
            PreparedStatement statement = connection.prepareStatement(TEST_EXC_JOB_SQL);
        } catch (SQLException e) {
            // 如果是表不存在,则尝试创建

            throw new RuntimeException(e);
        }
        try {
            PreparedStatement statement = connection.prepareStatement(TEST_EXC_LOCK_SQL);
        } catch (SQLException e) {
            // 如果是表不存在,则尝试创建

            throw new RuntimeException(e);
        }
        try {
            PreparedStatement statement = connection.prepareStatement(TEST_MOD_JOB_SQL);
        } catch (SQLException e) {
            // 如果是表不存在,则尝试创建

            throw new RuntimeException(e);
        }
        try {
            PreparedStatement statement = connection.prepareStatement(TEST_HEART_LOG_SQL);
        } catch (SQLException e) {
            // 如果是表不存在,则尝试创建

            throw new RuntimeException(e);
        }
    }

    /**
     * 保存当前节点的定时任务信息
     */
    private void saveNodeInfo(){
        findTargetMethod();
        saveExcJob();
        saveModJob();
    }

    private void saveExcJob() {
        for(Method m: excJobList){
            saveExcJobNode(m);
            saveExcJobLock(m);
        }

    }

    private void saveExcJobNode(Method m){
        Connection connection = DbOperator.getConnection();
        ExcJob excJob = AnnotationUtils.findAnnotation(m, ExcJob.class);
        String groupName = excJob.groupName();
        String nodeName = excJob.nodeName();
        // 先查看当前节点信息在数据库中是否已经存在, 已经存在,则可能是服务不是第一次启动了. 不用重复入库.
        try {
            PreparedStatement statement = connection.prepareStatement(QUERY_EXC_NODE_SQL);
            statement.setString(1, groupName);
            statement.setString(2, nodeName);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();

            int fetchSize = resultSet.getFetchSize();
            if(fetchSize>0){
                // 已经存在
                LOGGER.info("groupName: {}, nodeName: {}, had exist.", groupName, nodeName);
            }else {
                PreparedStatement addStatement = connection.prepareStatement(INSERT_EXC_NODE_SQL);
                addStatement.setString(1, groupName);
                addStatement.setString(2, nodeName);
                addStatement.execute();
            }
        } catch (SQLException e) {
            LOGGER.info("error", e);
        }
    }

    private void saveExcJobLock(Method m){
        Connection connection = DbOperator.getConnection();
        ExcJob excJob = AnnotationUtils.findAnnotation(m, ExcJob.class);
        String groupName = excJob.groupName();
        int maxLockTime = excJob.lockMax();
        try {
            PreparedStatement statement = connection.prepareStatement(QUERY_EXC_LOCK_SQL);
            statement.setString(1, groupName);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();

            int fetchSize = resultSet.getFetchSize();
            if(fetchSize>0){
                // 已经存在, 查看一下锁的最大锁定时间是否有变动, 有则更新
                LOGGER.info("groupName: {}, had exist.", groupName);
                int maxLock = resultSet.getInt("max_lock_time");
                if(maxLock != maxLockTime){
                    PreparedStatement updateStatement = connection.prepareStatement(UPDATE_EXC_LOCK_SQL);
                    updateStatement.setInt(1, maxLockTime);
                    updateStatement.setString(2, groupName);
                    updateStatement.execute();
                }
            }else {
                PreparedStatement addStatement = connection.prepareStatement(INSERT_EXC_LOCK_SQL);
                addStatement.setString(1, groupName);
                addStatement.setInt(2,maxLockTime);
                addStatement.execute();
            }
        } catch (SQLException e) {
            LOGGER.info("error", e);
        }
    }

    private void saveModJob() {
    }

    /**
     * 找到要进行分布式定时任务管理的方法
     */
    private void findTargetMethod(){
        // 找到目标方法, 这里直接遍历容器所有对象的所有方法, 找目标注解.
        List<Object> allBean = BeanContainer.getAllBean();
        for(Object o: allBean){
            Class clazz = o.getClass();
            Method[] methods = clazz.getMethods();
            for(Method m: methods){
                /*
                 * 注意, 常规的思路可以能过jdk的方法获取method对象上的注解, 但是由于spring中可能会通过CGLIB进行代理,而
                 * 代理后的对象, 方法上的注解会被忽略, 这时候通过 ExcJob excJob = m.getAnnotation(ExcJob.class);就永远
                 * 获取不到注解对象了, spring提供了一个工具类,AnnotationUtils.findAnnotation(m, ExcJob.class);
                 * 该方法不仅在当前对象找目标注解, 还会去其父类的这个方法上找, 这样就不受CGLIB代理的影响了.
                 */
//                ExcJob excJob = m.getAnnotation(ExcJob.class);
                ExcJob excJob = AnnotationUtils.findAnnotation(m, ExcJob.class);
                if(excJob != null){
                    excJobList.add(m);
                    continue;
                }
                ModJob modJob =  m.getAnnotation(ModJob.class);
                if(modJob != null){
                    modJobList.add(m);
                    continue;
                }
            }

        }

    }
}
