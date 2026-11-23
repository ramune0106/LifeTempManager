# LifeTempManager

Paper `1.21.11` (`v1_21_R7`) + MythicMobs `5.14.1+` 向けの温度管理プラグインです。

## 主な機能

- **プレイヤー個別温度管理**: デフォルト `20℃`、基本即死ライン `+100℃ / -80℃`
- **自然回復**: MythicMobs 等から温度変更を受けた際に待機カウント（デフォルト `10秒`）をリセット。経過後 `1秒` ごとに `20℃` ずつデフォルト温度へ回復
- **即死ライン逆算デメリット & 警告（A案）**:
  - 第1段階（即死 `-40℃` 手前）: 鈍足・盲目（防具 **2部位** で無効化）
  - 第2段階（即死 `-20℃` 手前）: 継続ダメージ ＋ 炎上/粉雪の視界エフェクト（防具 **4部位** で継続ダメージのみ無効化・視界エフェクトは維持）
  - 警告 Title（即死 `-20℃` 手前）: `config.yml` で指定した秒数ごとに繰り返し表示
  - 第3段階（即死ライン到達）: 即死（死亡時 `20℃` リセット）
- **リアルタイム BossBar 表示**: 毎秒（20tick）更新
- **専用防具（耐熱・耐冷独立計算）**: 1部位につき `+50℃ / -50℃` 延長（フルセットで `+300℃ / -280℃`）
- **空間での継続温度変化エリア**: WorldEdit 風のワンド／`pos1`・`pos2` 指定で直方体エリアを登録し、パーティクルで境界可視化可能
- **永続化**: プレイヤー PDC + `playerdata.yml` によりリログイン時も温度を維持（死亡時はリセット）

---

## MythicMobs 5.14+ 連携リファレンス

### 1. Mechanics（温度変更）

```yaml
ExampleSkills:
  Skills:
    # 温度を +20℃ 上昇させる
    - tempadd{v=20} @self
    # 温度を 40℃ 低下させる
    - tempadd{v=-40} @target
    # 温度を 20℃ に設定する
    - tempset{v=20} @self
    # 温度をデフォルト値 (20℃) にリセットする
    - tempreset @self
```

- パラメータ `v`（別名: `value`, `amount`, `a`）は MythicMobs のプレースホルダーにも対応しています。
- 任意オプション `rt=false`（`resettimer=false`）を付けると自然回復タイマーをリセットせずに変更できます（既定: `true`）。

### 2. Conditions（温度条件分岐）

```yaml
HotSkill:
  TargetConditions:
    - tempinrange{v=>=120}
  Skills:
    - message{m="&c120℃以上のためスキル発動！"} @target

BranchSkill:
  Skills:
    - skill{s=SkillA} @self ?tempinrange{v=>=80}
    - skill{s=SkillB} @self ?tempinrange{v=<=-20}
```

- 対応記法（MythicMobs `RangedDouble` 準拠）: `>120`, `>=120`, `<-20`, `<=-20`, `80to120`, `-20to80`, `20`

### 3. 専用防具の作成方法（MythicMobs Items）

MythicMobs の `NBT:` セクションに `lifetemp_heat` または `lifetemp_cold` を記載するだけで認識されます（キー名は `config.yml` で変更可能）。

```yaml
UraHeatHelmet:
  Id: NETHERITE_HELMET
  Display: '&c裏上級・耐熱兜'
  Lore:
    - '&7耐熱耐性: &c+50℃ (1部位)'
    - '&82部位で鈍足・盲目無効 / 4部位で継続ダメージ無効'
  NBT:
    lifetemp_heat: 50

UraColdChestplate:
  Id: NETHERITE_CHESTPLATE
  Display: '&b裏上級・耐冷胸当て'
  Lore:
    - '&7耐冷耐性: &b-50℃ (1部位)'
    - '&82部位で鈍足・盲目無効 / 4部位で継続ダメージ無効'
  NBT:
    lifetemp_cold: 50
```

※ゲーム内でアイテムを手持ちした状態で `/ltm armor set heat 50` を使って PDC タグを直接付与することも可能です。

---

## 管理コマンド (`/lifetemp` / `/ltm` / `/temp`)

| コマンド | 説明 | 権限 |
| :--- | :--- | :--- |
| `/ltm info [player]` | 現在温度・即死ライン・装備防具の耐性を表示 | `lifetempmanager.use` |
| `/ltm set <player> <温度>` | プレイヤーの温度を指定値に設定 | `lifetempmanager.admin` |
| `/ltm add <player> <変化量>` | プレイヤーの温度を加算/減算 | `lifetempmanager.admin` |
| `/ltm reset <player>` | 温度をデフォルト値（20℃）へリセット | `lifetempmanager.admin` |
| `/ltm armor set <heat\|cold> <延長温度> [部位数]` | 手持ちアイテムに耐性PDCを付与 | `lifetempmanager.admin` |
| `/ltm armor clear` | 手持ちアイテムの耐性PDCを削除 | `lifetempmanager.admin` |
| `/ltm armor inspect` | 手持ちアイテムの耐性情報を確認 | `lifetempmanager.admin` |
| `/ltm zone wand` | 範囲選択ワンドを入手（左クリック:`pos1` / 右クリック:`pos2`） | `lifetempmanager.admin` |
| `/ltm zone pos1` / `pos2` | 足元座標を `pos1` / `pos2` に設定 | `lifetempmanager.admin` |
| `/ltm zone create <名前> <変化量> <間隔秒>` | 選択範囲を温度変化エリアとして登録 | `lifetempmanager.admin` |
| `/ltm zone delete <名前>` | 温度変化エリアを削除 | `lifetempmanager.admin` |
| `/ltm zone list` | 登録済みエリア一覧 | `lifetempmanager.admin` |
| `/ltm zone info <名前>` | エリア詳細を表示 | `lifetempmanager.admin` |
| `/ltm zone show <名前>` | パーティクルでエリア境界を可視化 | `lifetempmanager.admin` |
| `/ltm zone tp <名前>` | エリア中心へテレポート | `lifetempmanager.admin` |
| `/ltm reload` | `config.yml` と `zones.yml` をリロード | `lifetempmanager.admin` |

---

## ビルド手順

Java 21 環境で以下を実行すると `build/libs/LifeTempManager-1.0.0.jar` が生成されます。

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat clean build
```
