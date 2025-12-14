package com.ghost.server;// ClientSession.java

import com.ghost.api.dto.PlayerData;

public class GhostClientData {
    public String nonce = null;            // 認証用チャレンジ文字列
    public boolean authenticated = false;   // 認証済みフラグ
    public PlayerData playerData = null;   // プレイヤー情報（位置座標など）

    public GhostClientData() {
    }

    public GhostClientData(String nonce) {
        this.nonce = nonce;
    }
}

