package net.nessness.android.thkinoko.misc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import net.nessness.android.thkinoko.R;

public class CategoryListAdapter extends ArrayAdapter<Boolean> {
    private static final int LAYOUT = R.layout.knk_category_item;

    private LayoutInflater inflater;
    private String[] categoriesName;
    private boolean[] categories;

    public CategoryListAdapter(Context context, boolean[] categories) {
        super(context, LAYOUT, new Boolean[DBHelper.CAT_NUM]);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        categoriesName = context.getResources().getStringArray(R.array.cateories);
        Boolean[] check = new Boolean[categories.length];
        for (int i = 0; i < categories.length; i++) {
            check[i] = Boolean.valueOf(categories[i]);
        }
        this.categories = categories;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(LAYOUT, null);
            CheckBox checkBox = ((CheckBox) view.findViewById(R.id.knk_dialog_cat_check));
            checkBox.setOnClickListener(new OnCategoryClickListener(position));
            checkBox.setText(categoriesName[position]);
            checkBox.setChecked(categories[position]);
        }
        return view;
    }

    private class OnCategoryClickListener implements View.OnClickListener {
        private final int position;

        public OnCategoryClickListener(int position) {
            super();
            this.position = position;
        }

        public void onClick(View view) {
            CheckBox checkBox = (CheckBox) view;
            boolean isChecked = checkBox.isChecked();

            categories[position] = isChecked;
            checkBox.setChecked(isChecked);
        }
    }
}
