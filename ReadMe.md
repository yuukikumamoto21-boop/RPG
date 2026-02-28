# Legend of Astra

## 概要
Java（HttpServer）とHTML/CSS/JavaScriptを用いた
ブラウザ型RPG Webアプリケーションです。

フロントエンドとバックエンドを分離設計し、
単一サーバ前提のWeb構成として実装しています。

※ 現在の実装は「メモリ内セッション管理」であり、
プロセス再起動時にセッションは保持されません。
多台数構成や永続化には未対応です。

---

## 技術スタック
- Backend: Java (HttpServer)
- Frontend: HTML / CSS / JavaScript
- Infra想定: Nginx / systemd
- OS想定: Ubuntu Server

---

## セッション設計（現状仕様）
- HttpOnly Cookie ベースでセッションIDを発行
- セッションはメモリ内（sessionId → GameService）
- サーバ再起動時に全セッション消失
- 単一インスタンス前提（スケールアウト未対応）
- SESSION_TTL_MINUTES による自動破棄

---

## CORS設計
環境変数により制御可能：

- CORS_ALLOW_ORIGINS
- CORS_ALLOW_METHODS
- CORS_ALLOW_HEADERS
- CORS_ALLOW_CREDENTIALS

※ 同一オリジン運用であれば CORS_ALLOW_CREDENTIALS=true は必須ではありません。

---

## 起動方法（ローカル）

### 前提
- Java 17 以上
- （任意）Maven

### Backend
scripts/start-backend.ps1 で起動可能。

※ 起動引数で指定可能なのは以下のみ：
- APP_PORT
- CORS系設定

SESSION_COOKIE_NAME / SESSION_TTL_MINUTES は
環境変数からのみ設定可能です。

---

## API エンドポイント

ベース: http://localhost:8080/api/game

GET /start  
GET /begin  
GET /state  
GET|POST /map/enter  
GET|POST /map/recover  
GET|POST /shop/open  
GET|POST /shop/close  
GET|POST /shop/buy  
GET|POST /menu  
GET|POST /action  
GET|POST /battle/continue  

GET /healthz → {"status":"ok"}

---

## リバースプロキシ構成

Nginx 設定は2種類あります：

- development 用:
  deployment/nginx/rpg.conf

- 本番用:
  deployment/nginx/rpg.prod.conf.template

本番環境では **rpg.prod.conf.template を使用します。**

構成:
- / → フロント静的配信
- /api/ → バックエンドへプロキシ
- /healthz → バックエンド転送

---

## 本番公開前提

- 単一サーバ構成
- メモリ内セッション管理
- 永続化なし
- URLに到達可能なネットワーク環境で利用可能

---

## 今後の拡張案
- Redisによるセッション外部化
- DB永続化対応
- 多台数構成対応
- Docker化
