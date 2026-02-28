# Legend of Astra (Frontend + Java Backend)

フロントエンドは `frontend/public` の静的ファイル、バックエンドは Java (`HttpServer`) です。

## 現在の起動構成

VS Code の実行とデバッグから以下を選択します。

- `RPGを起動（フロント+バックエンド）`

この構成は以下を同時起動します。

- `RPG Frontend` : `http://localhost:5500`
- `RPG Backend` : `http://localhost:8080`

## 前提

- Java 17 以上
- （任意）Maven

`mvn` がない場合でも、`scripts/start-backend.ps1` が `javac/java` で起動します。

## API エンドポイント

ベース: `http://localhost:8080/api/game`

- `GET /start` : 新規ゲーム初期化
- `GET /begin` : タイトルから開始
- `GET /state` : 現在状態取得
- `GET|POST /map/enter?node=...` : ノード進入
- `GET|POST /map/recover?item=...` : 地図画面で回復アイテム使用
- `GET|POST /shop/open` : 商店を開く
- `GET|POST /shop/close` : 商店を閉じる
- `GET|POST /shop/buy?item=...` : アイテム購入
- `GET|POST /menu?view=main|magic|item` : 戦闘メニュー切替
- `GET|POST /action?type=attack|magic|item|run&arg=...` : 行動実行
- `GET|POST /battle/continue` : 戦闘結果後に進行

## 難易度仕様（現在）

- 魔王（`final` ノード）撃破で `worldTier` が 1 上昇
- `worldTier` 上昇後は、全敵のレベル/HP/ATK/DEF/報酬が強化
- 強敵は特殊行動を使用
  - フルカウンター（与ダメの2倍反射）
  - 溜め攻撃
  - 連撃
  - 生命吸収

## 公開時の注意

- フロントの API ベースは自動判定です。
  - ローカル(`localhost`)では `:8080` を使用
  - それ以外は同一オリジンの `/api/game` を使用
- 明示指定したい場合は以下が使えます。
  - URL クエリ: `?apiBase=https://example.com/api/game`
  - `<meta name="api-base" content="https://example.com/api/game">`

## 環境変数

バックエンドは以下の環境変数に対応しています（`.env.example` 参照）。

- `APP_PORT` : バックエンド待受ポート（既定 `8080`）
- `CORS_ALLOW_ORIGINS` : 許可オリジン（`,` 区切り、既定 `*`）
- `CORS_ALLOW_METHODS` : 許可メソッド（既定 `GET,POST,OPTIONS`）
- `CORS_ALLOW_HEADERS` : 許可ヘッダ（既定 `Content-Type`）
- `CORS_ALLOW_CREDENTIALS` : `true/false`（既定 `false`）
- `SESSION_COOKIE_NAME` : セッションCookie名（既定 `RPG_SESSION`）
- `SESSION_TTL_MINUTES` : セッション有効期限（既定 `120` 分）

`scripts/start-backend.ps1` からも同等設定を起動引数で指定できます。

## セッション分離（本番向け）

- バックエンドは `HttpOnly` Cookie ベースでセッションIDを発行します。
- 各セッションIDごとにゲーム状態が分離されるため、ユーザー同士の進行は干渉しません。
- 一定時間アクセスがないセッションは `SESSION_TTL_MINUTES` に従って破棄されます。
- 同一オリジン公開（Nginx配下）を前提に、フロント側の追加設定は不要です。

## ヘルスチェック

- `GET /healthz` : `{"status":"ok"}` を返します。

ロードバランサや監視ではこのエンドポイントを利用してください。

## リバースプロキシ

Nginx のサンプル設定:

- `deployment/nginx/rpg.conf`

この設定では:
- `/` をフロント静的配信
- `/api/` をバックエンド（`127.0.0.1:8080`）へプロキシ
- `/healthz` をバックエンドのヘルスチェックへ転送

## 外部公開手順（本番）

対象: Ubuntu サーバ 1 台 + 独自ドメイン。

### 1. DNS を設定

- `A` レコードで `rpg.example.com` をサーバのグローバルIPへ向ける。

### 2. サーバへプロジェクトを配置

サーバへSSH接続し、任意の作業ディレクトリへこのリポジトリを置きます。

```bash
git clone <YOUR_REPO_URL>
cd project-ood-rpg-xxaxxx
```

### 3. 初回ブートストラップを実行

以下を root で実行します（Nginx/JDK/UFW/systemd/証明書の初期化まで）。

```bash
chmod +x deployment/prod/*.sh
sudo DOMAIN=rpg.example.com EMAIL=admin@example.com bash deployment/prod/bootstrap-ubuntu.sh
```

実行される内容:
- パッケージ導入（`nginx`, `openjdk-17-jdk`, `certbot` など）
- `rpg-backend.service` の登録と有効化
- Nginx サイト設定の配置
- Let's Encrypt 証明書の取得と HTTPS リダイレクト有効化
- `backend.env` 初期ファイル作成（`/opt/rpg/shared/backend.env`）

### 4. 本番環境変数を編集

```bash
sudoedit /opt/rpg/shared/backend.env
```

最低限以下を本番値に変更:
- `CORS_ALLOW_ORIGINS=https://rpg.example.com`
- `CORS_ALLOW_CREDENTIALS=true`
- `SESSION_TTL_MINUTES=120`（必要に応じ調整）

### 5. アプリをデプロイ

```bash
sudo REPO_URL=<YOUR_REPO_URL> BRANCH=main bash deployment/prod/deploy-app.sh
```

### 6. 動作確認

```bash
curl -i https://rpg.example.com/healthz
curl -I https://rpg.example.com/
```

ブラウザで `https://rpg.example.com` を開き、複数ブラウザ/シークレットウィンドウで進行が分離されることを確認します。

### 7. 以後の更新デプロイ

```bash
sudo REPO_URL=<YOUR_REPO_URL> BRANCH=main bash deployment/prod/deploy-app.sh
```

### 8. 緊急ロールバック

```bash
sudo bash deployment/prod/rollback.sh
```
