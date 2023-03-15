package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 判断到底是关注还是取关
        if (isFollow) {
            // 关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean save = save(follow);

        } else {
            // 取关，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            remove(new QueryWrapper<Follow>()
                    .eq("user_id",userId)
                    .eq("follow_user_id",followUserId));

        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 获取登录用户
        Long id = UserHolder.getUser().getId();
        // 查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", id).eq("follow_id_userId", followUserId).count();
        // 判断
        return Result.ok(count > 0);
    }
}
