package com.tian.disjob.entity;

import java.util.Date;

/**
 * 排它任务,锁对象
 * Created by tianxiong on 2019/6/10.
 */
public class ExcLock {
    private Long id;
    private String groupName;
    private Integer lockStatus;
    private Integer maxLockTime;
    private Integer version;
    private Date createTime;
    private Date modifyTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(Integer lockStatus) {
        this.lockStatus = lockStatus;
    }

    public Integer getMaxLockTime() {
        return maxLockTime;
    }

    public void setMaxLockTime(Integer maxLockTime) {
        this.maxLockTime = maxLockTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public String toString() {
        return "ExcLock{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", lockStatus=" + lockStatus +
                ", maxLockTime=" + maxLockTime +
                ", version=" + version +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }
}
