package net.nessness.android.thkinoko.misc;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.nessness.android.thkinoko.R;

import java.util.ArrayList;
import java.util.List;

public class DBHelper {

    // query mode to search
    /** 読み検索 */
    public static final int QUERY_KEY = 0;
    /** 書き検索 */
    public static final int QUERY_VAL = 1;
    /** 連想検索 */
    public static final int QUERY_REF = 2;
    /** 作品指定検索 */
    public static final int QUERY_AT = 3;
    /** キャラ指定検索 */
    public static final int QUERY_CHA = 4;

    // category type constants
    /** キャラクタカテゴリ */
    public static final int CAT_CHAR = 0;
    /** スペルカテゴリ */
    public static final int CAT_SPELL = 1;
    /** 音楽カテゴリ */
    public static final int CAT_MUSIC = 2;
    //public static final int CAT_ETC = 3; // not used
    public static final int CAT_NUM = 3;
    //public static final int CAT_EX = CAT_NUM+1;

    // ex) dic_spell.db
    private static final String DB_NAME_PREFIX = "dic_";
    private static final String DB_NAME_SUFFIX = ".db";
    private static final String DB_TABLE = "dictionary";

    private static final int DB_VERSION = 59;

    public static enum COLS {
        // colName, colNum, colDef
        ID("_id", 0, " INTEGER PRIMARY KEY"),
        VAL("value", 1, " TEXT NOT NULL"),
        KEY("key", 2, " TEXT NOT NULL"),
        CHA("cha", 4, " TEXT"),
        REF("ref", 5, " TEXT"),
        AT("at", 3, " TEXT"),;

        public final String colName;
        @Deprecated
        public final int colNum;
        private final String colDef;

        private COLS(String name, int num, String def) {
            this.colName = name;
            this.colNum = num;
            this.colDef = def;
        }

        @Override
        public String toString() {
            return colName + ", " + colNum;
        }

    }

    private SQLiteDatabase[] mDB;
    private final DBOpenHelper[] mDbOpenHelper;

    /**
     * コンストラクタ
     *
     * @param context
     */
    public DBHelper(Context context) {
        // TODO 引数検討
        //dbExtraOpenHelper = new DBOpenHelper(context, CAT_EX, DBHelper.DB_VERSION);

        mDbOpenHelper = new DBOpenHelper[CAT_NUM];
        for (int i = 0; i < CAT_NUM; i++) {
            mDbOpenHelper[i] = new DBOpenHelper(context, i, DBHelper.DB_VERSION);
        }
        this.establishDB();
    }

    public void establishDB() {

        mDB = new SQLiteDatabase[CAT_NUM];
        for (int i = 0; i < CAT_NUM; i++) {
            if (this.mDB[i] == null) {
                this.mDB[i] = this.mDbOpenHelper[i].getWritableDatabase();
            }
        }
    }

    public void cleanup() {
        for (int i = 0; i < CAT_NUM; i++) {
            if (this.mDB[i] != null) {
                this.mDB[i].close();
                this.mDB[i] = null;
            }
        }
    }

    /**
     * dbの全データを取得
     *
     * @return
     */
    public List<Word> getAll() {
        ArrayList<Word> ret = new ArrayList<Word>();
        Cursor c = null;
        try {
            for (int i = 0; i < CAT_NUM; i++) {
                c = this.mDB[i].rawQuery("select * from " + DBHelper.DB_TABLE, null);
                if (! c.moveToFirst()) {
                    continue;
                }

                // セパレータ用空データをつける
                ret.add(new Word(i));
                int len = c.getCount();
                for (int j = 0; j < len; j++) {
                    Word w = WordFactory.fromCursor(c, i);
                    ret.add(w);
                    c.moveToNext();
                }
            }
        } catch (SQLException e) {
            Log.e(Constants.TAG, getClass().getSimpleName(), e);
        } finally {
            if (c != null && ! c.isClosed()) {
                c.close();
            }
        }
        return ret;
    }

