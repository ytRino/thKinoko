package net.nessness.android.thkinoko.misc;

import java.util.ArrayList;
import java.util.List;

import net.nessness.android.thkinoko.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper {

	// query mode to search
    /** 読み検索 */	public static final int QUERY_KEY = 0;
	/** 書き検索 */	public static final int QUERY_VAL = 1;
	/** 連想検索 */	public static final int QUERY_REF = 2;
	/** 作品検索 */	public static final int QUERY_AT = 3;

	// category type constants
	/** キャラクタカテゴリ */ public static final int CAT_CHAR = 0;
	/** スペルカテゴリ     */ public static final int CAT_SPELL = 1;
	/** 音楽カテゴリ       */ public static final int CAT_MUSIC = 2;
	public static final int CAT_ETC = 3; // not used
	public static final int CAT_NUM = 3;
	//public static final int CAT_EX = CAT_NUM+1;

	// ex) dic_spell.db
	private static final String DB_NAME_PREFIX = "dic_";
	private static final String DB_NAME_SUFIX = ".db";
	private static final String DB_TABLE = "dictionary";
	private static final String DB_TABLE_EX = "extra";

	private static final int DB_VERSION = 59;

	public static enum COLS{
		// colName, colNum, colDef
		ID("_id", 0, " INTEGER PRIMARY KEY"),
		VAL("value", 1, " TEXT NOT NULL"),
		KEY("key", 2, " TEXT NOT NULL"),
		EXT("extra", 5, " INTEGER"),
		REF("ref", 4, " TEXT"),
		AT("at", 3, " TEXT"),
		;

		public final String colName;
		public final int colNum;
		private final String colDef;

		private COLS(String name, int num, String def){
			this.colName = name;
			this.colNum = num;
			this.colDef = def;
		}

		@Override
		public String toString(){
			return colName + ", " + colNum;
		}

		public static COLS get(int num){
			if(num == ID.colNum){ return ID;}
			else if(num == KEY.colNum){ return KEY;}
			else if(num == VAL.colNum){ return VAL;}
			else if(num == REF.colNum){ return REF;}
			else if(num == EXT.colNum){ return EXT;}
			else if(num == AT.colNum){ return AT;}
			return KEY;
		}
	}

	private SQLiteDatabase[] mDB;
	private final DBOpenHelper[] mDbOpenHelper;
	//private SQLiteDatabase dbExtra;
	//private final DBOpenHelper dbExtraOpenHelper;

	public static class Word{
		public long id;
		public String value;
		public String key;
		public String ref;
		//public int extra;
		public String at;
		public int category;

		/**
		 * リストビューセパレータ用
		 * @param i
		 */
		public Word(int i) {
			category = i;
		}
		public Word() {
		}
	}

	private static class DBOpenHelper extends SQLiteOpenHelper{

		private static final String DB_CREATE = new StringBuilder()
			.append("CREATE TABLE ")
			.append(DBHelper.DB_TABLE)
			.append(" (")
			.append(COLS.get(0).colName).append(COLS.get(0).colDef)
			.append(", ").append(COLS.get(1).colName).append(COLS.get(1).colDef)
			.append(", ").append(COLS.get(2).colName).append(COLS.get(2).colDef)
			.append(", ").append(COLS.get(3).colName).append(COLS.get(3).colDef)
			.append(", ").append(COLS.get(4).colName).append(COLS.get(4).colDef)
			//.append(", ").append(COLS.get(5).colName).append(COLS.get(5).colDef)
			.append(");")
			.toString();
		/*private static final String DB_CREATE_EX = new StringBuilder()
			.append("CREATE TABLE ")
			.append(DBHelper.DB_TABLE)
			.append(" (")
			.append(" _id").append(" INTEGER PRIMARY KEY").append(", ")
			.append(" extra").append(" TEXT").append(", ")
			.toString();
		*/
		private final Context mContext;
		private final int mCategory;

		/**
		 * コンストラクタ
		 * @param context
		 * @param category
		 * @param version
		 */
		public DBOpenHelper(Context context, int category, int version){
			super(context, "dic_"+category+".db", null, DBHelper.DB_VERSION);
			mCategory = category;
			mContext = context;
		}

		/**
		 * dbがないときに呼ばれる
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			try{
				initialize(db, mContext, mCategory);
			}catch(SQLException e){
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
		 * @param db
		 * @param context
		 * @param category
		 */
		private static void initialize(SQLiteDatabase db, Context context, int category) {
			final int[][] resid = {
					{R.array.char_keys, R.array.char_vals, R.array.char_at, R.array.char_refs},
					{R.array.spell_keys_1, R.array.spell_vals_1, R.array.spell_at_1, 0},
					{R.array.music_keys, R.array.music_vals, R.array.music_at, 0},
					{},
			};
			final int[][] resid2 = {
					{R.array.spell_keys_2, R.array.spell_vals_2, R.array.spell_at_2},
					{R.array.spell_keys_3, R.array.spell_vals_3, R.array.spell_at_3},
					{R.array.spell_keys_4, R.array.spell_vals_4, R.array.spell_at_4},
					{R.array.spell_keys_5, R.array.spell_vals_5, R.array.spell_at_5},
					{R.array.spell_keys_6, R.array.spell_vals_6, R.array.spell_at_6},
                    {R.array.spell_keys_7, R.array.spell_vals_7, R.array.spell_at_7},
                    {R.array.spell_keys_8, R.array.spell_vals_8, R.array.spell_at_8},
			};

			Resources res = context.getResources();
			int count = 0;
			db.beginTransaction();
			try{
				ContentValues v;
				if(category < 3){
					db.execSQL(DB_CREATE);
					Log.d(Constants.TAG, "### "+category);
					String[] keys = res.getStringArray(resid[category][0]);
					String[] values = res.getStringArray(resid[category][1]);
					String[] refs = (resid[category][3]==0)?null:res.getStringArray(resid[category][3]);
					String[] at = res.getStringArray(resid[category][2]);
					int len = values.length;
					for(int i = 0; i < len; i++){
						v = new ContentValues();
						v.put(DBHelper.COLS.KEY.colName, keys[i]);
						v.put(DBHelper.COLS.VAL.colName, values[i]);
						v.put(DBHelper.COLS.REF.colName, (refs==null)?"":refs[i]);
						v.put(DBHelper.COLS.AT.colName, at[i]);
						db.insert(DBHelper.DB_TABLE, null, v);
						count++;
					}
				}
				// スペルは分割されてるのでさらに登録
				if(category == DBHelper.CAT_SPELL){
					for(int j = 0; j < resid2.length; j++){
						//Log.d(Constants.TAG, "### "+category+" spell "+j);
						String[] keys = res.getStringArray(resid2[j][0]);
						String[] values = res.getStringArray(resid2[j][1]);
						String[] at = res.getStringArray(resid2[j][2]);
						//String[] values = res.getStringArray(resid[category][0]);
						int len = values.length;
						for(int i = 0; i < len; i++){
							v = new ContentValues();
							v.put(DBHelper.COLS.KEY.colName, keys[i]);
							v.put(DBHelper.COLS.VAL.colName, values[i]);
							v.put(DBHelper.COLS.AT.colName, at[i]);
							db.insert(DBHelper.DB_TABLE, null, v);
							count++;
						}
					}
				}

				// Extra用
				/*if(category == DBHelper.CAT_EX){
					db.execSQL(DB_CREATE_EX);
					String[] keys = res.getStringArray(resid[category][0]);
					String[] values = res.getStringArray(resid[category][1]);
					int len = keys.length;
					for(int i = 0; i < len; i++){

					}
				}*/

				Log.d(Constants.TAG, "### "+category+"end");
				db.setTransactionSuccessful();
			}finally{
				db.endTransaction();
			}

			Log.d(Constants.TAG, db.getPath()+" : inserted "+count+"items.");
		}
	}

	/**
	 * コンストラクタ
	 * @param context
	 */
	public DBHelper(Context context){
		// TODO 引数検討
		//dbExtraOpenHelper = new DBOpenHelper(context, CAT_EX, DBHelper.DB_VERSION);

		mDbOpenHelper = new DBOpenHelper[CAT_NUM];
		for(int i = 0; i < CAT_NUM; i++){
			mDbOpenHelper[i] = new DBOpenHelper(context, i, DBHelper.DB_VERSION);
		}
		this.establishDB();
	}

	public void establishDB() {
		//dbExtra = this.dbExtraOpenHelper.getWritableDatabase();

		mDB = new SQLiteDatabase[CAT_NUM];
		for(int i = 0; i < CAT_NUM; i++){
			if(this.mDB[i] == null){
				this.mDB[i] = this.mDbOpenHelper[i].getWritableDatabase();
			}
		}
	}

	public void cleanup(){
		for(int i = 0; i < CAT_NUM; i++){
			if(this.mDB[i] != null){
				this.mDB[i].close();
				this.mDB[i] = null;
			}
		}
	}

	public void eraseAllDB(){
		for(int i = 0; i < CAT_NUM; i++){
			mDB[i].delete(DBHelper.DB_TABLE, null, null);
		}
	}

	/*
	private static ContentValues getContentValues(Word word) {
		ContentValues values = new ContentValues();
		values.put(COLS.KEY.colName, word.key);
		values.put(COLS.VAL.colName, word.value);
		values.put(COLS.REF.colName, word.ref);
		values.put(COLS.EXT.colName, word.extra);
		return values;
	}*/


	//private boolean beginTransaction = false;
	/*
	public void useTransaction(boolean transaction){
		if(transaction){
			// トランザクション開始
			beginTransaction = true;
			for(int i = 0; i < CAT_NUM; i++){
				db[i].execSQL("BEGIN");
			}
		}else if(beginTransaction){
			// トランザクション終了
			beginTransaction = false;
			for(int i = 0; i < CAT_NUM; i++){
				db[i].execSQL("COMMIT");
			}
		}
		//開始されてない&&終了フラグ->何もしない
	}*/
	/*
	public void insert(Word word){
		int category = word.category;
		ContentValues values = DBHelper.getContentValues(word);
		this.db[category].insert(DBHelper.DB_TABLE, null, values);
	}

	public void update(Word word){
		int category = word.category;
		ContentValues values = DBHelper.getContentValues(word);
		this.db[category].update(DBHelper.DB_TABLE, values, "_id="+word.id, null);
		Log.d(Constants.TAG, getClass().getSimpleName()+", update.");
	}

	public void delete(long id){
		this.db.delete(DBHelper.DB_TABLE[CAT_CHAR], "_id="+id, null);
	}*/

	public List<Word> getAll(){
		ArrayList<Word> ret = new ArrayList<Word>();
		Cursor c = null;
		try{
			for(int i = 0; i < CAT_NUM; i++){
				c = this.mDB[i].rawQuery("select * from "+DBHelper.DB_TABLE, null);
				int numRows = c.getCount();
				Log.v(Constants.TAG, getClass().getSimpleName()+", query returns "+numRows+" rows.");
				ret.add(new Word(i)); // セパレータ用空データ
				c.moveToFirst();
				for(int j = 0; j < numRows; j++){
					Word w = new Word();
					w.id = c.getLong(COLS.ID.colNum);
					w.value = c.getString(COLS.VAL.colNum);
					w.key = c.getString(COLS.KEY.colNum);
					w.ref = c.getString(COLS.REF.colNum);
					w.at = c.getString(COLS.AT.colNum);
					w.category = i;
					ret.add(w);
					c.moveToNext();
				}
			}
		}catch(SQLException e){
			Log.e(Constants.TAG, getClass().getSimpleName(), e);
		}finally{
			if(c != null && !c.isClosed()){
				c.close();
			}
		}
		return ret;
	}

	public List<Word> query(CharSequence key, int queryMode, boolean[] categories){
		ArrayList<Word> ret = new ArrayList<Word>();
		String query = null;
		Cursor c = null;

		// モードに従って検索文をつくる
		switch(queryMode){
		case QUERY_VAL:
			query = "select * from "+DBHelper.DB_TABLE+" where "+COLS.VAL.colName+" like '%"+key+"%';";
			break;
		case QUERY_REF: // どうする
			query = "select * from "+DBHelper.DB_TABLE+" where "+COLS.VAL.colName+" || "+COLS.REF.colName+" like '%"+key+"%';";
			break;
		case QUERY_AT:
			query = "select * from "+DBHelper.DB_TABLE+" where "+COLS.AT.colName+" like '%"+key+"%';";
			break;
		case QUERY_KEY:
		default:
			query = "select * from "+DBHelper.DB_TABLE+" where "+COLS.KEY.colName+" like '%"+key +"%';";
		}

		// カテゴリごとにDBを検索
		try{
			for(int category = 0; category < CAT_NUM; category++){
			    // チェックを外したカテゴリは検索しない
				if(!categories[category]){
					continue;
				}

				// 09-01 11:25:17.423: ERROR/Cursor(436): android.database.sqlite.DatabaseObjectNotClosedException:
				// Application did not close the cursor or database object that was opened here
				c = this.mDB[category].rawQuery(query, null);
				if(!c.moveToFirst()){
					continue;
				}

				// セパレータ用空データをつける
				ret.add(new Word(category));

				int len = c.getCount();
				Word w;
				for(int j = 0; j < len; j++){
					w = new Word();
					w.id = c.getLong(COLS.ID.colNum);
					w.key = c.getString(COLS.KEY.colNum);
					w.value = c.getString(COLS.VAL.colNum);
					w.at = c.getString(COLS.AT.colNum);
					//w.extra = c.getInt(COLS.EXT.colNum);
					w.category = category;
					ret.add(w);
					c.moveToNext();
				}
			}
		}catch(SQLException e){
			Log.e(Constants.TAG, getClass().getSimpleName(), e);
		}finally{
			if(c != null && !c.isClosed()){
				c.close();
			}
		}
		return ret;
	}
}
