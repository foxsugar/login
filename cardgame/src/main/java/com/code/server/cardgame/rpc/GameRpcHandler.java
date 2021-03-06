package com.code.server.cardgame.rpc;

import com.code.server.cardgame.bootstarp.Shutdown;
import com.code.server.cardgame.handler.MessageHolder;
import com.code.server.cardgame.core.GameManager;
import com.code.server.cardgame.core.Player;
import com.code.server.cardgame.handler.GameProcessor;
import com.code.server.cardgame.utils.SpringUtil;
import com.code.server.cardgame.utils.ThreadPool;
import com.code.server.db.Service.ConstantService;
import com.code.server.db.Service.ServerService;
import com.code.server.db.Service.UserService;
import com.code.server.db.model.User;
import com.code.server.rpc.idl.*;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sunxianping on 2017/3/29.
 */
public class GameRpcHandler implements GameRPC.AsyncIface {






    @Override
    public void charge(Order order, AsyncMethodCallback<Integer> resultHandler) throws TException {
        MessageHolder<Integer> messageHolder = new MessageHolder<>();
        messageHolder.msgType = MessageHolder.MSG_TYPE_THRIFT;
        messageHolder.message = order;

        MessageHolder.RpcHolder<Integer> rpcHolder = new MessageHolder.RpcHolder<>();
        rpcHolder.thriftCallback = resultHandler;
        rpcHolder.rpcMethod = "charge";
        messageHolder.rpcHolder = rpcHolder;
        //加进队列
        GameProcessor.getInstance().messageQueue.add(messageHolder);
    }

    @Override
    public void getUserInfo(long userId, AsyncMethodCallback<com.code.server.rpc.idl.User> resultHandler) throws TException {
        Player player = GameManager.getInstance().getPlayers().get(userId);
        User user;
        if (player != null) {
            user = player.getUser();


        }else {
            UserService userService = SpringUtil.getBean(UserService.class);
            user = userService.getUserByUserId(userId);

        }

        com.code.server.rpc.idl.User userRep = new com.code.server.rpc.idl.User();
        if (user != null) {
            userRep.setId(user.getUserId());
            userRep.setGold(user.getGold());
            userRep.setMoney(user.getMoney());
            userRep.setUsername(user.getUsername());
        }
        resultHandler.onComplete(userRep);
    }

    @Override
    public void exchange(Order order, AsyncMethodCallback<Integer> resultHandler) throws TException {
        MessageHolder<Integer> messageHolder = new MessageHolder<>();
        messageHolder.msgType = MessageHolder.MSG_TYPE_THRIFT;
        messageHolder.message = order;

        MessageHolder.RpcHolder<Integer> rpcHolder = new MessageHolder.RpcHolder<>();
        rpcHolder.thriftCallback = resultHandler;
        rpcHolder.rpcMethod = "exchange";
        messageHolder.rpcHolder = rpcHolder;
        //加进队列
        GameProcessor.getInstance().messageQueue.add(messageHolder);
    }

