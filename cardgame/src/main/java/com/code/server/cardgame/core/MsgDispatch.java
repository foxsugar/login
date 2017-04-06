package com.code.server.cardgame.core;

import com.code.server.cardgame.Message.MessageHolder;
import com.code.server.cardgame.core.game.GameDouDiZhu;
import com.code.server.cardgame.core.game.GameTianDaKeng;
import com.code.server.cardgame.core.room.GoldRoomPool;
import com.code.server.cardgame.core.room.Room;
import com.code.server.cardgame.core.room.RoomDouDiZhu;
import com.code.server.cardgame.core.room.RoomTanDaKeng;
import com.code.server.cardgame.response.ErrorCode;
import com.code.server.cardgame.response.ResponseVo;
import com.code.server.cardgame.rpc.RpcMsgDispatch;
import com.code.server.cardgame.service.GameChatService;
import com.code.server.cardgame.service.GameUserService;
import com.code.server.cardgame.utils.SpringUtil;
import com.code.server.db.model.User;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by sun on 2015/8/21.
 */

@Service
public class MsgDispatch {

    private final Logger logger = LoggerFactory.getLogger(MsgDispatch.class);

    private Gson gson = new Gson();

    public static void sendMsg(ChannelHandlerContext ctx, Object msg) {
        ctx.writeAndFlush(msg);
    }


    public void handleMessage(MessageHolder msgHolder) {
        Object message = msgHolder.message;
        switch (msgHolder.msgType) {
            case MessageHolder.MSG_TYPE_RPC:{
                RpcMsgDispatch.dispatch(msgHolder);
                break;
            }
            case MessageHolder.MSG_TYPE_CLIENT_JSON:{
                JSONObject jSONObject = (JSONObject) message;
                System.out.println("handle message== " + jSONObject);
//                logger.info("handle message== " + jSONObject);
                String service = jSONObject.getString("service");
                String method = jSONObject.getString("method");
                JSONObject params = jSONObject.getJSONObject("params");

                //逻辑
                int code = dispatchAllMsg(service, method, params, msgHolder.ctx);
                //客户端要的方法返回
                if (code != 0) {
                    ResponseVo vo = new ResponseVo(service, method, code);
                    sendMsg(msgHolder.ctx, vo);
                }
                break;
            }

        }


    }

    private void handleRpcMessage(){

    }

    private int dispatchAllMsg(String service, String method, JSONObject params, ChannelHandlerContext ctx) {
        switch (service) {
            case "userService":
                return dispatchUserService(method, params, ctx);
            case "roomService":
                return dispatchRoomService(method, params, ctx);
            case "gameService":
                return dispatchGameService(method, params, ctx);
            case "chatService":
                return dispatchChatService(method, params, ctx);

            case "gameTDKService":
                return dispatchGameService(method, params, ctx);
            default:
                return ErrorCode.REQUEST_PARAM_ERROR;
        }
    }

    private int dispatchUserService(String method, JSONObject params, ChannelHandlerContext ctx) {

        GameUserService gameUserService = SpringUtil.getBean(GameUserService.class);
        switch (method) {
            case "login":
                String account = params.getString("account");
                String password = params.getString("password");
                return gameUserService.login(account, password, ctx);
            case "appleCheck":
                return gameUserService.appleCheck(ctx);
            case "checkOpenId":
                String openId = params.getString("openId");
                String username = params.getString("username");
                String image = params.getString("image");
                int sex = Integer.parseInt(params.getString("sex"));
                return gameUserService.checkOpenId(openId, username, image, sex, ctx);
            case "getUserMessage":{
                Player player = GameManager.getPlayerByCtx(ctx);
                if (player == null) {
                    return ErrorCode.YOU_HAVE_NOT_LOGIN;
                }
                return gameUserService.getUserMessage(player);
            }
            case "reconnection":{
                Player player = GameManager.getPlayerByCtx(ctx);
                if (player == null) {
                    return ErrorCode.YOU_HAVE_NOT_LOGIN;
                }
                return gameUserService.reconnection(player);
            }
            case "getUserRecodeByUserId":{

                Player player = GameManager.getPlayerByCtx(ctx);
                if (player == null) {
                    return ErrorCode.YOU_HAVE_NOT_LOGIN;
                }
                int type = params.getInt("type");
                return gameUserService.getUserRecodeByUserId(player, type);

            }
            case "bindReferrer":{
                Player player = GameManager.getPlayerByCtx(ctx);
                if (player == null) {
                    return ErrorCode.YOU_HAVE_NOT_LOGIN;
                }
                int referrerId = params.getInt("referrerId");
                return gameUserService.bindReferrer(player,referrerId);
            }

            case "getUserImage":
//                return gameUserService.getUserImage(userId,ctx);

            case "register":
//                return gameUserService.register(userId,ctx);


            default:

                return ErrorCode.REQUEST_PARAM_ERROR;
        }
    }

