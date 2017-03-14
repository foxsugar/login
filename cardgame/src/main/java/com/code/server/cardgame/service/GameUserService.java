package com.code.server.cardgame.service;


import com.code.server.cardgame.core.MsgDispatch;
import com.code.server.cardgame.core.GameManager;
import com.code.server.cardgame.core.Player;
import com.code.server.cardgame.response.ErrorCode;
import com.code.server.cardgame.response.ResponseVo;
import com.code.server.cardgame.utils.SpringUtil;
import com.code.server.cardgame.utils.ThreadPool;
import com.code.server.db.Service.UserService;
import com.code.server.db.model.ServerInfo;
import com.code.server.db.model.User;
import com.code.server.gamedata.UserVo;
import io.netty.channel.ChannelHandlerContext;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by win7 on 2017/3/10.
 */

@Service
public class GameUserService {




    private void doLogin(User user ,ChannelHandlerContext ctx){
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIP = insocket.getAddress().getHostAddress();
        user.setIpConfig(clientIP);
        Player player = new Player();
        player.setUser(user);
        player.setCtx(ctx);
        player.setUserId(user.getUserId());
        ctx.channel().attr(MsgDispatch.attributeKey).set(user.getUserId());
        GameManager.getInstance().addPlayer(player);


    }
    public int login(String account, String password, ChannelHandlerContext ctx) {

        Player player = GameManager.getInstance().getPlayerByAccount(account);
        if (player != null) {

            //密码不正确
            if(!password.equals( player.getUser().getPassword())){
                return ErrorCode.USERID_ERROR;
            }
            //todo 踢人
            if (ctx != player.getCtx()) {
                player.getCtx().close();
            }
            doLogin(player.getUser(),ctx);
            ResponseVo vo = new ResponseVo("userService", "login", getUserVo(player.getUser()));
            MsgDispatch.sendMsg(ctx,vo);


        } else {


            ThreadPool.getInstance().executor.execute(()->{
                ResponseVo vo = new ResponseVo("userService","login",0);
                UserService userService = SpringUtil.getBean(UserService.class);
                User user = userService.getUserByAccountAndPassword(account, password);
                //密码错误
                if (user == null) {
                    if (GameManager.getInstance().serverInfo.getAppleCheck() == 1) {
                        user = createUser(account, password);
                        userService.userDao.save(user);
                    } else {

                        vo.setCode(ErrorCode.USERID_ERROR);
                        MsgDispatch.sendMsg(ctx,vo);
                        return;
                    }
                }
                //加入缓存
                doLogin(user, ctx);
                vo.setParams(getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);
            });
        }



        return 0;
    }

    private UserVo getUserVo(User user){
        UserVo vo = new UserVo();


        vo.setId(user.getUserId());
        vo.setIpConfig(user.getIpConfig());
        vo.setAccount(user.getAccount());
        vo.setImage(user.getImage());
        vo.setMarquee(GameManager.getInstance().constant.getMarquee());
        vo.setSex(user.getSex());
        vo.setOpenId(user.getOpenId());
        vo.setMoney(user.getMoney());
        vo.setVip(user.getVip());
        vo.setUsername(user.getUsername());

        vo.setSeatId("0");
        vo.setRoomId("0");


        return vo;
    }

    public int appleCheck(ChannelHandlerContext ctx){


        ServerInfo serverInfo = GameManager.getInstance().serverInfo;
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("isInAppleCheck", serverInfo.getAppleCheck());
        jSONObject.put("address", serverInfo.getAddress());
        jSONObject.put("appleVersion", serverInfo.getVersionOfIos());
        jSONObject.put("androidVersion", serverInfo.getVersionOfAndroid());

        ResponseVo vo = new ResponseVo("userService","appleCheck",jSONObject);
        MsgDispatch.sendMsg(ctx,vo);
        return 0;
    }



    public int checkOpenId(final String openId,String username, final String image,int sex,ChannelHandlerContext ctx){

        ThreadPool.getInstance().executor.execute(()->{
            ResponseVo vo = null;
            UserService userService = SpringUtil.getBean(UserService.class);

            Player player = null;
            if(GameManager.getInstance().openId_uid.containsKey(openId)){
                long userId = GameManager.getInstance().openId_uid.get(openId);
                if (GameManager.getInstance().players.containsKey(userId)) {
                    player = GameManager.getInstance().players.get(userId);
                }
            }
            User user = null;
            if (player != null) {
                user = player.getUser();
            } else {
                user = userService.userDao.getUserByOpenId(openId);
            }

            String img = image;
            if(img == null || img.equals("")){
                img = "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=253777390,947512827&fm=23&gp=0.jpg/96";

            }

            if(user == null) {
                user = new User();
                user.setId(-100);
                user.setUserId(GameManager.getInstance().nextId());
                user.setOpenId(openId);
                user.setAccount(UUID.randomUUID().toString());
                user.setPassword("111111");
                try {
                    user.setUsername(URLEncoder.encode(username, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                user.setImage(img);
                user.setSex(sex);
                user.setVip(0);
                user.setUuid("0");
                user.setMoney(GameManager.getInstance().constant.getInitMoney());
                userService.userDao.save(user);

                doLogin(user,ctx);
                vo = new ResponseVo("userService", "checkOpenId", getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);

            }else{
                try {
                    user.setUsername(URLEncoder.encode(username, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                user.setImage(image);
                user.setSex(sex);
                userService.userDao.save(user);

                doLogin(user,ctx);
                vo = new ResponseVo("userService", "checkOpenId", getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);

            }
        });

        return 0;
    }


    private User createUser(String account,String password){
        User newUser = new User();
        newUser.setId(-100);
        newUser.setUserId(GameManager.getInstance().nextId());
        newUser.setAccount(account);
        newUser.setPassword(password);
        newUser.setOpenId(""+newUser.getUserId());
        try {
            newUser.setUsername(URLEncoder.encode(account, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        newUser.setImage("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=253777390,947512827&fm=23&gp=0.jpg/96");
        newUser.setSex(1);
        newUser.setVip(0);

        newUser.setUuid("0");
        newUser.setMoney(100);







        return newUser;
    }
}