    public List<Word> query(CharSequence key, int queryMode, boolean[] categories) {
        ArrayList<Word> ret = new ArrayList<Word>();
        String query;
        Cursor c = null;

        // モードに従って検索文をつくる
        switch (queryMode) {
        case QUERY_VAL:
            query = "select * from " + DBHelper.DB_TABLE + " where " + COLS.VAL.colName + " like '%" + key + "%';";
            break;
        case QUERY_REF: // どうする
            query = "select * from " + DBHelper.DB_TABLE + " where " + COLS.VAL.colName + " || " + COLS.REF.colName + " like '%" + key + "%';";
            break;
        case QUERY_AT:
            query = "select * from " + DBHelper.DB_TABLE + " where " + COLS.AT.colName + " like '%" + key + "%';";
            break;
        case QUERY_CHA:
            query = "select * from " + DBHelper.DB_TABLE + " where " + COLS.CHA.colName + " like '%" + key + "%';";
            break;
        case QUERY_KEY:
        default:
            query = "select * from " + DBHelper.DB_TABLE + " where " + COLS.KEY.colName + " like '%" + key + "%';";
        }

        // カテゴリごとにDBを検索
        try {
            for (int category = 0; category < CAT_NUM; category++) {
                // チェックを外したカテゴリは検索しない
                if (! categories[category]) {
                    continue;
                }

                // 09-01 11:25:17.423: ERROR/Cursor(436): android.database.sqlite.DatabaseObjectNotClosedException:
                // Application did not close the cursor or database object that was opened here
                c = this.mDB[category].rawQuery(query, null);
                if (! c.moveToFirst()) {
                    continue;
                }

                // セパレータ用空データをつける
                ret.add(new Word(category));

                int len = c.getCount();
                Word w;
                for (int j = 0; j < len; j++) {
                    w = WordFactory.fromCursor(c, category);
                    ret.add(w);
                    c.moveToNext();
                }
            }
        } catch (SQLException e) {
            Log.e(Constants.TAG, getClass().getSimpleName(), e);
        } finally {
            if (c != null && ! c.isClosed()) {
                c.close();
            }
        }
        return ret;
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {

        private static final String DB_CREATE = new StringBuilder()
                .append("CREATE TABLE ")
                .append(DBHelper.DB_TABLE)
                .append(" (")
                .append(COLS.ID.colName).append(COLS.ID.colDef)
                .append(", ").append(COLS.VAL.colName).append(COLS.VAL.colDef)
                .append(", ").append(COLS.KEY.colName).append(COLS.KEY.colDef)
                .append(", ").append(COLS.AT.colName).append(COLS.AT.colDef)
                .append(", ").append(COLS.CHA.colName).append(COLS.CHA.colDef)
                .append(", ").append(COLS.REF.colName).append(COLS.REF.colDef)
                .append(");")
                .toString();

        private final Context mContext;
        private final int mCategory;

        /**
         * コンストラクタ
         *
         * @param context
         * @param category
         * @param version
         */
        public DBOpenHelper(Context context, int category, int version) {
            super(context, DBHelper.DB_NAME_PREFIX + category + DBHelper.DB_NAME_SUFFIX, null, DBHelper.DB_VERSION);
            mCategory = category;
            mContext = context;
        }

        /**
         * dbがないときに呼ばれる
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                initialize(db, mContext, mCategory);
            } catch (SQLException e) {
                Log.e(Constants.TAG, getClass().getSimpleName(), e);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            super.onOpen(db);
        }

        /**
         * dbのバージョンが上がったときに呼ばれる
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DBHelper.DB_TABLE);
            this.onCreate(db);
        }

        /**
         * DB初期化 staticメソッド
         *
         * @param db
         * @param context
         * @param category
         */
        private static void initialize(SQLiteDatabase db, Context context, int category) {
            final int[][] resid = {
                    {R.array.char_keys, R.array.char_vals, R.array.char_refs},
                    {R.array.spell_keys_1, R.array.spell_val_1, 0},
                    {R.array.music_keys, R.array.music_vals, 0},
                    {},
            };
            final int[][] resid2 = {
                    {R.array.spell_keys_2, R.array.spell_val_2},
                    {R.array.spell_keys_3, R.array.spell_val_3},
                    {R.array.spell_keys_4, R.array.spell_val_4},
                    {R.array.spell_keys_5, R.array.spell_val_5},
                    {R.array.spell_keys_6, R.array.spell_val_6},
                    {R.array.spell_keys_7, R.array.spell_val_7},
                    {R.array.spell_keys_8, R.array.spell_val_8},
            };

            Resources res = context.getResources();
            int count = 0;
            db.beginTransaction();
            try {
                if (category < DBHelper.CAT_NUM) {
                    db.execSQL(DB_CREATE);
                    Log.d(Constants.TAG, "### " + category);
                    String[] keys = res.getStringArray(resid[category][0]);
                    String[] values = res.getStringArray(resid[category][1]);
                    String[] refs = (resid[category][2] == 0) ? null : res.getStringArray(resid[category][2]);
                    int len = values.length;
                    for (int i = 0; i < len; i++) {
                        ContentValues v = createRow(values[i], keys[i], (refs == null) ? "" : refs[i]);
                        db.insert(DBHelper.DB_TABLE, null, v);
                        count++;
                    }
                }
                // スペルは分割されてるのでさらに登録
                if (category == DBHelper.CAT_SPELL) {
                    for (int j = 0; j < resid2.length; j++) {
                        //Log.d(Constants.TAG, "### "+category+" spell "+j);
                        String[] keys = res.getStringArray(resid2[j][0]);
                        String[] values = res.getStringArray(resid2[j][1]);
                        int len = values.length;
                        for (int i = 0; i < len; i++) {
                            ContentValues v = createRow(values[i], keys[i]);
                            db.insert(DBHelper.DB_TABLE, null, v);
                            count++;
                        }
                    }
                }
                Log.d(Constants.TAG, "### " + category + "end");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Log.d(Constants.TAG, db.getPath() + " : inserted " + count + "items.");
        }

        /**
         * create row for db from resource
         *
         * @param val
         * @param key
         * @return
         */
        private static ContentValues createRow(String val, String key) {
            // 十六夜 咲夜,紅妖永萃花文緋非輝,0605
            String[] value = val.split(",");
            if (value.length < 2) {
                Log.w(Constants.TAG, value.length + "@" + val);
                return null;
            }
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.COLS.KEY.colName, key);
            cv.put(DBHelper.COLS.VAL.colName, value[0]);
            cv.put(DBHelper.COLS.AT.colName, value[1]);
            if (value.length >= 3) {
                cv.put(DBHelper.COLS.CHA.colName, value[2]);
            }
            return cv;
        }

        /**
         * create row for db from resource
         *
         * @param val
         * @param ref
         * @return
         */
        private static ContentValues createRow(String val, String key, String ref) {
            ContentValues cv = createRow(val, key);
            cv.put(DBHelper.COLS.REF.colName, ref);
            return cv;
        }
    }
}
