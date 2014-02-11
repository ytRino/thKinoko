package net.nessness.android.thkinoko;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.nessness.android.thkinoko.misc.Constants;
import net.nessness.android.thkinoko.misc.DBHelper;
import net.nessness.android.thkinoko.misc.Word;
import net.nessness.android.thkinoko.misc.WordAdapter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class KinokoActivity extends Activity{
    public static final String SIMEJI_KEY = "replace_key";
    public static final String SIMEJI_ACTION = "com.adamrocker.android.simeji.ACTION_INTERCEPT";

    private static final String PREF_NAME = "setting";
    private static final String PREF_USE_KEY = "search_use_key";
    private static final String PREF_USE_REF = "search_use_ref";
    private static final String PREF_CAT = "category_";

    private static final String STATE_QUERY_KEY = "state_query_key";
    private static final String STATE_QUERY_MODE = "state_query_mode";
    private static final String STATE_CAT_FILTER = "state_cat_filter";

    private static final int CMENU_WHO = 0;
    private static final int CMENU_AT = 1;

    private static final int MODE_LAUNCHER = 0;
    private static final int MODE_MUSHROOM = 1;
    private int mLaunchMode;

    private ListView mDicList;
    private EditText mKeywordBox;
    private ImageButton mSearchButton;
    private ImageButton mCatButton;

    private RadioGroup mSearchOptGroup;  // key or value
    private RadioButton mKeySearchOpt;
    private RadioButton mValSearchOpt;
    private CheckBox mRefSearchOpt;  // use ref when search by value

    private DBHelper mDbHelper;
    private WordAdapter mWordAdapter;
    private SharedPreferences mPref;

    private boolean[] mCatFilter;
    private CharSequence mLastQueryKey;
    private int mLastQueryMode;

    private class DBTask extends AsyncTask<Void, Void, Void>{
        private AlertDialog dialog;

        @Override
        protected void onPreExecute(){
            // Log.d(Constants.TAG,
            // getClass().getSimpleName()+", # start DBTask.");
            View v = getLayoutInflater().inflate(R.layout.dialog_progress, null);
            dialog = createDialogBuilderCompat()
                    .setCancelable(false)
                    .setView(v)
                    .create();
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0){
            mDbHelper = new DBHelper(KinokoActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(dialog == null || ! dialog.isShowing()){
                return;
            }
            dialog.dismiss();
            // Log.d(Constants.TAG,
            // getClass().getSimpleName()+", # end DBTask.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // Log.v(Constants.TAG, "###onCreate.");

        // Log.d(Constants.TAG,
        // getClass().getSimpleName()+", onCreate called.");

        getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.knk_main);

        new DBTask().execute();

        mPref = getSharedPreferences(PREF_NAME, 0);

        mLastQueryKey = "";
        mLastQueryMode = 0;

        mCatFilter = new boolean[DBHelper.CAT_NUM];
        for(int i = 0; i < DBHelper.CAT_NUM; i++){
            mCatFilter[i] = mPref.getBoolean(PREF_CAT + i, true);
        }

        mDicList = (ListView) findViewById(R.id.knk_dictionary);
        mKeywordBox = (EditText) findViewById(R.id.knk_keyword);
        mSearchButton = (ImageButton) findViewById(R.id.knk_search);
        mCatButton = (ImageButton) findViewById(R.id.knk_category);
        mSearchOptGroup = (RadioGroup) findViewById(R.id.knk_search_opt);
        mKeySearchOpt = (RadioButton) findViewById(R.id.knk_search_key);
        mValSearchOpt = (RadioButton) findViewById(R.id.knk_search_val);
        mRefSearchOpt = (CheckBox) findViewById(R.id.knk_search_val_ref);

        this.initWidgets();

        this.handleIntent();
    }

    @Override
    protected void onStart(){
        super.onStart();
        // Log.v(Constants.TAG, "###onSrart.");

        // Preferences から読みだす
        switch(mLaunchMode){
        case MODE_LAUNCHER:
            mRefSearchOpt.setChecked(mPref.getBoolean(PREF_USE_REF, false));
            if(mPref.getBoolean(PREF_USE_KEY, true)){
                mKeySearchOpt.setChecked(true);
            }else{
                mValSearchOpt.setChecked(true);
            }
            break;
        case MODE_MUSHROOM:
            mKeySearchOpt.setChecked(true);
        }

        // カテゴリフィルター
        for(int i = 0; i < DBHelper.CAT_NUM; i++){
            mCatFilter[i] = mPref.getBoolean(PREF_CAT + i, true);
        }

    }

    @Override
    protected void onResume(){
        super.onResume();
        // Log.v(Constants.TAG, "###onResumessss.");
        if(mWordAdapter != null){
            // 背景表示設定のチェックとリスト再描画
            mWordAdapter.updatePrefs(Constants.PREF_USE_IMG);
            mDicList.invalidateViews();
        }
        refleshSearch();
    }

    @Override
    protected void onPause(){
        super.onPause();
        // Log.v(Constants.TAG, "###onPause.");

        // Preferences の保存
        SharedPreferences.Editor editor = mPref.edit();
        switch(mLaunchMode){
        case MODE_LAUNCHER:
            editor.putBoolean(PREF_USE_KEY, mKeySearchOpt.isChecked());
            editor.putBoolean(PREF_USE_REF, mRefSearchOpt.isChecked());
            for(int i = 0; i < DBHelper.CAT_NUM; i++){
                editor.putBoolean(PREF_CAT + i, mCatFilter[i]);
            }
            break;
        case MODE_MUSHROOM:
        }

        for(int i = 0; i < DBHelper.CAT_NUM; i++){
            editor.putBoolean(PREF_CAT + i, mCatFilter[i]);
        }

        editor.commit();
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Log.v(Constants.TAG, "###onStop.");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        // Log.v(Constants.TAG, "###onSaveInstanceState.");

        outState.putCharSequence(STATE_QUERY_KEY, mLastQueryKey);
        outState.putInt(STATE_QUERY_MODE, mLastQueryMode);
        outState.putBooleanArray(STATE_CAT_FILTER, mCatFilter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        // Log.v(Constants.TAG,
        // "###onRestoreInstanceState."+savedInstanceState.getCharSequence(STATE_QUERY_KEY));

        mLastQueryKey = savedInstanceState.getCharSequence(STATE_QUERY_KEY);
        mLastQueryMode = savedInstanceState.getInt(STATE_QUERY_MODE);
        mCatFilter = savedInstanceState.getBooleanArray(STATE_CAT_FILTER);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // Log.v(Constants.TAG, "###onDestroy.");
        if(mDbHelper != null){
            mDbHelper.cleanup();
            mDbHelper = null;
        }
    }

    private void handleIntent(){
        // Log.v(Constants.TAG, "###handleIntent.");
        Intent intent = getIntent();
        String action = intent.getAction();

        // マッシュルームから起動
        if(action != null && action.equals(SIMEJI_ACTION)){
            mLaunchMode = MODE_MUSHROOM;
            String key = intent.getStringExtra(SIMEJI_KEY);
            mKeywordBox.setText(key);
            this.search(key);
        }
        // 普通に起動
        else{
            mLaunchMode = MODE_LAUNCHER;
            search(mLastQueryKey);
        }
    }

    /**
     * 再度検索
     */
    private void refleshSearch(){
        // Log.v(Constants.TAG, "###refleshSearch.");
        search(mLastQueryKey, mLastQueryMode);
    }

    /**
     * 検索
     *
     * @param key 検索キーワード
     */
    private void search(final CharSequence key){
        // Log.v(Constants.TAG, "###search(key).");
        search(key, getQueryMode());
    }

    /**
     * 検索
     *
     * @param key 検索キーワード
     * @param queryMode 検索モード
     */
    private void search(final CharSequence key, final int queryMode){
        // Log.v(Constants.TAG, "###search("+key+", "+ queryMode+").");
        // getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mKeywordBox.getWindowToken(), 0);

        mLastQueryKey = key;
        mLastQueryMode = queryMode;

        // TODO: これ大丈夫?
        final Handler mHandler = new Handler();
        mHandler.post(new Runnable(){
            public void run(){
                if(mDbHelper == null){
                    // Log.d(Constants.TAG,
                    // getClass().getSimpleName()+", wordAdapter is null.");
                    mHandler.postDelayed(this, 300);
                }else{
                    ArrayList<Word> w = new ArrayList<Word>();
                    w.addAll(mDbHelper.query(key, queryMode, mCatFilter));
                    mWordAdapter = new WordAdapter(KinokoActivity.this, w);
                    mDicList.setAdapter(mWordAdapter);
                }
            }
        });
    }

    /**
     * 検索モードの取得
     *
     * @return DBHelper.QUERY_*
     */
    private int getQueryMode(){
        if(mValSearchOpt.isChecked()){
            if(mRefSearchOpt.isChecked()){
                return DBHelper.QUERY_REF;
            }
            return DBHelper.QUERY_VAL;
        }
        return DBHelper.QUERY_KEY;
    }


    private String getValue(AdapterView<?> adapter, int position){
        return ((Word) adapter.getItemAtPosition(position)).value;
    }

    /**
     * メニュー
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.knk_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
        case R.id.knk_menu_about:
            // Log.v(Constants.TAG,
            // getLocalClassName()+", menu: "+item.getTitle());
            this.createMessageDialog(R.string.dialog_about_title,
                    R.string.dialog_about_content)
                    .create()
                    .show();
            break;
        case R.id.knk_menu_setting:
            startActivity(new Intent(this, SettingActivity.class));
            break;
        case R.id.knk_menu_help:
            this.createMessageDialog(R.string.dialog_help_title,
                    R.string.dialog_help_content)
                    .create()
                    .show();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);

        Word w = (Word) mDicList.getItemAtPosition(((AdapterContextMenuInfo) menuInfo).position);

        if((w == null) || w.at.length() < 1){
            return;
        }

        // ItemIdにキャラクター識別番号を設定
        // TODO: 具体的なキャラ名にしたほうがわかりやすい
        // 変換テーブル作るのは微妙だけどDBから引っ張るのは大げさっぽい
        String character = w.getPrimaryCharacter();
        if(character != null){
            menu.add(CMENU_WHO, Integer.parseInt(character), 0, R.string.cmenu_char);
        }

        char at[] = w.at.toCharArray();
        int len = at.length;
        for(int i = 0; i < len; i++){
            // TODO: 萃 -> 萃夢想関連の語句を見る みたいな感じにする?
            menu.add(CMENU_AT, 0, 0, String.valueOf(at[i]));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        int id = item.getItemId();
        mKeywordBox.setText("");
        if(id > 0)
            search(String.format("%04d", id), DBHelper.QUERY_CHA);     // キャラクター(元の4桁に戻す)
        else
            search(item.getTitle(), DBHelper.QUERY_AT); // 作品
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        // TODO 自動生成されたメソッド・スタブ
        super.onConfigurationChanged(newConfig);
        // Log.v(Constants.TAG, "###onConfigChanged.");
    }

    /**
     * widgetの初期化
     */
    private void initWidgets(){
        // Log.v(Constants.TAG, "###initWidget.");

        mDicList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id){
                String result = getValue(parent, position);
                switch(mLaunchMode){
                case MODE_MUSHROOM:
                    // Log.d("thKinoko",
                    // parent.getItemAtPosition(position).toString()); // Map
                    Intent i = new Intent();
                    i.putExtra(SIMEJI_KEY, result);
                    setResult(RESULT_OK, i);
                    break;
                case MODE_LAUNCHER:
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setText(result);
                    Toast.makeText(KinokoActivity.this,
                            "クリップボードに\"" + result + "\"をコピーしました",
                            Toast.LENGTH_LONG).show();
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException e){
                        // nop
                    }
                    break;
                }

                finish();
            }
        });

        registerForContextMenu(mDicList);

        mKeywordBox.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(event == null || event.getAction() == KeyEvent.ACTION_UP){
                    search(v.getText().toString());
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return true;
            }
        });
        mKeywordBox.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(! hasFocus){
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(
                                    mKeywordBox.getWindowToken(), 0);
                }
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                search(mKeywordBox.getText().toString());
            }
        });

        mSearchOptGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId){
                switch(checkedId){
                case R.id.knk_search_key:
                    mRefSearchOpt.setEnabled(false);
                    break;
                case R.id.knk_search_val:
                    mRefSearchOpt.setEnabled(true);
                    break;
                }
            }
        });
        mRefSearchOpt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked){

            }
        });

        final AlertDialog catDialog = createDialogBuilderCompat()
                .setMultiChoiceItems(R.array.cateories, mCatFilter,
                        new DialogInterface.OnMultiChoiceClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which, boolean isChecked){
                                mCatFilter[which] = isChecked;
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener(){
                    @Override
                    public void onCancel(DialogInterface dialog){
                        // Log.d(Constants.TAG, "refresh");
                        refleshSearch();
                    }
                })
                .create();
        mCatButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // カテゴリ選択ダイアログ表示
                // catChooser.show();
                catDialog.show();
                // Log.d(Constants.TAG,
                // ""+catFilter[0]+catFilter[1]+catFilter[2]+catFilter[3]);
            }
        });
    }

    /**
     * @param titleResId title
     * @param msgResId msg
     * @return タイトルとメッセージを設定したBuilder
     */
    private AlertDialog.Builder createMessageDialog(int titleResId, int msgResId){
        AlertDialog.Builder builder = createDialogBuilderCompat();
        if(titleResId != 0){
            builder.setTitle(titleResId);
        }
        if(msgResId != 0){
            builder.setMessage(msgResId);
        }
        return builder;
    }

    private static final Class[] mBuilderConstructorSignature =
            new Class[]{
                    Context.class, int.class
            };
    private Constructor<AlertDialog.Builder> mBuilderConstructor;
    private Object[] mBuilderConstructorArgs =
            new Object[]{
                    this, AlertDialog.THEME_TRADITIONAL
            };
    private boolean mBuilderConstructorChecked = false;

    /**
     * check if AlertDialog.Builder(Context, int) is usable or not. See
     * {@link AlertDialog.Builder}.<br> Make sure you can't call
     * AlertDialog.Builder(Context, int) direct even if usable.<br> This is
     * because minSdkVersion is set to 4 and such device will crash even if you
     * didn't call constructor.
     *
     * @return true if Constructor is usable.
     */
    private boolean isHoneyCombBuilderExecutable(){
        if(mBuilderConstructorChecked){
            return (mBuilderConstructor == null) ? false : true;
        }

        boolean b = false;
        mBuilderConstructorChecked = true;
        try{
            Class<AlertDialog.Builder> clazz = AlertDialog.Builder.class;
            mBuilderConstructor = clazz.getConstructor(mBuilderConstructorSignature);
            b = true;
        }catch(NoSuchMethodException e){
            mBuilderConstructor = null;
            b = false;
        }
        return b;
    }

    private AlertDialog.Builder createDialogBuilderCompat(){
        AlertDialog.Builder builder;
        if(isHoneyCombBuilderExecutable()){
            try{
                builder = mBuilderConstructor.newInstance(mBuilderConstructorArgs);
                // Log.d(Constants.TAG, "use theme builder");
            }catch(Exception e){
                // Log.d(Constants.TAG, "use old builder.");
                builder = new AlertDialog.Builder(this);
            }
        }else{
            // Log.d(Constants.TAG, "use old builder.");
            builder = new AlertDialog.Builder(this);
        }

        return builder;
    }
}
