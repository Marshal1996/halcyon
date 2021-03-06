package com.marshal.halcyon.workflow.handler.impl;

import com.marshal.halcyon.core.component.SessionContext;
import com.marshal.halcyon.workflow.constant.ActivitiConstant;
import com.marshal.halcyon.workflow.entity.TaskActionRequestExt;
import com.marshal.halcyon.workflow.exception.TaskHandleException;
import com.marshal.halcyon.workflow.handler.TaskHandler;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.task.Task;
import org.activiti.rest.service.api.runtime.task.TaskActionRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @auth: Marshal
 * @date: 2019/7/4
 * @desc: 转交处理器
 */
public class DelegateTaskHandler implements TaskHandler {

    @Autowired
    private TaskService taskService;

    @Override
    public String getAction() {
        return ActivitiConstant.ACTION_DELEGATE;
    }

    @Override
    public void process(SessionContext sessionContext, Task taskEntity, TaskActionRequestExt actionRequest) throws TaskHandleException {
        if (!TaskActionRequest.ACTION_DELEGATE.equalsIgnoreCase(actionRequest.getAction())) {
            return;
        }
        Authentication.setAuthenticatedUserId(sessionContext.getEmployeeCode());
        String assignee = actionRequest.getAssignee();
        if (StringUtils.isEmpty(assignee)) {
            throw new TaskHandleException(TaskHandleException.DELEGATE_NO_ASSIGNEE);
        }

        String taskId = taskEntity.getId();
        taskService.addComment(taskId, taskEntity.getProcessInstanceId(), "action", "delegate");
        taskService.addComment(taskId, taskEntity.getProcessInstanceId(), "comment",
                sessionContext.getEmployeeCode() + "转交给" + assignee + "  " + actionRequest.getComment());
        taskService.setAssignee(taskId, assignee);
    }
}