    @Override
    public void modifyMarquee(String str, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        GameManager.getInstance().constant.setMarquee(str);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyDownload(String str, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        GameManager.getInstance().constant.setDownload(str);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyAndroidVersion(String str, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ServerService serverService = SpringUtil.getBean(ServerService.class);
        GameManager.getInstance().serverInfo.setVersionOfAndroid(str);
        serverService.serverInfoDao.save( GameManager.getInstance().serverInfo);
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyIOSVersion(String str, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ServerService serverService = SpringUtil.getBean(ServerService.class);
        GameManager.getInstance().serverInfo.setVersionOfIos(str);
        serverService.serverInfoDao.save( GameManager.getInstance().serverInfo);
        resultHandler.onComplete(0);
    }

    @Override
    public void shutdown(AsyncMethodCallback<Integer> resultHandler) throws TException {
        Shutdown.shutdown();
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyInitMoney(int money, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        GameManager.getInstance().constant.setInitMoney(money);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyAppleCheck(int status, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ServerService serverService = SpringUtil.getBean(ServerService.class);
        GameManager.getInstance().serverInfo.setAppleCheck(status);
        serverService.serverInfoDao.save( GameManager.getInstance().serverInfo);
        resultHandler.onComplete(0);
    }

    @Override
    public void modifyDownload2(String str, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        GameManager.getInstance().constant.setDownload2(str);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void addBlackList(long userId, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        if(GameManager.getInstance().constant.getBlackList() == null){
            GameManager.getInstance().constant.setBlackList(new HashSet<>());
        }
        GameManager.getInstance().constant.getBlackList().add(userId);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void removeBlackList(long userId, AsyncMethodCallback<Integer> resultHandler) throws TException {
        ConstantService constantService = SpringUtil.getBean(ConstantService.class);
        if(GameManager.getInstance().constant.getBlackList() == null){
            GameManager.getInstance().constant.setBlackList(new HashSet<>());
        }
        GameManager.getInstance().constant.getBlackList().remove(userId);
        constantService.constantDao.save(GameManager.getInstance().constant);
        resultHandler.onComplete(0);
    }

    @Override
    public void getBlackList(AsyncMethodCallback<Set<Long>> resultHandler) throws TException {
        if(GameManager.getInstance().constant.getBlackList() == null){
            GameManager.getInstance().constant.setBlackList(new HashSet<>());
        }
        resultHandler.onComplete(GameManager.getInstance().constant.getBlackList());
    }

    @Override
    public void getOnlineUser(AsyncMethodCallback<OnlineNum> resultHandler) throws TException {
        OnlineNum onlineNum = new OnlineNum();
        onlineNum.setRoomNum(GameManager.getInstance().rooms.size());
        onlineNum.setUserNum(GameManager.getInstance().players.size());
        resultHandler.onComplete(onlineNum);
    }

    private static void saveUser(User user){
        if (user == null) {
            return;
        }
        ThreadPool.getInstance().executor.execute(()->{
            UserService userService = SpringUtil.getBean(UserService.class);
            userService.save(user);
        });
    }

    public static void doCharge(MessageHolder<Integer> messageHolder){
        Order order = (Order) messageHolder.message;
        //玩家是否在内存中
        Player player = GameManager.getInstance().getPlayers().get(order.getUserId());
        if (player != null) {
            chargeLogic(player.getUser(), order, messageHolder,true);

        } else {
            //不在内存中
            ThreadPool.getInstance().executor.execute(()->{
                UserService userService = SpringUtil.getBean(UserService.class);
                User user = userService.getUserByUserId(order.getUserId());
                chargeLogic(user, order, messageHolder,false);
            });
        }
    }

    private static void chargeLogic(User user,Order order,MessageHolder<Integer> messageHolder,boolean isAsyncSave){
        if (user == null) {
            messageHolder.rpcHolder.thriftCallback.onComplete(RPCError.NO_USER.getValue());
            return;
        }
        if (order.getType() == ChargeType.money.getValue()) {
            double newMoney = user.getMoney() + order.getNum();
            user.setMoney(newMoney);
        } else if(order.getType() == ChargeType.gold.getValue()) {
            double newGold = user.getGold() + order.getNum();
            user.setGold(newGold);
        }
        if (isAsyncSave) {
            saveUser(user);
        } else {
            UserService userService = SpringUtil.getBean(UserService.class);
            userService.save(user);
        }

        messageHolder.rpcHolder.thriftCallback.onComplete(0);
    }


    public static void doExchange(MessageHolder<Integer> messageHolder){
        Order order = (Order) messageHolder.message;
        Player player = GameManager.getInstance().getPlayers().get(order.getUserId());
        if (player != null) {
            User user = player.getUser();
            exchangeLogic(user, order, messageHolder,true);
            saveUser(player.getUser());
        } else {
            //不在内存中
            ThreadPool.getInstance().executor.execute(()->{
                UserService userService = SpringUtil.getBean(UserService.class);
                User user = userService.getUserByUserId(order.getUserId());
                exchangeLogic(user, order, messageHolder,false);
                userService.save(user);
            });
        }
    }

    private static void exchangeLogic(User user,Order order,MessageHolder<Integer> messageHolder,boolean isAsyncSave){
        if (user.getMoney() < order.getNum()) {
            messageHolder.rpcHolder.thriftCallback.onComplete(RPCError.NO_USER.getValue());
            return;
        }
        double newMoney = user.getMoney() - order.getNum();
        user.setMoney(newMoney);
        user.setGold(user.getGold() + order.getNum());
        if (isAsyncSave) {
            saveUser(user);
        } else {
            UserService userService = SpringUtil.getBean(UserService.class);
            userService.save(user);
        }
        messageHolder.rpcHolder.thriftCallback.onComplete(RPCError.NO_USER.getValue());
    }

}
