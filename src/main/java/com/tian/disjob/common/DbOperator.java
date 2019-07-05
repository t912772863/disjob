package com.tian.disjob.common;

import com.tian.disjob.container.BeanContainer;
import com.tian.disjob.entity.ExcLock;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by tianxiong on 2019/6/9.
 */
public class DbOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbOperator.class);

    public static Connection getConnection(){
        BasicDataSource dataSource = BeanContainer.getBean(BasicDataSource.class);
        if(dataSource == null){
            throw new RuntimeException("found no data source.");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取某个分组的锁信息
     * @param groupName
     * @return
     */
    public static ExcLock queryExcLock(String groupName){
        Connection connection = getConnection();
        String sql = "select * from dis_job.exc_lock where group_name = ?;";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1,groupName);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            if(resultSet.getFetchSize()>0){
                ExcLock excLock = new ExcLock();
                excLock.setId(resultSet.getLong("id"));
                excLock.setCreateTime(resultSet.getDate("create_time"));
                excLock.setGroupName(resultSet.getString("group_name"));
                excLock.setLockStatus(resultSet.getInt("lock_status"));
                excLock.setMaxLockTime(resultSet.getInt("max_lock_time"));
                excLock.setModifyTime(resultSet.getDate("modify_time"));
                excLock.setVersion(resultSet.getInt("version"));
                return excLock;
            }
        } catch (SQLException e) {
            LOGGER.error("error", e);
        }
        return null;
    }

    /**
     * 获取一个可用锁对象
     * @param groupName
     * @return
     */
    public static ExcLock getExcLock(String groupName){
        Connection connection = getConnection();
        String sql = "select * from dis_job.exc_lock where group_name = ? and (lock_status = 0 or date_add(modify_time, interval max_lock_time, minute) < now());";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1,groupName);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            if(resultSet.getFetchSize()>0){
                ExcLock excLock = new ExcLock();
                excLock.setId(resultSet.getLong("id"));
                excLock.setCreateTime(resultSet.getDate("create_time"));
                excLock.setGroupName(resultSet.getString("group_name"));
                excLock.setLockStatus(resultSet.getInt("lock_status"));
                excLock.setMaxLockTime(resultSet.getInt("max_lock_time"));
                excLock.setModifyTime(resultSet.getDate("modify_time"));
                excLock.setVersion(resultSet.getInt("version"));
                return excLock;
            }
        } catch (SQLException e) {
            LOGGER.error("error", e);
        }
        return null;
    }

    /**
     * 锁定,或者释放锁
     * @param groupName
     * @param version
     * @param lockStatus
     * @return
     */
    public static boolean updateLockStatus(String groupName, int version, int lockStatus){
        Connection connection = getConnection();
        String sql = "update dis_job.exc_lock set lock_status = ?, modify_time = now(), version = version+1 where group_name = ? and version = ?;";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1,lockStatus);
            statement.setString(2, groupName);
            statement.setInt(3, version);

            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            if(resultSet.getFetchSize()>0){
             return true;
            }
        } catch (SQLException e) {
            LOGGER.error("error", e);
        }
        return false;
    }
}
