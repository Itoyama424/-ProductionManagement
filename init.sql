-- DB作成
CREATE DATABASE productionmanagement;

\c productionmanagement;

create table public.item_master (
  item_id character varying(20) not null
  , item_name character varying(100) not null
  , item_type integer not null
  , unit character varying(10)
  , primary key (item_id)
);

create table public.bom_master (
  parent_id character varying(20) not null
  , child_id character varying(20) not null
  , quantity numeric(10, 3) not null
  , primary key (parent_id, child_id)
);

CREATE TABLE public.bom_structure (
    parent_item_id CHARACTER VARYING(20) NOT NULL, -- 親（作るもの）
    child_item_id CHARACTER VARYING(20) NOT NULL,  -- 子（使うもの）
    quantity DECIMAL(10, 2) NOT NULL,              -- 使う数
    lead_time INTEGER NOT NULL,                    -- これ！「リードタイム（日）」
    PRIMARY KEY (parent_item_id, child_item_id),
    -- 親も子も、item_masterに登録されているIDじゃないとダメという紐付け
    FOREIGN KEY (parent_item_id) REFERENCES public.item_master(item_id),
    FOREIGN KEY (child_item_id) REFERENCES public.item_master(item_id)
);

-- 在庫マスタ（有効在庫管理版）
CREATE TABLE public.stock_master (
    item_id            character varying(20)  NOT NULL, -- 品目ID
    stock_quantity     numeric(18, 3)         NOT NULL, -- 実在庫（倉庫にある現物総数）
    defective_quantity numeric(18, 3)         NOT NULL DEFAULT 0.000, -- 不良在庫数
    hold_quantity      numeric(18, 3)         NOT NULL DEFAULT 0.000, -- 保留在庫数（手がかり）
    
    CONSTRAINT stock_master_pkey PRIMARY KEY (item_id)
);

-- BOM展開計算結果：作業用一時テーブル
CREATE TABLE public.bom_calc_result (
    -- 計算実行ごとに発行する一意識別子（UUIDを想定）
    calc_id            character varying(36)  NOT NULL, 
    -- 品目ID：item_master(20)と完全に一致させる
    item_id            character varying(20)  NOT NULL, 
    -- 展開の起点（完成品）ID
    top_parent_id      character varying(20)  NOT NULL, 
    -- 総必要数：bom_master(10,3)より余裕を持たせ、stock_master(18,3)に合わせる
    gross_qty          numeric(38, 3)         NOT NULL DEFAULT 0.000, 
    -- 有効在庫：stock_master(18,3)と完全に一致
    available_stock    numeric(38, 3)         NOT NULL DEFAULT 0.000, 
    -- 不足数（Net）：計算結果の精度を担保
    net_qty            numeric(38, 3)         NOT NULL DEFAULT 0.000, 
    -- レコード作成日時
    created_at         timestamp              NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 計算IDと品目IDの組み合わせで主キー（LazyLoadingの検索キー）
    CONSTRAINT pk_bom_calc_result PRIMARY KEY (calc_id, item_id),
    -- 外部キー制約（必要に応じて。パフォーマンス優先なら外す選択肢もあり）
    CONSTRAINT fk_bom_calc_result_item FOREIGN KEY (item_id) REFERENCES public.item_master(item_id)
);

-- LazyLoading（＋ボタン）時の子部品取得用インデックス
-- WHERE calc_id = ? AND item_id = ? のクエリを最速にする
CREATE INDEX idx_bom_calc_lookup ON public.bom_calc_result (calc_id, item_id);

-- テーブル/カラムコメント
COMMENT ON TABLE  public.bom_calc_result IS 'BOM展開計算結果スナップショット：LazyLoading表示用';
COMMENT ON COLUMN public.bom_calc_result.calc_id IS '一回の一括計算を識別するUUID';
COMMENT ON COLUMN public.bom_calc_result.net_qty IS '在庫を引いた後の最終的な不足数';


-- カラムへの説明（コメント）
COMMENT ON TABLE  public.stock_master                    IS '在庫マスタ：実在庫・不良・保留を区分管理';
COMMENT ON COLUMN public.stock_master.stock_quantity     IS '倉庫内の物理的な総在庫数';
COMMENT ON COLUMN public.stock_master.defective_quantity IS '品質不良等の理由で使用不可能な在庫数';
COMMENT ON COLUMN public.stock_master.hold_quantity      IS '検査待ち・保留中（手がかり）の在庫数';

-- 一旦クリア（整合性のため）
DELETE FROM public.bom_structure;
DELETE FROM public.bom_master;
DELETE FROM public.stock_master;
DELETE FROM public.item_master;

-- 1. item_master (品目マスタ)
-- type1:完成品, type2:アッセンブリ, type3:単体部品・材料
INSERT INTO public.item_master (item_id, item_name, item_type, unit) VALUES
('EV-PRO-01', '次世代EVプロトタイプ', 1, '台'),
-- Level 1
('BAT-PK-01', '大容量バッテリーパック', 2, '個'),
('DRV-UN-01', '高出力ドライブユニット', 2, '基'),
-- Level 2
('BAT-MD-01', 'リチウムイオンモジュール', 2, '個'),
('MOT-AS-01', '主駆動モータASSY', 2, '個'),
('INV-AS-01', 'パワーインバータASSY', 2, '個'),
-- Level 3
('CEL-LT-01', '高性能バッテリーセル', 2, '個'),
('STT-CO-01', 'ステータ・コイル', 2, '個'),
('PCB-CN-01', '制御基板基幹ユニット', 2, '枚'),
-- Level 4
('ANO-MT-01', 'アノード電極材', 3, 'kg'),
('CPR-WI-01', '高純度銅ワイヤ', 3, 'm'),
('SEM-CH-01', 'パワー半導体チップ', 3, '個'),
-- Level 5 (最深部：原材料)
('RAW-LIT-01', '精製リチウム化合物', 3, 'kg'),
('RAW-CPR-01', '粗銅材', 3, 'kg'),
('RAW-SIL-01', '高純度シリコン', 3, 'kg'),
-- 共通汎用部品
('SCR-M4-01', '精密ネジ(M4)', 3, '本');

