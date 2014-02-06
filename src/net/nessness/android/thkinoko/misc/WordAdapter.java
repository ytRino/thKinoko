package net.nessness.android.thkinoko.misc;

import java.util.ArrayList;
import java.util.List;
import net.nessness.android.thkinoko.R;
import net.nessness.android.thkinoko.misc.DBHelper.Word;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class WordAdapter extends ArrayAdapter<Word> {
	private static final int LAYOUT_WORD = R.layout.knk_dic_word;
	private static final int LAYOUT_SEPARATER = R.layout.word_separater;

	private LayoutInflater inflater;
	private ArrayList<TextView> separaters;
	private Resources res;
	private String[] labels;
	private SharedPreferences pref;
	private boolean useImg;

	/**
	 * コンストラクタ
	 * @param context
	 * @param textViewResourceId レイアウトの指定
	 * @param words データの指定
	 */
	public WordAdapter(Context context, List<Word> words) {
		super(context, LAYOUT_WORD, words);

		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.res = context.getResources();
		this.labels = res.getStringArray(R.array.cateories);
		pref = PreferenceManager.getDefaultSharedPreferences(context);
		useImg = pref.getBoolean(Constants.PREF_USE_IMG, false);
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position).key == null? false: true;
	}

	// ViewHolder
	// コンストラクタでレイアウト指定するのに中身が決め打ちになってる…
	static class ViewHolder{
		ImageView charactor;
		ImageView category;
		TextView key;
		TextView value;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Word word = getItem(position);

		ViewHolder holder;

		View view = convertView;
		if(word.key == null){
			if(separaters == null){
				separaters = new ArrayList<TextView>(DBHelper.CAT_NUM);
				TextView tv;
				for(int i = 0; i < DBHelper.CAT_NUM; i++){
					tv = (TextView) inflater.inflate(LAYOUT_SEPARATER, null);
					tv.setText(labels[i]);
					separaters.add(tv);
				}
			}
			return separaters.get(word.category);
		}else if(view == null || view.getTag() == null){
			// 新しくviewをつくる場合
			view = this.inflater.inflate(LAYOUT_WORD, null);
			holder = new ViewHolder();

			holder.charactor = (ImageView)view.findViewById(R.id.word_charactor);
			holder.category = (ImageView)view.findViewById(R.id.word_category);
			holder.key = (TextView)view.findViewById(R.id.word_key);
			holder.value = (TextView)view.findViewById(R.id.word_value);

			view.setTag(holder);
		}else{
			holder = (ViewHolder)view.getTag();
		}

		// データをセットして返す
		setCharactorImage(holder.charactor, word);
		setCategoryImage(holder.category, word);
		holder.key.setText(word.key.split(";")[0]+"\n"+word.at); // ;以降をカット
		holder.value.setText(word.value);

		return view;
	}

	private void setCategoryImage(ImageView category, Word word) {
		switch(word.category){
		case DBHelper.CAT_CHAR:
			category.setImageResource(R.drawable.cat_char);
			break;
		case DBHelper.CAT_SPELL:
			category.setImageResource(R.drawable.cat_spell);
			break;
		case DBHelper.CAT_MUSIC:
			category.setImageResource(R.drawable.cat_music);
			break;
		case DBHelper.CAT_ETC:
			category.setImageResource(R.drawable.cat_char);
			break;
		default:
			category.setImageResource(R.drawable.cat_char);
		}
	}

	private void setCharactorImage(ImageView charactor, Word word) {
		//Log.v(Constants.TAG, Boolean.toString(useImg));
		if(!useImg){
			charactor.setImageDrawable(null);
			charactor.setAlpha(255);
			return;
		}

		String[] owner = word.at.split(":");
		Drawable d = null;
		int alpha = 102;
		int resid;
		try{
		if(owner.length < 2){
			//d = null;
			alpha = 255;
		}else if((resid = res.getIdentifier("c"+Integer.parseInt(owner[1]), "drawable", "net.nessness.android.thkinoko")) != 0){
			d = res.getDrawable(resid);
			alpha = 102;
		}else{
			//d = null;
			alpha = 255;
		}
		}catch(ArrayIndexOutOfBoundsException e){
			Log.e(Constants.TAG, owner.length+"");
		}
		charactor.setImageDrawable(d);
		charactor.setAlpha(alpha);
	}

	public void updatePrefs(String key){
		if(key.equals(Constants.PREF_USE_IMG)){
			useImg = pref.getBoolean(Constants.PREF_USE_IMG, false);
		}
	}
}
