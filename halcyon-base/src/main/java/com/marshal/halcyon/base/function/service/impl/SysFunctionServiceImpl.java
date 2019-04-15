package com.marshal.halcyon.base.function.service.impl;

import com.github.pagehelper.PageHelper;

import com.marshal.halcyon.base.function.entity.SysFunction;
import com.marshal.halcyon.base.function.mapper.SysFunctionMapper;
import com.marshal.halcyon.base.function.service.SysFunctionService;
import com.marshal.halcyon.core.service.impl.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * author: Marshal
 * Date: 2018/11/5
 * Time: 22:03
 * Description:
 */
@Service
public class SysFunctionServiceImpl extends BaseServiceImpl<SysFunction> implements SysFunctionService {

    @Autowired
    SysFunctionMapper sysFunctionMapper;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Override
    public List<SysFunction> selectFunctions(SysFunction condition, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return sysFunctionMapper.selectFunctions(condition);
    }

    @Override
    public List<Map> getFunctionOptions() {
        return sysFunctionMapper.getFunctionOptions();
    }

    @Override
//    @Cacheable(value = "halcyon:cache:menu", key = "123")
    public List<SysFunction> getMenus() {
        List<SysFunction> topFunctionList = selectTopFunctions();
        getChildFunctions(topFunctionList);
        return topFunctionList;
    }

    /**
     * 生成首页菜单
     *
     * @return
     */
    @Override
    public List<SysFunction> selectTopFunctions() {
        return sysFunctionMapper.selectTopFunctions();
    }


    @Override
    public List<SysFunction> selectChildFunctions(Long functionId) {
        return sysFunctionMapper.selectChildFunctions(functionId);
    }

    /**
     * 递归填充最外层菜单的子菜单
     *
     * @param functionList
     * @return
     */
    public List<SysFunction> getChildFunctions(List<SysFunction> functionList) {
        for (SysFunction item : functionList) {
            List<SysFunction> childList = selectChildFunctions(item.getFunctionId());
            if (childList != null && childList.size() > 0) {
                item.setChildFunctions(childList);
                getChildFunctions(childList);
            } else {
                continue;
            }
        }
        return functionList;
    }

    @Override
    public List<Map> selectRoleFunctionAssignList(Long roleId) {
        return sysFunctionMapper.selectRoleFunctionAssignList(roleId);
    }
}
