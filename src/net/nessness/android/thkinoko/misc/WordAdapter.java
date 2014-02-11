package net.nessness.android.thkinoko.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.nessness.android.thkinoko.R;

import java.util.ArrayList;
import java.util.List;

public class WordAdapter extends ArrayAdapter<Word> {
    private static final int LAYOUT_WORD = R.layout.knk_dic_word;
    private static final int LAYOUT_SEPARATOR = R.layout.word_separater;

    private LayoutInflater inflater;
    private ArrayList<TextView> separators;
    private Resources res;
    private String[] labels;
    private SharedPreferences pref;
    private boolean useImg;

    /**
     * コンストラクタ
     *
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
        return getItem(position).key == null ? false : true;
    }

    // ViewHolder
    // コンストラクタでレイアウト指定するのに中身が決め打ちになってる…
    static class ViewHolder {
        ImageView character;
        ImageView category;
        TextView key;
        TextView value;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Word word = getItem(position);

        ViewHolder holder;

        View view = convertView;
        if (word.key == null) {
            if (separators == null) {
                separators = new ArrayList<TextView>(DBHelper.CAT_NUM);
                TextView tv;
                for (int i = 0; i < DBHelper.CAT_NUM; i++) {
                    tv = (TextView) inflater.inflate(LAYOUT_SEPARATOR, null);
                    tv.setText(labels[i]);
                    separators.add(tv);
                }
            }
            return separators.get(word.category);
        } else if (view == null || view.getTag() == null) {
            // 新しくviewをつくる場合
            view = this.inflater.inflate(LAYOUT_WORD, null);
            holder = new ViewHolder();

            holder.character = (ImageView) view.findViewById(R.id.word_charactor);
            holder.category = (ImageView) view.findViewById(R.id.word_category);
            holder.key = (TextView) view.findViewById(R.id.word_key);
            holder.value = (TextView) view.findViewById(R.id.word_value);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // データをセットして返す
        setCharacterImage(holder.character, word);
        setCategoryImage(holder.category, word);
        holder.key.setText(word.key.split(";")[0] + "\n" + word.at); // ;以降をカット
        holder.value.setText(word.value);

        return view;
    }

    private void setCategoryImage(ImageView category, Word word) {
        switch (word.category) {
        case DBHelper.CAT_CHAR:
            category.setImageResource(R.drawable.cat_char);
            break;
        case DBHelper.CAT_SPELL:
            category.setImageResource(R.drawable.cat_spell);
            break;
        case DBHelper.CAT_MUSIC:
            category.setImageResource(R.drawable.cat_music);
            break;
        default:
            category.setImageResource(R.drawable.cat_char);
        }
    }

    private void setCharacterImage(ImageView character, Word word) {
        //Log.v(Constants.TAG, Boolean.toString(useImg));
        if (! useImg) {
            character.setImageDrawable(null);
            character.setAlpha(255);
            return;
        }

        Drawable d = null;
        int alpha;
        int resid;
        String cha = word.getPrimaryCharacter();
        if (cha == null) {
            //d = null;
            alpha = 255;
        } else if ((resid = res.getIdentifier("c" + Integer.parseInt(cha), "drawable", getContext().getPackageName())) != 0) {
            d = res.getDrawable(resid);
            alpha = 102;
        } else {
            //d = null;
            alpha = 255;
        }
        character.setImageDrawable(d);
        character.setAlpha(alpha);
    }

    public void updatePrefs(String key) {
        if (key.equals(Constants.PREF_USE_IMG)) {
            useImg = pref.getBoolean(Constants.PREF_USE_IMG, false);
        }
    }
}