-- 2. stock_master (在庫マスタ)
-- 既存データのクリア
TRUNCATE TABLE public.stock_master;

-- 投入データ内容の定義
-- item_id, stock_quantity (実在庫), defective_quantity (不良), hold_quantity (保留/手がかり)
INSERT INTO public.stock_master (item_id, stock_quantity, defective_quantity, hold_quantity) VALUES

-- 1. 在庫が半分あり、不足分のみ展開されるケース（有効在庫: 2）
('BAT-PK-01', 2.000, 0.000, 0.000),

-- 2. 【重要】実在庫はあるが、不良・保留（手がかり）のため「有効在庫」が激減しているケース
-- 実 50.0 - 不良 30.0 - 保留 10.0 = 有効 10.0（画像上の在庫 10個 の根拠をシミュレーション）
('BAT-MD-01', 50.000, 30.000, 10.000),

-- 3. 実在庫はあるが、すべて「保留（手がかり）」のため、有効在庫がゼロのケース
-- これにより、その下の層（ANO-MT-01 等）が全数展開される
('CEL-LT-01', 500.000, 0.000, 500.000),

-- 4. 在庫なし（DRV-UN-01：高出力ドライブユニットは 1個 だけ現物がある設定）
('DRV-UN-01', 1.000, 0.000, 0.000),

-- 5. 【停止条件】在庫が十分にあるため、これ以下の展開を止めるべきケース
-- MOT-AS-01（主駆動モータ）が 5個 あれば、指示数 4 に対して充足するため、STT-CO-01 以下の展開は不要
('MOT-AS-01', 5.000, 0.000, 0.000),

-- 6. 同様に、在庫が潤沢なネジ類
('SCR-M4-01', 5000.000, 0.000, 0.000),

-- 7. 銅材などの原材料（末端の計算結果確認用）
('RAW-CPR-01', 200.000, 0.000, 0.000),
('RAW-LIT-01', 50.000, 0.000, 0.000),
('RAW-SIL-01', 10.000, 0.000, 0.000);

-- 3. bom_master & bom_structure (構成とリードタイム)
-- 階層を深く(5段階)連結させます
INSERT INTO public.bom_master (parent_id, child_id, quantity) VALUES
('EV-PRO-01', 'BAT-PK-01', 1.000),
('EV-PRO-01', 'DRV-UN-01', 1.000),
('BAT-PK-01', 'BAT-MD-01', 12.000), -- パックの中に12モジュール
('DRV-UN-01', 'MOT-AS-01', 1.000),
('DRV-UN-01', 'INV-AS-01', 1.000),
('BAT-MD-01', 'CEL-LT-01', 20.000), -- モジュールの中に20セル
('MOT-AS-01', 'STT-CO-01', 1.000),
('INV-AS-01', 'PCB-CN-01', 1.000),
('CEL-LT-01', 'ANO-MT-01', 0.500),  -- セル1個に0.5kgの電極
('STT-CO-01', 'CPR-WI-01', 50.000), -- ステータ1個に50mの銅線
('PCB-CN-01', 'SEM-CH-01', 8.000),   -- 基板1枚に8個のチップ
('ANO-MT-01', 'RAW-LIT-01', 0.200), -- 電極1kgに0.2kgのリチウム (Level 5)
('CPR-WI-01', 'RAW-CPR-01', 1.100), -- 銅線1mに1.1kgの粗銅 (Level 5)
('SEM-CH-01', 'RAW-SIL-01', 0.010), -- チップ1個に0.01kgのシリコン (Level 5)
('BAT-PK-01', 'SCR-M4-01', 48.000), -- 汎用ネジ
('INV-AS-01', 'SCR-M4-01', 12.000);
('RAW-LIT-01', 'BAT-MD-01', 1.000)
-- bom_structure (リードタイム付き)
INSERT INTO public.bom_structure (parent_item_id, child_item_id, quantity, lead_time) VALUES
('EV-PRO-01', 'BAT-PK-01', 1.00, 10),
('EV-PRO-01', 'DRV-UN-01', 1.00, 14),
('BAT-PK-01', 'BAT-MD-01', 12.00, 5),
('DRV-UN-01', 'MOT-AS-01', 1.00, 7),
('DRV-UN-01', 'INV-AS-01', 1.00, 7),
('BAT-MD-01', 'CEL-LT-01', 20.00, 3),
('MOT-AS-01', 'STT-CO-01', 1.00, 4),
('INV-AS-01', 'PCB-CN-01', 1.00, 10),
('CEL-LT-01', 'ANO-MT-01', 0.50, 20),
('STT-CO-01', 'CPR-WI-01', 50.00, 2),
('PCB-CN-01', 'SEM-CH-01', 8.00, 30),
('ANO-MT-01', 'RAW-LIT-01', 0.20, 60), -- 原材料は調達が長い
('CPR-WI-01', 'RAW-CPR-01', 1.10, 15),
('SEM-CH-01', 'RAW-SIL-01', 0.01, 90),
('BAT-PK-01', 'SCR-M4-01', 48.00, 1),
('INV-AS-01', 'SCR-M4-01', 12.00, 1);
---------------------------------------------------------------------------------------
-- DB作成
