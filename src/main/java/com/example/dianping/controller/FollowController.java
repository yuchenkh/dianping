package com.example.dianping.controller;

import com.example.dianping.dto.Result;
import com.example.dianping.service.IFollowService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 好友关注相关的接口。最后编辑于：2022-7-1。
 * @author yuchen
 */
@RestController
@RequestMapping("/follow")
@AllArgsConstructor
public class FollowController {

    private final IFollowService followService;

    /**
     * 查询某个用户是否关注了另一用户。
     * @param userId    被关注用户的 ID
     * @return          查询结果
     */
    @GetMapping("/or/not/{userId}")
    public Result followed(@PathVariable Long userId) {
        return followService.followed(userId);
    }

    /**
     * 关注另一用户。
     * @param userId    要关注的用户 ID
     * @return          操作结果
     */
    @PutMapping("/{userId}/true")
    public Result follow(@PathVariable Long userId) {
        return followService.follow(userId);
    }

    /**
     * 取消关注另一用户。
     * @param userId    要取消关注的用户 ID
     * @return          操作结果
     */
    @PutMapping("/{userId}/false")
    public Result unfollow(@PathVariable Long userId) {
        return followService.unfollow(userId);
    }

    /**
     * 查询当前用户与指定用户的共同关注列表
     * @param userId    另一个用户的 ID
     * @return          用户列表
     */
    @GetMapping("/common/{userId}")
    public Result commonFollowing(@PathVariable Long userId) {
        return followService.commonFollowing(userId);
    }
}