    private int dispatchRoomService(String method, JSONObject params, ChannelHandlerContext ctx) {
        Player player = GameManager.getPlayerByCtx(ctx);
        if (player == null) {
            return ErrorCode.YOU_HAVE_NOT_LOGIN;
        }

        switch (method) {
            case "createRoom":{

                int gameNumber = params.getInt("gameNumber");
                int multiple = params.getInt("maxMultiple");
                return RoomDouDiZhu.createRoom(player, gameNumber, multiple);
            }
            case "createRoomTDK":{

                int gameNumber = params.getInt("gameNumber");
                int multiple = params.getInt("maxMultiple");
                int personNumber = params.getInt("personNumber");
                return RoomTanDaKeng.createRoom(player, gameNumber,multiple,personNumber);
            }
            case "joinRoom": {
                String roomId = params.getString("roomId");
                Room room = GameManager.getInstance().rooms.get(roomId);
                if (room == null) {
                    return ErrorCode.CANNOT_JOIN_ROOM_NOT_EXIST;
                }
                return room.joinRoom(player);
            }
            case "joinRoomQuick":{
                double type = params.getInt("type");
                return GoldRoomPool.getInstance().addRoom(player, type);

            }
            case "quitRoom": {
                Room room = getRoomByPlayer(player);
                if (room == null) {
                    return ErrorCode.CAN_NOT_NO_ROOM;
                }
                return room.quitRoom(player);
            }
            case "getReady": {
                Room room = getRoomByPlayer(player);
                if (room == null) {
                    return ErrorCode.CAN_NOT_NO_ROOM;
                }
                return room.getReady(player);
            }
            case "dissolveRoom": {
                Room room = getRoomByPlayer(player);
                if (room == null) {
                    return ErrorCode.CAN_NOT_NO_ROOM;
                }
                return room.dissolution(player, true, method);
            }
            case "answerIfDissolveRoom":
                Room room = getRoomByPlayer(player);
                if (room == null) {
                    return ErrorCode.CAN_NOT_NO_ROOM;
                }
                boolean isAgree = "2".equals(params.getString("answer"));
                return room.dissolution(player, isAgree, method);
            default:
                return ErrorCode.REQUEST_PARAM_ERROR;
        }
    }


    private int dispatchGameService(String method, JSONObject params, ChannelHandlerContext ctx) {
        Player player = GameManager.getPlayerByCtx(ctx);
        if (player == null) {
            return -1;
        }

        Room room = getRoomByPlayer(player);
        if (room == null) {
            return ErrorCode.CAN_NOT_NO_ROOM;
        }
        GameDouDiZhu game = (GameDouDiZhu) room.getGame();
        if (game == null) {
            return ErrorCode.CAN_NOT_NO_GAME;
        }

        switch (method) {
            case "jiaoDizhu":
                boolean isJiao = params.getBoolean("isJiao");
                return game.jiaoDizhu(player, isJiao);
            case "qiangDizhu":
                boolean isQiang = params.getBoolean("isQiang");
                return game.qiangDizhu(player, isQiang);
            case "play":
                CardStruct cardStruct = gson.fromJson(params.getString("cards"), CardStruct.class);
                return game.play(player, cardStruct);
            case "pass":
                return game.pass(player);
            default:

                return ErrorCode.REQUEST_PARAM_ERROR;
        }
    }


    private int dispatchGameTDKService(String method, JSONObject params, ChannelHandlerContext ctx) {
        Player player = GameManager.getPlayerByCtx(ctx);
        if (player == null) {
            return -1;
        }

        RoomTanDaKeng room = getRoomTDKByPlayer(player);
        if (room == null) {
            return ErrorCode.CAN_NOT_NO_ROOM;
        }
        GameTianDaKeng game = (GameTianDaKeng) room.getGame();
        if (game == null) {
            return ErrorCode.CAN_NOT_NO_GAME;
        }

        switch (method) {
            case "bet"://下注
                int chip = params.getInt("chip");
                return game.bet(player, chip);
            case "call"://跟注
                return game.call(player);
            case "raise"://加注，踢
                return game.raise(player);
            case "pass"://不跟
                return game.pass(player);
            case "fold"://弃牌
                return game.fold(player);
            default:
                return ErrorCode.REQUEST_PARAM_ERROR;
        }
    }


    private int dispatchChatService(String method, JSONObject params, ChannelHandlerContext ctx) {
        Player player = GameManager.getPlayerByCtx(ctx);
        if (player == null) {
            return -1;
        }
        GameChatService chatService = SpringUtil.getBean(GameChatService.class);
        switch (method) {
            case "sendMessageToOne": {
                long acceptUserId = params.getLong("acceptUserId");
                String messageType = params.getString("messageType");
                String message = params.getString("message");
                return chatService.sendMessageToOne(player, acceptUserId, messageType, message);
            }
            case "sendMessage": {
                String messageType = params.getString("messageType");
                String message = params.getString("message");
                return chatService.sendMessage(player, messageType, message);
            }
            default: {
                return ErrorCode.REQUEST_PARAM_ERROR;
            }


        }

    }

    private Room getRoomByPlayer(Player player) {
        String roomId = GameManager.getInstance().getUserRoom().get(player.getUserId());
        if (roomId == null) {
            return null;
        }
        return GameManager.getInstance().rooms.get(roomId);
    }


    private RoomTanDaKeng getRoomTDKByPlayer(Player player) {
        String roomId = GameManager.getInstance().getUserRoom().get(player.getUserId());
        if (roomId == null) {
            return null;
        }
        return GameManager.getInstance().roomsOfTanDaKeng.get(roomId);
    }
}
