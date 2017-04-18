package com.code.server.cardgame.service;


import com.code.server.cardgame.core.GameManager;
import com.code.server.cardgame.core.MsgDispatch;
import com.code.server.cardgame.core.Player;
import com.code.server.cardgame.core.room.Room;
import com.code.server.cardgame.core.room.RoomTanDaKeng;
import com.code.server.cardgame.encoding.Notice;
import com.code.server.cardgame.response.*;
import com.code.server.cardgame.utils.SpringUtil;
import com.code.server.cardgame.utils.ThreadPool;
import com.code.server.db.Service.ConstantService;
import com.code.server.db.Service.UserService;
import com.code.server.db.model.Constant;
import com.code.server.db.model.ServerInfo;
import com.code.server.db.model.User;
import io.netty.channel.ChannelHandlerContext;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import static com.code.server.cardgame.core.GameManager.getUserVo;

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
        ctx.channel().attr(GameManager.attributeKey).set(user.getUserId());
        GameManager.getInstance().addPlayer(player);
        player.setLastSendMsgTime(System.currentTimeMillis());
        GameManager.getInstance().getKickUser().remove(player);


    }

    private void sendExit(Player player){
        Notice notice = new Notice();
        notice.setMessage("notice exit");
        player.sendMsg("userService","noticeExit",notice);
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
                sendExit(player);
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
                        userService.save(user);
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


    public int getUserRecodeByUserId(Player player, int type){
        User user = player.getUser();
        user.getRecord().getRoomRecords().get(type);
        player.sendMsg("userService","getUserRecodeByUserId",user.getRecord().getRoomRecords().get(type));
        return 0;
    }

    public int appleCheck(ChannelHandlerContext ctx){
        ServerInfo serverInfo = GameManager.getInstance().serverInfo;
        Constant constant = GameManager.getInstance().constant;
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("isInAppleCheck", serverInfo.getAppleCheck());
        jSONObject.put("address", constant.getDownload());
        jSONObject.put("appleVersion", serverInfo.getVersionOfIos());
        jSONObject.put("androidVersion", serverInfo.getVersionOfAndroid());

        ResponseVo vo = new ResponseVo("userService","appleCheck",jSONObject);
        MsgDispatch.sendMsg(ctx,vo);
        return 0;
    }


    public int getUserMessage(Player player){
        User user = player.getUser();
        UserVo userVo = getUserVo(user);
        ResponseVo vo = new ResponseVo("userService", "getUserMessage", userVo);
        player.sendMsg(vo);
        return 0;
    }

    public int reconnection(Player player){
        ReconnectResp reconnectResp = new ReconnectResp();
        reconnectResp.setExist(false);
        Room room = GameManager.getInstance().getRoomByUser(player.getUserId());
        if(room != null) {
            reconnectResp.setExist(true);
            if(room instanceof RoomTanDaKeng){
                reconnectResp.setRoom(new RoomVo((RoomTanDaKeng) room,player));
            }else{
                reconnectResp.setRoom(new RoomVo(room,player));
            }
        }
        ResponseVo vo = new ResponseVo("userService", "reconnection", reconnectResp);
        player.sendMsg(vo);
        return 0;
    }

    public int getRecord(Player player,int type) {
        User user = player.getUser();
        user.getRecord().getRoomRecords().get(type);


        return 0;
    }


    public int bindReferrer(Player player, int referrerId){
        User user = player.getUser();
        if(referrerId<=0 || user.getReferee()!=0){
            return ErrorCode.CAN_NOT_BING_REFERRER;
        }
        user.setReferee(referrerId);
        user.setMoney(user.getMoney() + 10);
        GameManager.getInstance().getSaveUser2DB().add(user);
        player.sendMsg("userService","bindReferrer",0);

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
                //todo 踢人
                if (ctx != player.getCtx()) {
                    sendExit(player);
                }
            } else {
                user = userService.getUserByOpenId(openId);
            }

            String img = image;
            if(img == null || img.equals("")){
                img = "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=253777390,947512827&fm=23&gp=0.jpg/96";

            }

            if(user == null) {
                user = new User();
//                user.setId(0);
//                user.setUserId(GameManager.getInstance().nextId());
                user.setOpenId(openId);
                user.setAccount(UUID.randomUUID().toString());
                user.setPassword("111111");
                try {
                    user.setUsername(URLDecoder.decode(username, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                user.setImage(img);
                user.setSex(sex);
                user.setVip(0);
                user.setUuid("0");
                user.setMoney(GameManager.getInstance().constant.getInitMoney());
                userService.save(user);

                doLogin(user,ctx);
                vo = new ResponseVo("userService", "checkOpenId", getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);

            }else{
                try {
                    user.setUsername(URLDecoder.decode(username, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                user.setImage(image);
                user.setSex(sex);
                userService.save(user);

                doLogin(user,ctx);
                vo = new ResponseVo("userService", "checkOpenId", getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);

            }
        });

        return 0;
    }

    public static void main(String[] args) {
        String s = "家用饮水机";
        try {
            String ss = URLDecoder.decode(s,"utf-8");
            System.out.println(ss);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private User createUser(String account,String password){
        User newUser = new User();
//        newUser.setId(-100);
//        newUser.setUserId(GameManager.getInstance().nextId());
        newUser.setAccount(account);
        newUser.setPassword(password);
        newUser.setOpenId(""+GameManager.getInstance().nextId());
        try {
            newUser.setUsername(URLDecoder.decode(account, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        newUser.setImage("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=253777390,947512827&fm=23&gp=0.jpg/96");
        newUser.setSex(1);
        newUser.setVip(0);
        newUser.setUuid("0");
        newUser.setMoney(GameManager.getInstance().constant.getInitMoney());

        return newUser;
    }

    public int getUserMessage(long userId,ChannelHandlerContext ctx) {

        ThreadPool.getInstance().executor.execute(()->{
            UserService userService = SpringUtil.getBean(UserService.class);
            Player player  = GameManager.getInstance().players.get(userId);
            User user = null;
            if (player != null) {
                user = player.getUser();
            } else {
                user = userService.getUserByUserId(userId);
            }
            ResponseVo vo = new ResponseVo("userService","getUserMessage",getUserVo(user));
            MsgDispatch.sendMsg(ctx,vo);
        });
        return 0;
    }

    public int getUserImage(long userId,ChannelHandlerContext ctx) {
        ThreadPool.getInstance().executor.execute(()->{
            UserService userService = SpringUtil.getBean(UserService.class);
            Player player  = GameManager.getInstance().players.get(userId);
            User user = null;
            if (player != null) {
                user = player.getUser();
            } else {
                user = userService.getUserByUserId(userId);
            }

            JSONObject jSONObject = new JSONObject();
            jSONObject.put("Image", user.getImage());

            ResponseVo vo = new ResponseVo("userService","getUserImage",jSONObject);
            MsgDispatch.sendMsg(ctx,vo);
        });
        return 0;
    }


    public int register(long userId,ChannelHandlerContext ctx) {

        ThreadPool.getInstance().executor.execute(()->{
            UserService userService = SpringUtil.getBean(UserService.class);
            Player player  = GameManager.getInstance().players.get(userId);
            User user = null;
            if (player != null) {
                user = player.getUser();
            } else {
                user = userService.getUserByUserId(userId);
            }
            if(user==null){
                ResponseVo vo = new ResponseVo("userService","register",0);
                MsgDispatch.sendMsg(ctx,vo);
            }else{
                ResponseVo vo = new ResponseVo("userService","register",getUserVo(user));
                MsgDispatch.sendMsg(ctx,vo);
            }
        });
        return 0;



    }
}
