package com.hmdp.controller;


import ch.qos.logback.core.pattern.util.RestrictedEscapeUtil;
import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;
    // 关注
    @PutMapping("/{id}/{isfollow}")
    public Result follow(@PathVariable("id") Long followUserId,@PathVariable("isfollow") Boolean isFollow) {
        return followService.follow(followUserId,isFollow);
    }
    // 取消关注
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }
}
